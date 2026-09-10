package com.fagarester.translator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Скачивание больших файлов моделей (сотни МБ — единицы ГБ) с прогрессом.
 * Используется на экране онбординга при первом запуске.
 *
 * Многие CDN (в т.ч. Hugging Face) ограничивают скорость на ОДНО TCP-соединение —
 * независимо от реальной скорости интернета клиента. Поэтому файл качается
 * несколькими параллельными кусками (Range-запросы), как в обычных ускорителях
 * закачек — суммарная скорость от этого заметно растёт. Если сервер Range
 * не поддерживает — тихий откат на обычную последовательную загрузку.
 */
object ModelDownloader {

    private const val CHUNK_COUNT = 6

    /**
     * Google DNS-over-HTTPS вместо системного резолвера — иногда системный DNS
     * оператора/роутера резолвит новые домены заметно медленнее, что даёт
     * задержку перед стартом загрузки. При любой проблеме (нет сети до dns.google,
     * блокировка и т.п.) — тихий откат на обычный системный резолвер.
     */
    private val dohClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val fastDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            try {
                val request = Request.Builder()
                    .url("https://dns.google/resolve?name=$hostname&type=A")
                    .build()
                dohClient.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        val json = org.json.JSONObject(body)
                        val answers = json.optJSONArray("Answer")
                        if (answers != null) {
                            val addresses = mutableListOf<InetAddress>()
                            for (i in 0 until answers.length()) {
                                val ip = answers.getJSONObject(i).optString("data")
                                if (ip.isNotEmpty() && ip[0].isDigit()) {
                                    try {
                                        addresses.add(InetAddress.getByName(ip))
                                    } catch (t: Throwable) { /* игнор конкретной записи */ }
                                }
                            }
                            if (addresses.isNotEmpty()) return addresses
                        }
                    }
                }
            } catch (t: Throwable) {
                // игнор — откатываемся на системный DNS ниже
            }
            return Dns.SYSTEM.lookup(hostname)
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // без таймаута — модели большие, качаются долго
        .writeTimeout(30, TimeUnit.SECONDS)
        .dns(fastDns)
        .build()

    /**
     * @param onProgress вызывается на главном потоке со значением 0f..1f
     */
    suspend fun download(
        url: String,
        destFile: File,
        onProgress: suspend (Float?) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            destFile.parentFile?.mkdirs()
            val tmpFile = File(destFile.parentFile, destFile.name + ".part")

            val (totalSize, supportsRange) = probeServer(url)

            if (supportsRange && totalSize > 0) {
                downloadParallel(url, tmpFile, totalSize, onProgress)
            } else {
                downloadSequential(url, tmpFile, onProgress)
            }

            if (destFile.exists()) destFile.delete()
            tmpFile.renameTo(destFile)

            Result.success(destFile)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Узнаём размер файла и поддержку частичной загрузки (Range) одним быстрым
     * запросом (GET с Range: bytes=0-0), а не отдельным HEAD — у некоторых CDN
     * (в т.ч. Hugging Face) HEAD обрабатывается заметно медленнее GET.
     */
    private fun probeServer(url: String): Pair<Long, Boolean> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-0")
                .build()
            client.newCall(request).execute().use { resp ->
                if (resp.code == 206) {
                    // Формат заголовка: "bytes 0-0/12345678"
                    val total = resp.header("Content-Range")
                        ?.substringAfterLast('/')
                        ?.toLongOrNull() ?: -1L
                    total to (total > 0)
                } else {
                    val total = resp.header("Content-Length")?.toLongOrNull() ?: -1L
                    total to false
                }
            }
        } catch (t: Throwable) {
            -1L to false
        }
    }

    private suspend fun downloadParallel(
        url: String,
        tmpFile: File,
        totalSize: Long,
        onProgress: suspend (Float?) -> Unit
    ) = coroutineScope {
        RandomAccessFile(tmpFile, "rw").use { it.setLength(totalSize) }

        val chunkSize = totalSize / CHUNK_COUNT
        val ranges = (0 until CHUNK_COUNT).map { i ->
            val start = i * chunkSize
            val end = if (i == CHUNK_COUNT - 1) totalSize - 1 else (start + chunkSize - 1)
            start to end
        }

        val downloadedTotal = AtomicLong(0)
        val lastReportedPercent = AtomicInteger(-1)

        val jobs = ranges.map { (start, end) ->
            async(Dispatchers.IO) {
                downloadChunkWithRetry(url, tmpFile, start, end, totalSize, downloadedTotal, lastReportedPercent, onProgress)
            }
        }
        jobs.awaitAll()
    }

    /**
     * Качает один кусок файла с авто-повтором при обрыве соединения (например
     * "stream was reset") — сетевые сбои на отдельном потоке случаются, но не
     * должны валить всю загрузку целиком (остальные 5 кусков могли быть в порядке).
     */
    private suspend fun downloadChunkWithRetry(
        url: String,
        tmpFile: File,
        start: Long,
        end: Long,
        totalSize: Long,
        downloadedTotal: AtomicLong,
        lastReportedPercent: AtomicInteger,
        onProgress: suspend (Float?) -> Unit,
        maxAttempts: Int = 3
    ) {
        var attempt = 0
        var lastError: Throwable? = null
        while (attempt < maxAttempts) {
            attempt++
            val bytesBeforeAttempt = downloadedTotal.get()
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=$start-$end")
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body ?: throw Exception("Пустой ответ сервера")
                    RandomAccessFile(tmpFile, "rw").use { raf ->
                        raf.seek(start)
                        body.byteStream().use { input ->
                            val buffer = ByteArray(256 * 1024)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                raf.write(buffer, 0, bytesRead)
                                val total = downloadedTotal.addAndGet(bytesRead.toLong())
                                val percent = ((total * 100) / totalSize).toInt()
                                if (percent != lastReportedPercent.getAndSet(percent)) {
                                    withContext(Dispatchers.Main) {
                                        onProgress(total.toFloat() / totalSize.toFloat())
                                    }
                                }
                            }
                        }
                    }
                }
                return // успех
            } catch (t: Throwable) {
                lastError = t
                // Откатываем счётчик прогресса на то, что успели написать в этой
                // попытке, — при повторе кусок скачивается заново с самого начала.
                val bytesWrittenThisAttempt = downloadedTotal.get() - bytesBeforeAttempt
                if (bytesWrittenThisAttempt > 0) {
                    downloadedTotal.addAndGet(-bytesWrittenThisAttempt)
                }
                if (attempt < maxAttempts) {
                    kotlinx.coroutines.delay(500L * attempt)
                }
            }
        }
        throw lastError ?: Exception("Не удалось скачать часть файла")
    }

    private suspend fun downloadSequential(
        url: String,
        tmpFile: File,
        onProgress: suspend (Float?) -> Unit
    ) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Сервер ответил: ${response.code}")
            val body = response.body ?: throw Exception("Пустой ответ сервера")
            val totalSize = body.contentLength()

            body.byteStream().use { input ->
                tmpFile.outputStream().use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var bytesCopied = 0L
                    var bytesRead: Int
                    var lastReportedPercent = -1
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead
                        if (totalSize > 0) {
                            val percent = ((bytesCopied * 100) / totalSize).toInt()
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                withContext(Dispatchers.Main) {
                                    onProgress(bytesCopied.toFloat() / totalSize.toFloat())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}