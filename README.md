# FlexAI

Android-переводчик на Kotlin с локальными LLM/STT/TTS моделями (LiteRT-LM, Qualcomm QNN).

## Что изменилось при переименовании

- Пакет приложения: `com.fagarester.translator` → **`com.flexai.translator`**.
- Название приложения (label / `app_name`): **FlexAI**.
- Пакет `dev.notune.transcribe` (класс `MainActivity`, активность `RecognizeActivity`)
  **не переименован намеренно** — он жёстко зашит в нативной (JNI) библиотеке
  распознавания речи, и переименование сломает связь Kotlin ⇄ native код.

## Версии сборки

Взяты официальные актуальные стабильные версии на сентябрь 2026:

| Инструмент / библиотека            | Версия        |
|-------------------------------------|---------------|
| Android Gradle Plugin (AGP)         | 8.13.2        |
| Gradle                              | 8.13          |
| Kotlin                              | 2.2.21        |
| JDK                                 | 17            |
| compileSdk / targetSdk              | 36            |
| minSdk                              | 26            |
| Compose BOM                         | 2026.08.00    |
| androidx.core:core-ktx              | 1.18.0        |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.11.0   |
| androidx.activity:activity-compose  | 1.13.0        |
| kotlinx-coroutines-android          | 1.11.0        |
| okhttp                              | 5.4.0         |
| com.google.ai.edge.litertlm:litertlm-android | latest.release (Google Maven) |

Сознательно **не** взята Android Gradle Plugin 9.x — она вышла совсем недавно
и требует Gradle 9.6, а из-за нативных зависимостей (litertlm, QNN-библиотеки)
это повышает риск проблем совместимости. AGP 8.13.2 — последняя версия
предыдущей, полностью стабильной линейки.

Сборка ограничена ABI `arm64-v8a`, так как в `jniLibs` есть библиотеки только
под эту архитектуру (Qualcomm QNN/DSP).

## ⚠️ Про gradlew

В репозитории есть `gradle/wrapper/gradle-wrapper.properties`, но **нет
самого бинарного `gradle-wrapper.jar`** — я не могу сгенерировать бинарный
файл без доступа к сети/Gradle в этой среде.

Это не мешает открыть проект — Android Studio при первом открытии сама
сгенерирует `gradlew` и `gradle-wrapper.jar`. Если хочешь сделать это вручную
(с установленным Gradle 8.13):

```bash
gradle wrapper --gradle-version 8.13 --distribution-type bin
```

После этого `gradlew`/`gradlew.bat` появятся и заработают как обычно.
GitHub Actions workflow (`.github/workflows/android-build.yml`) собирает
проект через `setup-gradle`, поэтому он **не зависит** от наличия wrapper-а
в репозитории — можно сразу пушить в GitHub, CI соберёт debug APK.

## Подпись release-сборки (GitHub Actions)

Для сборки подписанного release APK нужно один раз создать keystore и
добавить 4 секрета в репозиторий GitHub.

### 1. Создать keystore (если его ещё нет)

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias flexai \
  -keyalg RSA -keysize 2048 -validity 10000
```

Команда спросит пароль хранилища, пароль ключа (можно тот же) и данные
владельца — вводи что угодно, на технические характеристики это не влияет.
**Сохрани `release.keystore` и оба пароля в надёжном месте** — если потеряешь,
обновлять приложение в Google Play под тем же `applicationId` будет нельзя.

### 2. Закодировать keystore в base64

```bash
base64 -i release.keystore -o release.keystore.base64   # macOS
# или
base64 -w0 release.keystore > release.keystore.base64   # Linux
```

### 3. Добавить секреты репозитория

GitHub → репозиторий → **Settings → Secrets and variables → Actions →
New repository secret**. Добавить четыре секрета:

| Имя секрета                  | Значение                                  |
|-------------------------------|--------------------------------------------|
| `ANDROID_KEYSTORE_BASE64`     | содержимое файла `release.keystore.base64` |
| `ANDROID_KEYSTORE_PASSWORD`   | пароль хранилища                          |
| `ANDROID_KEY_ALIAS`           | алиас ключа (`flexai` из примера выше)    |
| `ANDROID_KEY_PASSWORD`        | пароль ключа                              |

### 4. Запустить сборку

Workflow `.github/workflows/android-release.yml` запускается:
- автоматически — при пуше тега вида `v1.0.0`:
  ```bash
  git tag v1.0.0
  git push origin v1.0.0
  ```
- вручную — вкладка **Actions → Android CI (signed release build) → Run workflow**.

Подписанный APK появится артефактом `flexai-release-apk`.

Без этих секретов `assembleRelease` тоже отработает (локально или в
обычном debug-workflow это не нужно), но соберёт **неподписанный** APK —
такой нельзя ставить как обновление уже установленного приложения и
нельзя публиковать в Google Play.

## Сборка локально

```bash
# если gradlew уже сгенерирован Android Studio:
./gradlew assembleDebug

# либо через локально установленный Gradle:
gradle assembleDebug
```

APK появится в `app/build/outputs/apk/debug/`.
