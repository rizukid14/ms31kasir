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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val namaToko by viewModel.namaToko.collectAsState()
    val alamatToko by viewModel.alamatToko.collectAsState()
    val alamatToko2 by viewModel.alamatToko2.collectAsState()
    val namaKasir by viewModel.namaKasir.collectAsState()
    val printerMac by viewModel.printerMac.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val pajakPersen by viewModel.pajakPersen.collectAsState()
    var pajakText by remember { mutableStateOf("") }
    LaunchedEffect(pajakPersen) {
        val currentParsed = pajakText.toDoubleOrNull() ?: 0.0
        if (currentParsed != pajakPersen || (pajakText.isEmpty() && pajakPersen != 0.0)) {
            pajakText = pajakPersen.toString()
        }
    }

    val logoUri by viewModel.logoUri.collectAsState()
    val receiptHeader by viewModel.receiptHeader.collectAsState()
    val receiptFooter1 by viewModel.receiptFooter1.collectAsState()
    val receiptFooter2 by viewModel.receiptFooter2.collectAsState()
    val receiptSpacingTop by viewModel.receiptSpacingTop.collectAsState()
    val receiptSpacingBottom by viewModel.receiptSpacingBottom.collectAsState()
    
    var spacingTopText by remember { mutableStateOf("") }
    LaunchedEffect(receiptSpacingTop) {
        val currentParsed = spacingTopText.toIntOrNull() ?: 0
        if (currentParsed != receiptSpacingTop || (spacingTopText.isEmpty() && receiptSpacingTop != 0)) {
            spacingTopText = receiptSpacingTop.toString()
        }
    }

    var spacingBottomText by remember { mutableStateOf("") }
    LaunchedEffect(receiptSpacingBottom) {
        val currentParsed = spacingBottomText.toIntOrNull() ?: 0
        if (currentParsed != receiptSpacingBottom || (spacingBottomText.isEmpty() && receiptSpacingBottom != 0)) {
            spacingBottomText = receiptSpacingBottom.toString()
        }
    }

    val showLogo by viewModel.showLogo.collectAsState()
    val logoWidthChar by viewModel.logoWidthChar.collectAsState()
    
    var logoWidthText by remember { mutableStateOf("") }
    LaunchedEffect(logoWidthChar) {
        val currentParsed = logoWidthText.toIntOrNull() ?: 0
        if (currentParsed != logoWidthChar || (logoWidthText.isEmpty() && logoWidthChar != 0)) {
            logoWidthText = logoWidthChar.toString()
        }
    }

    val showReceiptCode by viewModel.showReceiptCode.collectAsState()
    val showSeqNumber by viewModel.showSeqNumber.collectAsState()
    val showUnitQty by viewModel.showUnitQty.collectAsState()
    val showNomorMeja by viewModel.showNomorMeja.collectAsState()
    val showReceiptNumber by viewModel.showReceiptNumber.collectAsState()
    val showTotalQty by viewModel.showTotalQty.collectAsState()
    val showSignatureSection by viewModel.showSignatureSection.collectAsState()

    var showPreviewDialog by remember { mutableStateOf(false) }

    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Pratinjau Struk Dummy") },
            text = {
                val dummyCartItems = listOf(
                    com.mekarsari.kasir.domain.model.CartItem(
                        product = com.mekarsari.kasir.data.local.entity.Product(id = 1, nama = "Sate Kambing", harga = 25000L, kategori = "Makanan"),
                        quantity = 1,
                        customHarga = 25000L
                    ),
                    com.mekarsari.kasir.domain.model.CartItem(
                        product = com.mekarsari.kasir.data.local.entity.Product(id = 2, nama = "Es Teh Manis", harga = 4000L, kategori = "Minuman"),
                        quantity = 2,
                        customHarga = 4000L
                    )
                )
                val dummySubtotal = 33000L
                val dummyTaxAmount = (dummySubtotal * (pajakPersen / 100.0)).toLong()
                val dummyTotal = dummySubtotal + dummyTaxAmount

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(550.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    com.mekarsari.kasir.ui.kasir.ReceiptPreview(
                        shopName = namaToko,
                        shopAddress = alamatToko,
                        shopAddress2 = alamatToko2,
                        customHeader = receiptHeader,
                        customFooter1 = receiptFooter1,
                        customFooter2 = receiptFooter2,
                        cartItems = dummyCartItems,
                        subtotal = dummySubtotal,
                        taxPercentage = pajakPersen,
                        taxAmount = dummyTaxAmount,
                        total = dummyTotal,
                        payAmount = 50000L,
                        change = 50000L - dummyTotal,
                        nomorMeja = "05",
                        spacingTop = receiptSpacingTop,
                        spacingBottom = receiptSpacingBottom,
                        logoUri = logoUri,
                        showLogo = showLogo,
                        logoWidthChar = if (logoWidthChar > 0) logoWidthChar else 12,
                        showReceiptCode = showReceiptCode,
                        showSeqNumber = showSeqNumber,
                        showUnitQty = showUnitQty,
                        showNomorMeja = showNomorMeja,
                        showReceiptNumber = showReceiptNumber,
                        showTotalQty = showTotalQty,
                        showSignatureSection = showSignatureSection,
                        namaKasir = namaKasir
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPreviewDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    val logoBitmap by produceState<Bitmap?>(initialValue = null, key1 = logoUri) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.mekarsari.kasir.printer.BitmapHelper.loadBitmapFromUri(context, logoUri)
        }
    }

    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val localFile = java.io.File(context.filesDir, "shop_logo.png")
                        java.io.FileOutputStream(localFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        viewModel.saveLogoUri(Uri.fromFile(localFile).toString())
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal menyimpan logo: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    val backupStatus by viewModel.backupStatus.collectAsState()
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri ->
            if (uri != null) {
                viewModel.backupDatabase(context, uri)
            }
        }
    )

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.restoreDatabase(context, uri)
            }
        }
    )

    // Handling dialogs for backup / restore
    when (val status = backupStatus) {
        is BackupRestoreStatus.Loading -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("Memproses Data") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Harap tunggu sebentar...")
                    }
                }
            )
        }
        is BackupRestoreStatus.Success -> {
            AlertDialog(
                onDismissRequest = {
                    if (status.message.contains("Aplikasi akan ditutup")) {
                        val activity = context as? android.app.Activity
                        activity?.finishAffinity()
                        System.exit(0)
                    } else {
                        viewModel.resetBackupStatus()
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (status.message.contains("Aplikasi akan ditutup")) {
                                val activity = context as? android.app.Activity
                                activity?.finishAffinity()
                                System.exit(0)
                            } else {
                                viewModel.resetBackupStatus()
                            }
                        }
                    ) {
                        Text("OK")
                    }
                },
                title = { Text("Sukses") },
                text = { Text(status.message) }
            )
        }
        is BackupRestoreStatus.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetBackupStatus() },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetBackupStatus() }) {
                        Text("Tutup")
                    }
                },
                title = { Text("Gagal") },
                text = { Text(status.errorMsg) }
            )
        }
        BackupRestoreStatus.Idle -> { /* Do nothing */ }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirm = false
                        restoreLauncher.launch(arrayOf("*/*"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ya, Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Batal")
                }
            },
            title = { Text("Konfirmasi Restore Data") },
            text = {
                Text("Apakah Anda yakin ingin me-restore data?\n\nSemua transaksi saat ini akan DITIMPA dan DIHAPUS, lalu digantikan oleh data dari file backup. Aplikasi akan ditutup secara otomatis setelah proses berhasil.")
            }
        )
    }

    val printerManager = remember { BluetoothPrinterManager(context) }
    var pairedDevices by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var verifyingMacAddress by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    if (verifyingMacAddress != null) {
        AlertDialog(
            onDismissRequest = { verifyingMacAddress = null },
            title = { Text("Hubungkan Printer") },
            text = {
                if (isVerifying) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sedang menguji koneksi printer...")
                    }
                } else {
                    Text("Apakah Anda ingin memverifikasi koneksi dan menyimpan printer ini?")
                }
            },
            confirmButton = {
                if (!isVerifying) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                isVerifying = true
                                val mac = verifyingMacAddress!!
                                val testContent = "[C]<b>TEST KONEKSI</b>\n[C]Berhasil Terkoneksi!\n\n\n\n"
                                val testResult = printerManager.printReceipt(mac, testContent)
                                isVerifying = false
                                if (testResult.isSuccess) {
                                    viewModel.savePrinterMac(mac)
                                    Toast.makeText(context, "Koneksi Berhasil! Printer disimpan.", Toast.LENGTH_SHORT).show()
                                    verifyingMacAddress = null
                                } else {
                                    Toast.makeText(context, "Gagal konek: ${testResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text("Uji & Simpan")
                    }
                }
            },
            dismissButton = {
                if (!isVerifying) {
                    TextButton(onClick = { verifyingMacAddress = null }) {
                        Text("Batal")
                    }
                }
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val allGranted = permissions.values.all { it }
            hasPermission = allGranted
            if (allGranted) {
                pairedDevices = printerManager.getPairedDevices()
            } else {
                Toast.makeText(context, "Izin Bluetooth ditolak", Toast.LENGTH_SHORT).show()
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap.asImageBitmap(),
                                contentDescription = "Logo Toko",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (namaToko.length >= 2) namaToko.take(2).uppercase() else "MS",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (namaToko.isNotEmpty()) namaToko else "RM. Mekar Sari",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (alamatToko.isNotEmpty()) alamatToko else "Alamat belum diatur",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ElevatedButton(
                            onClick = { logoLauncher.launch("image/*") },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pilih Logo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        if (logoUri.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.saveLogoUri("")
                                    try {
                                        val localFile = java.io.File(context.filesDir, "shop_logo.png")
                                        if (localFile.exists()) {
                                            localFile.delete()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hapus", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
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
                value = alamatToko2,
                onValueChange = { viewModel.saveAlamatToko2(it) },
                label = { Text("Header 2 (di bawah Alamat)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = namaKasir,
                onValueChange = { viewModel.saveNamaKasir(it) },
                label = { Text("Nama Kasir") },
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
            Divider()
        }

        item {
            Text(
                text = "Tampilan Struk Transaksi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tampilkan logo", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = showLogo,
                    onCheckedChange = { viewModel.saveShowLogo(it) }
                )
            }
        }

        if (showLogo) {
            item {
                OutlinedTextField(
                    value = logoWidthText,
                    onValueChange = { newValue ->
                        logoWidthText = newValue
                        val parsed = newValue.toIntOrNull()
                        if (parsed != null && parsed >= 0) {
                            viewModel.saveLogoWidthChar(parsed)
                        } else if (newValue.isEmpty()) {
                            viewModel.saveLogoWidthChar(0)
                        }
                    },
                    label = { Text("Panjang logo (karakter)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tampilkan No. Urut", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = showSeqNumber,
                    onCheckedChange = { viewModel.saveShowSeqNumber(it) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tampilkan Nomor Meja", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = showNomorMeja,
                    onCheckedChange = { viewModel.saveShowNomorMeja(it) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tampilkan No. Struk", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = showReceiptNumber,
                    onCheckedChange = { viewModel.saveShowReceiptNumber(it) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tampilkan Satuan (Porsi/Gelas)", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = showUnitQty,
                    onCheckedChange = { viewModel.saveShowUnitQty(it) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tampilkan Total Kuantitas", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = showTotalQty,
                    onCheckedChange = { viewModel.saveShowTotalQty(it) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tampilkan Kolom Tanda Tangan", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = showSignatureSection,
                    onCheckedChange = { viewModel.saveShowSignatureSection(it) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tampilkan Kode Struk (QR Code)", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = showReceiptCode,
                    onCheckedChange = { viewModel.saveShowReceiptCode(it) }
                )
            }
        }

        item {
            Button(
                onClick = { showPreviewDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("LIHAT PRATINJAU STRUK DUMMY")
            }
            Spacer(modifier = Modifier.height(16.dp))
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
                    Button(onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                            )
                        )
                    }) {
                        Text("Beri Izin")
                    }
                }
            }
        }

        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item {
                Text(
                    text = "Aplikasi memerlukan izin Bluetooth untuk mendeteksi printer thermal Anda.",
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
                            .clickable { 
                                if (isSelected) {
                                    viewModel.savePrinterMac("")
                                    Toast.makeText(context, "Printer dinonaktifkan", Toast.LENGTH_SHORT).show()
                                } else {
                                    verifyingMacAddress = mac
                                }
                            },
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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

        item {
            Divider()
        }

        item {
            Text(
                text = "Manajemen Data (Backup & Restore)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cadangkan seluruh database transaksi dan pengaturan ke file eksternal (.db), atau pulihkan dari cadangan sebelumnya.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                backupLauncher.launch("mekarsari_backup.db")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showRestoreConfirm = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


