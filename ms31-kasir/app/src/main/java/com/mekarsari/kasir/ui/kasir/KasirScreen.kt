package com.mekarsari.kasir.ui.kasir

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mekarsari.kasir.data.local.entity.Product
import com.mekarsari.kasir.data.local.entity.Transaction
import com.mekarsari.kasir.data.local.entity.TransactionItem
import com.mekarsari.kasir.domain.model.CartItem
import com.mekarsari.kasir.printer.BluetoothPrinterManager
import com.mekarsari.kasir.printer.ReceiptFormatter
import com.mekarsari.kasir.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirScreen(
    viewModel: KasirViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val total by viewModel.total.collectAsState()
    val subtotal by viewModel.subtotal.collectAsState()
    val taxPercentage by viewModel.taxPercentage.collectAsState()
    val taxAmount by viewModel.taxAmount.collectAsState()

    var showPaymentSheet by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: Products Catalog Grid
        Column(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Cari produk berdasarkan nama / kategori...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Produk tidak ditemukan")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductCard(
                            product = product,
                            onClick = { viewModel.addToCart(product) }
                        )
                    }
                }
            }
        }

        // Right Column: Active Order Cart Panel
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Keranjang Belanja",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (cart.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearCart() }) {
                                Text("Kosongkan", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    if (cart.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Keranjang Kosong", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cart) { item ->
                                CartItemRow(
                                    item = item,
                                    onIncrement = { viewModel.incrementQuantity(item) },
                                    onDecrement = { viewModel.decrementQuantity(item) },
                                    onEditPrice = { newPrice -> viewModel.updateItemPrice(item, newPrice) }
                                )
                            }
                        }
                    }
                }

                // Total Summary
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    if (taxPercentage > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = formatRupiah(subtotal),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pajak (${taxPercentage}%)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = formatRupiah(taxAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatRupiah(total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.setPayAmount(total)
                            showPaymentSheet = true
                        },
                        enabled = cart.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Bayar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }

    if (showPaymentSheet) {
        PaymentBottomSheet(
            viewModel = viewModel,
            onDismiss = { showPaymentSheet = false },
            onPaymentSuccess = { transactionId ->
                showPaymentSheet = false
                Toast.makeText(context, "Transaksi TX#$transactionId berhasil disimpan!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = product.nama,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = formatRupiah(product.harga),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!product.kategori.isNullOrEmpty()) {
                    Text(
                        text = product.kategori,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEditPrice: (Long) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var tempPriceText by remember { mutableStateOf(item.customHarga.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    tempPriceText = item.customHarga.toString()
                    showEditDialog = true
                }
        ) {
            Text(
                text = item.product.nama,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatRupiah(item.customHarga),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (item.customHarga != item.product.harga) {
                    Text(
                        text = "(${formatRupiah(item.product.harga)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDecrement,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("-", fontWeight = FontWeight.Bold)
            }

            Text(
                text = item.quantity.toString(),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedButton(
                onClick = onIncrement,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Sesuaikan Harga Barang") },
            text = {
                Column {
                    Text("Harga Master: ${formatRupiah(item.product.harga)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempPriceText,
                        onValueChange = { tempPriceText = it },
                        label = { Text("Harga Baru (Rupiah)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = tempPriceText.toLongOrNull()
                        if (parsed != null && parsed >= 0) {
                            onEditPrice(parsed)
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onEditPrice(item.product.harga)
                        showEditDialog = false
                    }
                ) {
                    Text("Reset Asli")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentBottomSheet(
    viewModel: KasirViewModel,
    onDismiss: () -> Unit,
    onPaymentSuccess: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val total by viewModel.total.collectAsState()
    val subtotal by viewModel.subtotal.collectAsState()
    val taxPercentage by viewModel.taxPercentage.collectAsState()
    val taxAmount by viewModel.taxAmount.collectAsState()
    val payAmount by viewModel.payAmount.collectAsState()
    val change by viewModel.change.collectAsState()

    val printerManager = remember { BluetoothPrinterManager(context) }
    val formatter = remember { ReceiptFormatter() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pembayaran",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (taxPercentage > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = formatRupiah(subtotal),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pajak ($taxPercentage%)", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = formatRupiah(taxAmount),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Tagihan", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatRupiah(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Jumlah Bayar", fontWeight = FontWeight.Bold)
                        Text(
                            text = formatRupiah(payAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kembalian")
                        Text(
                            text = formatRupiah(change),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (payAmount >= total) SuccessGreen else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Quick cash options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val amounts = listOf(null, 20000L, 50000L, 100000L)
                amounts.forEach { amt ->
                    val label = if (amt == null) "Uang Pas" else formatRupiah(amt).replace("Rp ", "")
                    val value = amt ?: total
                    
                    OutlinedButton(
                        onClick = { viewModel.setPayAmount(value) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(label, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }

            // Interactive Numpad
            Numpad(
                value = payAmount,
                onValueChange = { viewModel.setPayAmount(it) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val result = viewModel.checkout()
                            if (result.isSuccess) {
                                val newTxId = result.getOrNull() ?: 0
                                onPaymentSuccess(newTxId)
                            } else {
                                Toast.makeText(context, "Checkout gagal: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = payAmount >= total,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Bayar", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        scope.launch {
                            val macAddress = viewModel.getPrinterMac()
                            val store = viewModel.getStoreDetails()
                            val logoUri = viewModel.getLogoUri()
                            val logoBitmap = com.mekarsari.kasir.printer.BitmapHelper.loadBitmapFromUri(context, logoUri)
                            
                            val currentCart = viewModel.cart.value
                            val currentTotal = viewModel.total.value
                            val currentPay = viewModel.payAmount.value
                            val currentChange = viewModel.change.value

                            val result = viewModel.checkout()
                            if (result.isSuccess) {
                                val newTxId = result.getOrNull() ?: 0
                                
                                // Attempt bluetooth print
                                if (macAddress.isNotEmpty()) {
                                    val mockTxWithItems = com.mekarsari.kasir.data.local.dao.TransactionWithItems(
                                        transaction = Transaction(
                                            id = newTxId,
                                            total = currentTotal,
                                            bayar = currentPay,
                                            kembalian = currentChange,
                                            createdAt = System.currentTimeMillis()
                                        ),
                                        items = currentCart.map { item ->
                                            TransactionItem(
                                                transactionId = newTxId,
                                                productId = item.product.id,
                                                namaProdukSnapshot = item.product.nama,
                                                hargaSaatItu = item.customHarga,
                                                qty = item.quantity,
                                                subtotal = item.subtotal
                                            )
                                        }
                                    )
                                    val receiptStr = formatter.format(store.first, store.second, mockTxWithItems)
                                    printerManager.printReceipt(macAddress, receiptStr, logoBitmap)
                                } else {
                                    Toast.makeText(context, "Printer belum diatur, struk gagal dicetak", Toast.LENGTH_SHORT).show()
                                }
                                
                                onPaymentSuccess(newTxId)
                            } else {
                                Toast.makeText(context, "Checkout gagal: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = payAmount >= total,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cetak Struk", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun Numpad(
    value: Long,
    onValueChange: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⌫")
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    Button(
                        onClick = {
                            val str = value.toString()
                            when (key) {
                                "C" -> onValueChange(0L)
                                "⌫" -> {
                                    if (str.length > 1) {
                                        onValueChange(str.dropLast(1).toLongOrNull() ?: 0L)
                                    } else {
                                        onValueChange(0L)
                                    }
                                }
                                else -> {
                                    val newVal = if (value == 0L) key else str + key
                                    onValueChange(newVal.toLongOrNull() ?: value)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(key, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun formatRupiah(value: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    format.maximumFractionDigits = 0
    return format.format(value).replace("Rp", "Rp ")
}
