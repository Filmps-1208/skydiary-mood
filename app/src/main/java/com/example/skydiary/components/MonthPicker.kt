package com.example.skydiary.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skydiary.ui.theme.*

@Composable
fun MonthPicker(
    value: YearMonth,
    onChange: (YearMonth) -> Unit,
    fontSize: Int = 20,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "‹",
            modifier = Modifier
                .clickable { onChange(value.plus(-1)) }
                .padding(horizontal = 20.dp, vertical = 4.dp),
            fontFamily = Prompt, fontWeight = FontWeight.Bold,
            fontSize = (fontSize + 8).sp, color = SkyText
        )
        Text(
            value.title,
            textAlign = TextAlign.Center,
            fontFamily = Prompt, fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp, color = SkyText
        )
        Text(
            "›",
            modifier = Modifier
                .clickable { onChange(value.plus(1)) }
                .padding(horizontal = 20.dp, vertical = 4.dp),
            fontFamily = Prompt, fontWeight = FontWeight.Bold,
            fontSize = (fontSize + 8).sp, color = SkyText
        )
    }
}