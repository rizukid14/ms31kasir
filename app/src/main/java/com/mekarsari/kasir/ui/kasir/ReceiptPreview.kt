package com.mekarsari.kasir.ui.kasir

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReceiptPreview(
    receiptText: String,
    logoUri: String,
    showLogo: Boolean = true,
    logoWidthChar: Int = 12
) {
    val context = LocalContext.current
    val previewLogoBitmap by produceState<Bitmap?>(initialValue = null, key1 = logoUri) {
        value = withContext(Dispatchers.IO) {
            if (logoUri.isNotEmpty()) {
                com.mekarsari.kasir.printer.BitmapHelper.loadBitmapFromUri(context, logoUri)
            } else {
                com.mekarsari.kasir.printer.BitmapHelper.getDefaultPrintLogo(context)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.width(300.dp).background(Color.White),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Split receiptText into lines and render them
                val lines = receiptText.split("\n")
                lines.forEach { line ->
                    if (line.isEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                    } else if (line.contains("[LOGO]")) {
                        if (showLogo) {
                            previewLogoBitmap?.let { bitmap ->
                                val widthChar = if (logoWidthChar > 0) logoWidthChar else 12
                                val logoSizeDp = (widthChar * 5).coerceIn(32, 120).dp
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Preview Logo Struk",
                                    modifier = Modifier
                                        .size(logoSizeDp)
                                        .padding(bottom = 8.dp)
                                )
                            }
                        }
                    } else {
                        RenderReceiptLine(line)
                    }
                }
            }
        }
    }
}

@Composable
fun RenderReceiptLine(line: String) {
    // Check if line is centered
    val isCentered = line.startsWith("[C]")
    val cleanLine = line.removePrefix("[C]").removePrefix("[L]").trim()

    // Check if line is a QR code
    if (cleanLine.startsWith("<qrcode") && cleanLine.contains("</qrcode>")) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(1.dp, Color.Gray)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("QR CODE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        }
        return
    }

    // Check if line contains left and right components
    if (line.startsWith("[L]") && line.contains("[R]")) {
        val parts = line.removePrefix("[L]").split("[R]")
        val left = parts.getOrNull(0) ?: ""
        val right = parts.getOrNull(1) ?: ""
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RenderStyledText(left, Modifier.weight(1f, fill = false))
            RenderStyledText(right)
        }
        return
    }

    // Normal line
    RenderStyledText(
        text = cleanLine,
        modifier = Modifier.fillMaxWidth(),
        textAlign = if (isCentered) TextAlign.Center else TextAlign.Start
    )
}

@Composable
fun RenderStyledText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    // Parse <b> and </b> tags
    var isBold = false
    var cleanText = text
    if (text.startsWith("<b>") && text.endsWith("</b>")) {
        isBold = true
        cleanText = text.removePrefix("<b>").removeSuffix("</b>")
    }

    // Render divider lines as Compose Dividers
    if (cleanText == "================================") {
        Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
        return
    }
    if (cleanText == "--------------------------------") {
        Divider(color = Color.Gray.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
        return
    }

    Text(
        text = cleanText,
        modifier = modifier,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        color = Color.Black,
        textAlign = textAlign
    )
}
