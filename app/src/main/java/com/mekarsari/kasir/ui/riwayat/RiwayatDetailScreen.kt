package com.mekarsari.kasir.ui.riwayat

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mekarsari.kasir.printer.BluetoothPrinterManager
import com.mekarsari.kasir.printer.ReceiptFormatter
import com.mekarsari.kasir.util.CurrencyUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatDetailScreen(
    transactionId: Int,
    viewModel: RiwayatViewModel,
    kasirViewModel: com.mekarsari.kasir.ui.kasir.KasirViewModel,
    navController: androidx.navigation.NavController,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val txWithItems by viewModel.selectedTransaction.collectAsState()
    val settingsMap by viewModel.settingsMap.collectAsState()

    val printerManager = remember { BluetoothPrinterManager(context) }
    val formatter = remember { ReceiptFormatter() }

    var selectedTab by remember { mutableStateOf(0) }
    var showPrintConfirmDialog by remember { mutableStateOf(false) }

    val logoUri = settingsMap["logo_uri"] ?: ""
    val shopName = settingsMap["nama_toko"] ?: "Mekar Sari"
    val shopAddress = settingsMap["alamat_toko"] ?: ""
    val shopAddress2 = settingsMap["alamat_toko2"] ?: ""
    val customHeader = settingsMap["receipt_header"] ?: ""
    val customFooter1 = settingsMap["receipt_footer1"] ?: ""
    val customFooter2 = settingsMap["receipt_footer2"] ?: ""

    LaunchedEffect(transactionId) {
        viewModel.selectTransaction(transactionId)
    }

    // Generate receipt text reactively
    val receiptStr = remember(settingsMap, txWithItems, shopName, shopAddress, shopAddress2, customHeader, customFooter1, customFooter2) {
        val detail = txWithItems
        if (detail == null) "" else {
            formatter.format(
                shopName = shopName,
                shopAddress = shopAddress,
                shopAddress2 = shopAddress2,
                transactionWithItems = detail,
                customHeader = customHeader,
                customFooter1 = customFooter1,
                customFooter2 = customFooter2,
                spacingTop = settingsMap["receipt_spacing_top"]?.toIntOrNull() ?: 1,
                spacingBottom = settingsMap["receipt_spacing_bottom"]?.toIntOrNull() ?: 4,
                showLogo = settingsMap["show_logo"]?.toBoolean() ?: true,
                showReceiptCode = settingsMap["show_receipt_code"]?.toBoolean() ?: false,
                showSeqNumber = settingsMap["show_seq_number"]?.toBoolean() ?: false,
                showUnitQty = settingsMap["show_unit_qty"]?.toBoolean() ?: false,
                showNomorMeja = settingsMap["show_nomor_meja"]?.toBoolean() ?: true,
                showReceiptNumber = settingsMap["show_receipt_number"]?.toBoolean() ?: true,
                showTotalQty = settingsMap["show_total_qty"]?.toBoolean() ?: false,
                showSignatureSection = settingsMap["show_signature_section"]?.toBoolean() ?: false,
                namaKasir = detail.transaction.namaKasir ?: settingsMap["nama_kasir"] ?: ""
            )
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
                                        Text("${item.qty} x ${CurrencyUtil.formatRupiah(item.hargaSaatItu)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Text(CurrencyUtil.formatRupiah(item.subtotal), fontWeight = FontWeight.Bold)
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
                                    Text(CurrencyUtil.formatRupiah(transaction.total), fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Bayar")
                                    Text(CurrencyUtil.formatRupiah(transaction.bayar))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Kembali", fontWeight = FontWeight.SemiBold)
                                    Text(CurrencyUtil.formatRupiah(transaction.kembalian), fontWeight = FontWeight.SemiBold)
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
                        com.mekarsari.kasir.ui.kasir.ReceiptPreview(
                            receiptText = receiptStr,
                            logoUri = logoUri,
                            showLogo = settingsMap["show_logo"]?.toBoolean() ?: true,
                            logoWidthChar = settingsMap["logo_width_char"]?.toIntOrNull()?.let { if (it > 0) it else 12 } ?: 12
                        )
                    }
                }

                var showDeleteConfirmDialog by remember { mutableStateOf(false) }

                if (showDeleteConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmDialog = false },
                        title = { Text("Hapus Transaksi") },
                        text = { Text("Apakah Anda yakin ingin menghapus transaksi TX#${detail.transaction.id}? Tindakan ini tidak dapat dibatalkan.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirmDialog = false
                                    viewModel.deleteTransaction(detail.transaction.id) {
                                        Toast.makeText(context, "Transaksi berhasil dihapus", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    }
                                }
                            ) {
                                Text("Hapus", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                Text("Batal")
                            }
                        }
                    )
                }

                // Action buttons grouped closely together
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier
                                .weight(0.18f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Transaksi",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        Button(
                            onClick = {
                                kasirViewModel.startEditingTransaction(detail)
                                navController.navigate(com.mekarsari.kasir.ui.navigation.Screen.Kasir.route) {
                                    popUpTo(com.mekarsari.kasir.ui.navigation.Screen.Kasir.route) {
                                        inclusive = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(0.82f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Transaksi", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            showPrintConfirmDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cetak Ulang Struk", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showPrintConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showPrintConfirmDialog = false },
                    title = { Text("Konfirmasi Cetak Ulang") },
                    text = { Text("Apakah Anda yakin ingin mencetak ulang struk transaksi ini? Pastikan kertas printer thermal tersedia.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showPrintConfirmDialog = false
                                scope.launch {
                                    val macAddress = settingsMap["printer_mac"] ?: ""
                                    val logoBitmap = com.mekarsari.kasir.printer.BitmapHelper.loadBitmapFromUri(context, logoUri)
                                    
                                    if (macAddress.isEmpty()) {
                                        Toast.makeText(context, "Atur printer Bluetooth di halaman Settings terlebih dahulu", Toast.LENGTH_LONG).show()
                                        return@launch
                                    }

                                    val logoWidth = settingsMap["logo_width_char"]?.toIntOrNull()?.let { if (it > 0) it else 12 } ?: 12
                                    val printResult = printerManager.printReceipt(macAddress, receiptStr, logoBitmap, logoWidth)
                                    if (printResult.isSuccess) {
                                        Toast.makeText(context, "Struk dicetak ulang", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Gagal mencetak: ${printResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        ) {
                            Text("Cetak")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showPrintConfirmDialog = false }
                        ) {
                            Text("Batal")
                        }
                    }
                )
            }
        } ?: run {
            Box(modifier = modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }
        }
    }
}
