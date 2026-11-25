package com.example.skydiary.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SkyColorScheme = lightColorScheme(
    primary = SkyPrimary,
    background = SkyBackground,
    surface = SkyCard,
    onPrimary = Color.White,
    onBackground = SkyText,
    onSurface = SkyText
)

@Composable
fun SkyDiaryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SkyColorScheme,
        typography = Typography,
        content = content
    )
}