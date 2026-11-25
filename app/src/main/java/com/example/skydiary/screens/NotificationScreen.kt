package com.example.skydiary.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skydiary.ui.theme.*

data class AppNotification(
    val title: String,
    val message: String,
    val dateText: String,
    val onClick: (() -> Unit)? = null,
    val actionText: String? = null
)

@Composable
fun NotificationScreen(
    notifications: List<AppNotification> = emptyList(),
    onClose: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackground)
            .padding(horizontal = 18.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                "✕",
                modifier = Modifier.clickable { onClose() }.padding(4.dp),
                fontSize = 26.sp, color = SkyDanger
            )
        }
        Text(
            "ข้อความแจ้งเตือน",
            fontFamily = Prompt, fontWeight = FontWeight.Bold,
            fontSize = 26.sp, color = SkyText
        )
        Spacer(Modifier.height(20.dp))

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "ยังไม่มีข้อความแจ้งเตือน",
                    fontFamily = Prompt, fontSize = 16.sp, color = SkyHint,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(notifications) { n ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(28.dp))
                            .background(SkyCard, RoundedCornerShape(28.dp))
                            .then(if (n.onClick != null) Modifier.clickable { n.onClick.invoke() } else Modifier)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                n.title,
                                fontFamily = Prompt, fontWeight = FontWeight.Bold,
                                fontSize = 15.sp, color = SkyText
                            )
                            Text(n.dateText, fontFamily = Prompt, fontSize = 13.sp, color = SkyHint)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(n.message, fontFamily = Prompt, fontSize = 14.sp, color = SkyText)
                        if (n.actionText != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                n.actionText,
                                fontFamily = Prompt, fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, color = SkyLink
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
fun NotificationPreview() {
    SkyDiaryTheme {
        NotificationScreen(
            listOf(
                AppNotification(
                    "แจ้งเตือนการบันทึก:",
                    "วันนี้คุณเป็นยังไงบ้าง? อย่าลืมเข้ามาบอก/บันทึกกับเรานะ 💗",
                    "5/4/69"
                )
            )
        )
    }
}