package com.example.skydiary.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Prompt = FontFamily.Default

val Typography = Typography(
    bodyLarge = TextStyle(fontFamily = Prompt, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Prompt, fontSize = 14.sp),
    titleLarge = TextStyle(fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    labelLarge = TextStyle(fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 20.sp)
)