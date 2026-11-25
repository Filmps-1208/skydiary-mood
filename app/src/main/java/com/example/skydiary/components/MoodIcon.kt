package com.example.skydiary.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.skydiary.R

fun moodDrawable(key: String): Int = when (key) {
    "happy" -> R.drawable.mood_happy
    "sad" -> R.drawable.mood_sad
    "scared" -> R.drawable.mood_scared
    "angry" -> R.drawable.mood_angry
    else -> R.drawable.mood_confused
}

@Composable
fun MoodIcon(key: String, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = moodDrawable(key)),
        contentDescription = key,
        modifier = modifier.size(size)
    )
}