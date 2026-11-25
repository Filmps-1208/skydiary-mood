package com.example.skydiary.screens

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skydiary.components.*
import com.example.skydiary.ui.theme.*

@Composable
fun ProfileScreen(
    name: String = "Carfiw",
    email: String = "Carfiw.car@dome.tu.ac.th",
    phone: String = "080-123-4567",
    password: String = "Carfiw123",
    onNavigate: (String) -> Unit = {},
    onAddClick: () -> Unit = {},
    onBellClick: () -> Unit = {},
    hasUnread: Boolean = false,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    var avatar by remember { mutableStateOf<Bitmap?>(null) }
    var showPassword by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            avatar = try {
                if (Build.VERSION.SDK_INT >= 28) {
                    val src = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(SkyBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.End
        ) {
            BellButton(hasUnread = hasUnread, onClick = onBellClick)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(32.dp))
                    .background(Color(0xFFF0F6FF), RoundedCornerShape(32.dp))
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.Black, CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatar != null) {
                        Image(
                            bitmap = avatar!!.asImageBitmap(),
                            contentDescription = "รูปโปรไฟล์",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("👤", fontSize = 48.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Edit",
                    modifier = Modifier.clickable { galleryLauncher.launch("image/*") },
                    fontFamily = Prompt, fontSize = 13.sp, color = SkyText,
                    textDecoration = TextDecoration.Underline
                )

                Spacer(Modifier.height(18.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE3EAF6), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    InfoRow("NAME :", name)
                    HorizontalDivider(color = Color(0xFF9AA5B5))
                    InfoRow("EMAIL :", email)
                    HorizontalDivider(color = Color(0xFF9AA5B5))
                    InfoRow("NUMBER :", phone)
                    HorizontalDivider(color = Color(0xFF9AA5B5))
                    InfoRow(
                        label = "PASSWORD :",
                        value = if (showPassword) password else "•".repeat(password.length),
                        trailing = {


                        }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "ออกจากระบบ",
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .background(SkyDanger, RoundedCornerShape(12.dp))
                    .clickable { onLogout() }
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
                fontFamily = Prompt, fontWeight = FontWeight.Bold,
                fontSize = 22.sp, color = Color.White
            )
        }

        BottomBar(current = "profile", onNavigate = onNavigate, onAddClick = onAddClick)
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.width(110.dp),
            fontFamily = Prompt, fontSize = 14.sp, color = SkyText
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            fontFamily = Prompt, fontSize = 15.sp, color = SkyText
        )
        if (trailing != null) trailing()
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
fun ProfilePreview() {
    SkyDiaryTheme { ProfileScreen() }
}