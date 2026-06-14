package com.mekarsari.kasir.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mekarsari.kasir.printer.BluetoothPrinterManager
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val namaToko by viewModel.namaToko.collectAsState()
    val alamatToko by viewModel.alamatToko.collectAsState()
    val printerMac by viewModel.printerMac.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val pajakPersen by viewModel.pajakPersen.collectAsState()
    var pajakText by remember(pajakPersen) { mutableStateOf(pajakPersen.toString()) }
    val logoUri by viewModel.logoUri.collectAsState()
    val receiptHeader by viewModel.receiptHeader.collectAsState()
    val receiptFooter1 by viewModel.receiptFooter1.collectAsState()
    val receiptFooter2 by viewModel.receiptFooter2.collectAsState()
    val receiptSpacingTop by viewModel.receiptSpacingTop.collectAsState()
    val receiptSpacingBottom by viewModel.receiptSpacingBottom.collectAsState()
    var spacingTopText by remember(receiptSpacingTop) { mutableStateOf(receiptSpacingTop.toString()) }
    var spacingBottomText by remember(receiptSpacingBottom) { mutableStateOf(receiptSpacingBottom.toString()) }

    val logoBitmap = remember(logoUri) {
        com.mekarsari.kasir.printer.BitmapHelper.loadBitmapFromUri(context, logoUri)
    }

    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.saveLogoUri(uri.toString())
            }
        }
    )

    val printerManager = remember { BluetoothPrinterManager(context) }
    var pairedDevices by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                pairedDevices = printerManager.getPairedDevices()
            } else {
                Toast.makeText(context, "Izin Bluetooth Connect ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            pairedDevices = printerManager.getPairedDevices()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Pengaturan Toko",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (logoBitmap != null) {
                    Image(
                        bitmap = logoBitmap.asImageBitmap(),
                        contentDescription = "Logo Toko",
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.LightGray, shape = CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LOGO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { logoLauncher.launch("image/*") },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Pilih Logo Toko", fontSize = 12.sp)
                    }
                    if (logoUri.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.saveLogoUri("") },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Hapus Logo", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = namaToko,
                onValueChange = { viewModel.saveNamaToko(it) },
                label = { Text("Nama Toko") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = alamatToko,
                onValueChange = { viewModel.saveAlamatToko(it) },
                label = { Text("Alamat Toko") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = pajakText,
                onValueChange = { newValue ->
                    pajakText = newValue
                    val parsed = newValue.toDoubleOrNull()
                    if (parsed != null && parsed >= 0.0) {
                        viewModel.savePajakPersen(parsed)
                    } else if (newValue.isEmpty()) {
                        viewModel.savePajakPersen(0.0)
                    }
                },
                label = { Text("Pajak (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = receiptHeader,
                onValueChange = { viewModel.saveReceiptHeader(it) },
                label = { Text("Kalimat Header Struk (Kustom)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = receiptFooter1,
                onValueChange = { viewModel.saveReceiptFooter1(it) },
                label = { Text("Kalimat Footer Struk Baris 1") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = receiptFooter2,
                onValueChange = { viewModel.saveReceiptFooter2(it) },
                label = { Text("Kalimat Footer Struk Baris 2") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = spacingTopText,
                    onValueChange = { newValue ->
                        spacingTopText = newValue
                        val parsed = newValue.toIntOrNull()
                        if (parsed != null && parsed >= 0) {
                            viewModel.saveReceiptSpacingTop(parsed)
                        } else if (newValue.isEmpty()) {
                            viewModel.saveReceiptSpacingTop(0)
                        }
                    },
                    label = { Text("Spasi Atas Struk (Baris)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = spacingBottomText,
                    onValueChange = { newValue ->
                        spacingBottomText = newValue
                        val parsed = newValue.toIntOrNull()
                        if (parsed != null && parsed >= 0) {
                            viewModel.saveReceiptSpacingBottom(parsed)
                        } else if (newValue.isEmpty()) {
                            viewModel.saveReceiptSpacingBottom(0)
                        }
                    },
                    label = { Text("Spasi Bawah Struk (Baris)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        item {
            Text(
                text = "Tema Aplikasi (Light Mode Only)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val themes = listOf(
                    Triple("ORANGE", "Orange", Color(0xFFE65100)),
                    Triple("BLUE", "Biru", Color(0xFF0D47A1)),
                    Triple("GREEN", "Hijau", Color(0xFF1B5E20)),
                    Triple("PURPLE", "Ungu", Color(0xFF4A148C))
                )

                themes.forEach { (themeName, label, color) ->
                    val isSelected = themeName == selectedTheme
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.saveTheme(themeName) }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = color,
                            modifier = Modifier
                                .size(48.dp)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        item {
            Divider()
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bluetooth Printer (ESC/POS)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) }) {
                        Text("Beri Izin")
                    }
                }
            }
        }

        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item {
                Text(
                    text = "Aplikasi memerlukan izin Bluetooth Connect untuk mendeteksi printer thermal Anda.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            if (pairedDevices.isEmpty()) {
                item {
                    Text(
                        text = "Tidak ada perangkat Bluetooth berpasangan terdeteksi. Silakan pasangkan printer Anda di menu Bluetooth pengaturan Android terlebih dahulu.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(pairedDevices) { (mac, name) ->
                    val isSelected = mac == printerMac
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.savePrinterMac(mac) },
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = name, fontWeight = FontWeight.Bold)
                                Text(text = mac, style = MaterialTheme.typography.bodySmall)
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Terpilih",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (printerMac.isNotEmpty()) {
            item {
                Button(
                    onClick = {
                        scope.launch {
                            val testContent = "[C]<b>TEST PRINT</b>\n[C]Printer Terkoneksi!\n[C]Mekar Sari Kasir\n[C]--------------------------------\n[C]\n\n\n\n"
                            val result = printerManager.printReceipt(printerMac, testContent)
                            if (result.isSuccess) {
                                Toast.makeText(context, "Test print berhasil", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Test print gagal: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Print Struk")
                }
            }
        }
    }
}


