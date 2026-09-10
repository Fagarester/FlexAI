package com.fagarester.translator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingWelcomeScreen(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AI Переводчик",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = GradientContentColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Полностью локальный переводчик и распознаватель речи — " +
                "может работать прямо на устройстве, без интернета.\n\n" +
                "Для этого используются две офлайн-модели: Gemma переводит текст, " +
                "Parakeet распознаёт речь. Их нужно один раз скачать.\n\n" +
                "Если офлайн-точности не хватает — в настройках можно подключить " +
                "облачный AI API вместо локальной модели.",
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        GradientButton(
            onClick = onNext,
            brush = defaultButtonBrush(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Далее")
        }
    }
}