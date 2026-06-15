package com.mekarsari.kasir.ui.riwayat

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mekarsari.kasir.printer.BluetoothPrinterManager
import com.mekarsari.kasir.printer.ReceiptFormatter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatDetailScreen(
    transactionId: Int,
    viewModel: RiwayatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val txWithItems by viewModel.selectedTransaction.collectAsState()

    val printerManager = remember { BluetoothPrinterManager(context) }
    val formatter = remember { ReceiptFormatter() }

    var selectedTab by remember { mutableStateOf(0) }
    var shopName by remember { mutableStateOf("Mekar Sari") }
    var shopAddress by remember { mutableStateOf("") }
    var shopAddress2 by remember { mutableStateOf("") }
    var logoUri by remember { mutableStateOf("") }
    var customHeader by remember { mutableStateOf("") }
    var customFooter1 by remember { mutableStateOf("") }
    var customFooter2 by remember { mutableStateOf("") }
    var rSettingsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(transactionId) {
        viewModel.selectTransaction(transactionId)
        try {
            val store = viewModel.getStoreDetails()
            shopName = store.first
            shopAddress = store.second
            shopAddress2 = store.third
            logoUri = viewModel.getLogoUri()
            val rSettings = viewModel.getReceiptSettings()
            rSettingsMap = rSettings
            customHeader = rSettings["receipt_header"] ?: ""
            customFooter1 = rSettings["receipt_footer1"] ?: ""
            customFooter2 = rSettings["receipt_footer2"] ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Transaksi TX#$transactionId") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        txWithItems?.let { detail ->
            val transaction = detail.transaction
            val items = detail.items

            val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("in", "ID"))
            val dateFormatted = sdf.format(Date(transaction.createdAt))

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Detail Pesanan") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Preview Struk") }
                    )
                }

                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header details Card
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "Informasi Transaksi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Divider()
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Waktu")
                                    Text(dateFormatted, fontWeight = FontWeight.SemiBold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Metode Pembayaran")
                                    Text(transaction.metodePembayaran.uppercase(), fontWeight = FontWeight.SemiBold)
                                }
                                if (!transaction.nomorMeja.isNullOrEmpty()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Nomor Meja")
                                        Text(transaction.nomorMeja, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // Items list header
                        Text(text = "Daftar Item", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.namaProdukSnapshot, fontWeight = FontWeight.SemiBold)
                                        Text("${item.qty} x ${formatRupiah(item.hargaSaatItu)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Text(formatRupiah(item.subtotal), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Summary calculations Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total", fontWeight = FontWeight.Bold)
                                    Text(formatRupiah(transaction.total), fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Bayar")
                                    Text(formatRupiah(transaction.bayar))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Kembali", fontWeight = FontWeight.SemiBold)
                                    Text(formatRupiah(transaction.kembalian), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RiwayatReceiptPreview(
                            shopName = shopName,
                            shopAddress = shopAddress,
                            shopAddress2 = shopAddress2,
                            customHeader = customHeader,
                            customFooter1 = customFooter1,
                            customFooter2 = customFooter2,
                            transaction = transaction,
                            items = items,
                            logoUri = logoUri,
                            showLogo = rSettingsMap["show_logo"]?.toBoolean() ?: true,
                            logoWidthChar = rSettingsMap["logo_width_char"]?.toIntOrNull()?.let { if (it > 0) it else 12 } ?: 12,
                            showReceiptCode = rSettingsMap["show_receipt_code"]?.toBoolean() ?: false,
                            showSeqNumber = rSettingsMap["show_seq_number"]?.toBoolean() ?: false,
                            showUnitQty = rSettingsMap["show_unit_qty"]?.toBoolean() ?: false,
                            showNomorMeja = rSettingsMap["show_nomor_meja"]?.toBoolean() ?: true,
                            showReceiptNumber = rSettingsMap["show_receipt_number"]?.toBoolean() ?: true,
                            showTotalQty = rSettingsMap["show_total_qty"]?.toBoolean() ?: false,
                            showSignatureSection = rSettingsMap["show_signature_section"]?.toBoolean() ?: true,
                            namaKasir = rSettingsMap["nama_kasir"] ?: "Kasir 1"
                        )
                    }
                }

                // Print button
                Button(
                    onClick = {
                        scope.launch {
                            val macAddress = viewModel.getPrinterMac()
                            val store = viewModel.getStoreDetails()
                            val rSettings = viewModel.getReceiptSettings()
                            val logoBitmap = com.mekarsari.kasir.printer.BitmapHelper.loadBitmapFromUri(context, viewModel.getLogoUri())
                            
                            if (macAddress.isEmpty()) {
                                Toast.makeText(context, "Atur printer Bluetooth di halaman Settings terlebih dahulu", Toast.LENGTH_LONG).show()
                                return@launch
                            }

                             val receiptStr = formatter.format(
                                 shopName = store.first,
                                 shopAddress = store.second,
                                 shopAddress2 = store.third,
                                 transactionWithItems = detail,
                                 customHeader = rSettings["receipt_header"] ?: "",
                                 customFooter1 = rSettings["receipt_footer1"] ?: "",
                                 customFooter2 = rSettings["receipt_footer2"] ?: "",
                                 spacingTop = rSettings["receipt_spacing_top"]?.toIntOrNull() ?: 1,
                                 spacingBottom = rSettings["receipt_spacing_bottom"]?.toIntOrNull() ?: 4,
                                 showLogo = rSettings["show_logo"]?.toBoolean() ?: true,
                                 showReceiptCode = rSettings["show_receipt_code"]?.toBoolean() ?: false,
                                 showSeqNumber = rSettings["show_seq_number"]?.toBoolean() ?: false,
                                 showUnitQty = rSettings["show_unit_qty"]?.toBoolean() ?: false,
                                 showNomorMeja = rSettings["show_nomor_meja"]?.toBoolean() ?: true,
                                 showReceiptNumber = rSettings["show_receipt_number"]?.toBoolean() ?: true,
                                 showTotalQty = rSettings["show_total_qty"]?.toBoolean() ?: false,
                                 showSignatureSection = rSettings["show_signature_section"]?.toBoolean() ?: true,
                                 namaKasir = rSettings["nama_kasir"] ?: "Kasir 1"
                             )
                             val logoWidth = rSettings["logo_width_char"]?.toIntOrNull()?.let { if (it > 0) it else 12 } ?: 12
                             val printResult = printerManager.printReceipt(macAddress, receiptStr, logoBitmap, logoWidth)
                            if (printResult.isSuccess) {
                                Toast.makeText(context, "Struk dicetak ulang", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Gagal mencetak: ${printResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cetak Ulang Struk", fontWeight = FontWeight.Bold)
                }
            }
        } ?: run {
            Box(modifier = modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun RiwayatReceiptPreview(
    shopName: String,
    shopAddress: String,
    shopAddress2: String = "",
    customHeader: String,
    customFooter1: String,
    customFooter2: String,
    transaction: com.mekarsari.kasir.data.local.entity.Transaction,
    items: List<com.mekarsari.kasir.data.local.entity.TransactionItem>,
    logoUri: String = "",
    showLogo: Boolean = true,
    logoWidthChar: Int = 12,
    showReceiptCode: Boolean = false,
    showSeqNumber: Boolean = false,
    showUnitQty: Boolean = false,
    showNomorMeja: Boolean = true,
    showReceiptNumber: Boolean = true,
    showTotalQty: Boolean = false,
    showSignatureSection: Boolean = true,
    namaKasir: String = ""
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.width(300.dp).background(Color.White),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val context = LocalContext.current
                
                val previewLogoBitmap = remember(logoUri) {
                    if (logoUri.isNotEmpty()) {
                        com.mekarsari.kasir.printer.BitmapHelper.loadBitmapFromUri(context, logoUri)
                    } else {
                        // Fallback ke logo default dari drawable
                        com.mekarsari.kasir.printer.BitmapHelper.getDefaultPrintLogo(context)
                    }
                }
                
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
                
                // Simulated Thermal Receipt Header
                if (customHeader.isNotEmpty()) {
                    Text(
                        text = customHeader,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )
                }
                Text(
                    text = shopName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
                if (shopAddress.isNotEmpty()) {
                    Text(
                        text = shopAddress,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )
                }
                if (shopAddress2.isNotEmpty()) {
                    Text(
                        text = shopAddress2,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )
                }

                Text(
                    text = "================================",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 1
                )

                if (showSeqNumber) {
                    Text(
                        text = "NO. URUT: ${String.format("%03d", (transaction.id % 1000).coerceAtLeast(1))}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "--------------------------------",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                val dateStr = sdfDate.format(Date(transaction.createdAt))
                val timeStr = sdfTime.format(Date(transaction.createdAt))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dateStr, fontSize = 10.sp, color = Color.Black)
                    if (showReceiptNumber) {
                        Text("No: TX#${transaction.id}", fontSize = 10.sp, color = Color.Black)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(timeStr, fontSize = 10.sp, color = Color.Black)
                    if (namaKasir.isNotEmpty()) {
                        Text("Kasir: $namaKasir", fontSize = 10.sp, color = Color.Black)
                    }
                }

                val mesa = transaction.nomorMeja
                if (showNomorMeja && !mesa.isNullOrEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Text("Meja: $mesa", fontSize = 10.sp, color = Color.Black)
                    }
                }

                Text(
                    text = "--------------------------------",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 1
                )

                // Items List
                items.forEach { item ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = item.namaProdukSnapshot,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val qtyText = if (showUnitQty) {
                                val isMinuman = item.namaProdukSnapshot.contains("es ", ignoreCase = true) ||
                                                item.namaProdukSnapshot.contains("teh", ignoreCase = true) ||
                                                item.namaProdukSnapshot.contains("jeruk", ignoreCase = true) ||
                                                item.namaProdukSnapshot.contains("kopi", ignoreCase = true) ||
                                                item.namaProdukSnapshot.contains("milo", ignoreCase = true) ||
                                                item.namaProdukSnapshot.contains("susu", ignoreCase = true) ||
                                                item.namaProdukSnapshot.contains("soda", ignoreCase = true) ||
                                                item.namaProdukSnapshot.contains("cola", ignoreCase = true) ||
                                                item.namaProdukSnapshot.contains("fanta", ignoreCase = true) ||
                                                item.namaProdukSnapshot.contains("sprite", ignoreCase = true) ||
                                                item.namaProdukSnapshot.contains("frestea", ignoreCase = true) ||
                                                item.namaProdukSnapshot.contains("air ", ignoreCase = true)
                                val unit = if (isMinuman) "Gelas" else "Porsi"
                                "  ${item.qty} $unit x ${formatRupiah(item.hargaSaatItu)}"
                            } else {
                                "  ${item.qty} x ${formatRupiah(item.hargaSaatItu)}"
                            }
                            Text(
                                text = qtyText,
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                text = formatRupiah(item.subtotal),
                                fontSize = 11.sp,
                                color = Color.Black
                            )
                        }
                    }
                }

                Text(
                    text = "--------------------------------",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 1
                )

                // Calculations
                val subtotal = items.sumOf { it.subtotal }
                val taxAmount = transaction.total - subtotal
                
                if (showTotalQty) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Kuantitas", fontSize = 11.sp, color = Color.Black)
                        Text("${items.sumOf { it.qty }}", fontSize = 11.sp, color = Color.Black)
                    }
                    Text(
                        text = "--------------------------------",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                if (taxAmount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", fontSize = 11.sp, color = Color.Black)
                        Text(formatRupiah(subtotal), fontSize = 11.sp, color = Color.Black)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val taxPercent = if (subtotal > 0) {
                            Math.round((taxAmount.toDouble() / subtotal.toDouble()) * 100.0 * 10.0) / 10.0
                        } else 0.0
                        Text("Pajak ($taxPercent%)", fontSize = 11.sp, color = Color.Black)
                        Text(formatRupiah(taxAmount), fontSize = 11.sp, color = Color.Black)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(formatRupiah(transaction.total), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bayar", fontSize = 11.sp, color = Color.Black)
                    Text(formatRupiah(transaction.bayar), fontSize = 11.sp, color = Color.Black)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kembali", fontSize = 11.sp, color = Color.Black)
                    Text(formatRupiah(transaction.kembalian), fontSize = 11.sp, color = Color.Black)
                }

                Text(
                    text = "================================",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 1
                )

                // Simulated Thermal Receipt Footer
                if (customFooter1.isNotEmpty()) {
                    Text(
                        text = customFooter1,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )
                }
                if (customFooter2.isNotEmpty()) {
                    Text(
                        text = customFooter2,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )
                }
                if (showSignatureSection) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tanda Tangan", fontSize = 10.sp, color = Color.Black, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("(....................)", fontSize = 10.sp, color = Color.Black, textAlign = TextAlign.Center)
                }
                if (showReceiptCode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .border(1.dp, Color.Gray)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("QR CODE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }
            }
        }
    }
}

private fun formatRupiah(value: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    format.maximumFractionDigits = 0
    return format.format(value).replace("Rp", "Rp ")
}
