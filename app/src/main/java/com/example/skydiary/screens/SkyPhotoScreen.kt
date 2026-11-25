package com.example.skydiary.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skydiary.ui.theme.*

@Composable
fun SkyPhotoScreen(
    dateText: String = "Today",
    onSkip: () -> Unit = {},
    onSend: (Bitmap) -> Unit = {}
) {
    val context = LocalContext.current
    var photo by remember { mutableStateOf<Bitmap?>(null) }

    // เปิดกล้องของเครื่อง (ได้รูปขนาดย่อกลับมา เหมาะกับการพรีวิว)
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp -> if (bmp != null) photo = bmp }

    // เลือกรูปจากแกลเลอรี
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photo = try {
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

    if (photo == null) {
        CaptureView(
            dateText = dateText,
            onShutter = { cameraLauncher.launch(null) },
            onGallery = { galleryLauncher.launch("image/*") },
            onSkip = onSkip
        )
    } else {
        PreviewView(
            dateText = dateText,
            photo = photo!!,
            onRetake = { photo = null },
            onSend = { onSend(photo!!) }
        )
    }
}

@Composable
private fun CaptureView(
    dateText: String,
    onShutter: () -> Unit,
    onGallery: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackground)
            .padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(dateText, fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = SkyText)
        Spacer(Modifier.height(8.dp))
        Text("วันนี้ท้องฟ้าของคุณเป็นยังไง?", fontFamily = Prompt, fontSize = 20.sp, color = SkyText)
        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .shadow(8.dp, RoundedCornerShape(56.dp))
                .background(Color.White, RoundedCornerShape(56.dp))
                .clickable { onShutter() },
            contentAlignment = Alignment.Center
        ) {
            Text("📷", fontSize = 100.sp)
        }

        Spacer(Modifier.height(36.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(60.dp))
            ShutterButton(onClick = onShutter) {}
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(6.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .clickable { onGallery() },
                contentAlignment = Alignment.Center
            ) { Text("🖼️", fontSize = 24.sp) }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "ข้าม",
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(20.dp))
                .background(SkyDanger, RoundedCornerShape(20.dp))
                .clickable { onSkip() }
                .padding(horizontal = 22.dp, vertical = 6.dp),
            fontFamily = Prompt, fontSize = 14.sp, color = Color.White
        )
    }
}

@Composable
private fun PreviewView(
    dateText: String,
    photo: Bitmap,
    onRetake: () -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackground)
            .padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(dateText, fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = SkyText)
        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(56.dp))
                .background(Color.White, RoundedCornerShape(56.dp))
                .padding(28.dp)
        ) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = "รูปท้องฟ้า",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(36.dp))

        ShutterButton(onClick = onSend) {
            Text("➤", fontSize = 40.sp, color = SkyText)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "ถ่ายใหม่",
            modifier = Modifier.clickable { onRetake() },
            fontFamily = Prompt, fontSize = 14.sp, color = SkyLink, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .shadow(6.dp, CircleShape)
            .background(Color(0xFF6DA2F0), CircleShape)
            .padding(10.dp)
            .background(Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
fun SkyPhotoPreview() {
    SkyDiaryTheme { SkyPhotoScreen() }
}