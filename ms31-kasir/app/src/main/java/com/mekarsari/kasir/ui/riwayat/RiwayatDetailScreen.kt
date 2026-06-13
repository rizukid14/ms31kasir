package com.mekarsari.kasir.ui.riwayat

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(transactionId) {
        viewModel.selectTransaction(transactionId)
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
                // Header details Card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Informasi Transaksi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Waktu")
                            Text(dateFormatted, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Metode Pembayaran")
                            Text(transaction.metodePembayaran.uppercase(), fontWeight = FontWeight.SemiBold)
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

                // Print button
                Button(
                    onClick = {
                        scope.launch {
                            val macAddress = viewModel.getPrinterMac()
                            val store = viewModel.getStoreDetails()
                            val logoUri = viewModel.getLogoUri()
                            val logoBitmap = com.mekarsari.kasir.printer.BitmapHelper.loadBitmapFromUri(context, logoUri)
                            
                            if (macAddress.isEmpty()) {
                                Toast.makeText(context, "Atur printer Bluetooth di halaman Settings terlebih dahulu", Toast.LENGTH_LONG).show()
                                return@launch
                            }

                            val receiptStr = formatter.format(store.first, store.second, detail)
                            val printResult = printerManager.printReceipt(macAddress, receiptStr, logoBitmap)
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

private fun formatRupiah(value: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    format.maximumFractionDigits = 0
    return format.format(value).replace("Rp", "Rp ")
}
