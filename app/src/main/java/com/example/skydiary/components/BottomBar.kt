package com.example.skydiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skydiary.ui.theme.*

@Composable
fun BottomBar(
    current: String,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(SkyCard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BarItem("🏠", "หน้าแรก", current == "home") { onNavigate("home") }
            BarItem("📅", "ปฏิทิน", current == "calendar") { onNavigate("calendar") }
            Spacer(Modifier.width(56.dp))
            BarItem("📊", "สรุปผล", current == "summary") { onNavigate("summary") }
            BarItem("👤", "โปรไฟล์", current == "profile") { onNavigate("profile") }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(64.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(SkyBorder)
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 40.sp, color = androidx.compose.ui.graphics.Color.White)
        }
    }
}

@Composable
private fun BarItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) SkyBackground else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 22.sp)
        Text(
            label,
            fontFamily = Prompt,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) SkyLink else SkyText
        )
    }
}