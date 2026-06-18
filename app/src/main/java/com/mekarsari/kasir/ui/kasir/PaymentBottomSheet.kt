package com.mekarsari.kasir.ui.kasir

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mekarsari.kasir.data.local.entity.Transaction
import com.mekarsari.kasir.data.local.entity.TransactionItem
import com.mekarsari.kasir.printer.BluetoothPrinterManager
import com.mekarsari.kasir.printer.ReceiptFormatter
import com.mekarsari.kasir.util.CurrencyUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentBottomSheet(
    viewModel: KasirViewModel,
    onDismiss: () -> Unit,
    onPaymentSuccess: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    val currentCart by viewModel.cart.collectAsState()
    val initialTaxPercentage by viewModel.taxPercentage.collectAsState()
    val nomorMeja by viewModel.nomorMeja.collectAsState()

    var applyTax by remember { mutableStateOf(initialTaxPercentage > 0.0) }

    LaunchedEffect(initialTaxPercentage) {
        applyTax = initialTaxPercentage > 0.0
    }

    val isTaxInputError = false

    val activeTaxPercent = if (applyTax) {
        initialTaxPercentage
    } else {
        0.0
    }

    val activeSubtotal = currentCart.sumOf { it.subtotal }
    val activeTaxAmount = (activeSubtotal * (activeTaxPercent / 100.0)).toLong()
    val activeTotal = activeSubtotal + activeTaxAmount

    val payAmount by viewModel.payAmount.collectAsState()
    val activeChange = if (payAmount >= activeTotal) payAmount - activeTotal else 0L

    LaunchedEffect(activeTotal) {
        viewModel.setPayAmount(activeTotal)
    }

    val settingsMap by viewModel.settingsMap.collectAsState()

    val storeLogoUri = settingsMap["logo_uri"] ?: ""
    val storeName = settingsMap["nama_toko"] ?: "Mekar Sari"
    val storeAddress = settingsMap["alamat_toko"] ?: ""
    val storeAddress2 = settingsMap["alamat_toko2"] ?: ""
    val rHeader = settingsMap["receipt_header"] ?: ""
    val rFooter1 = settingsMap["receipt_footer1"] ?: ""
    val rFooter2 = settingsMap["receipt_footer2"] ?: ""
    val rSpacingTop = settingsMap["receipt_spacing_top"]?.toIntOrNull() ?: 1
    val rSpacingBottom = settingsMap["receipt_spacing_bottom"]?.toIntOrNull() ?: 4

    val printerManager = remember { BluetoothPrinterManager(context) }
    val formatter = remember { ReceiptFormatter() }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Pesanan", "Pratinjau & Bayar")
    val canCheckout = payAmount >= activeTotal && currentCart.isNotEmpty() && !isTaxInputError && nomorMeja.isNotEmpty()

    var showPrintConfirmDialog by remember { mutableStateOf(false) }

    // Generate receipt text reactively so it exactly matches printed receipt
    val receiptStr = remember(settingsMap, currentCart, activeSubtotal, activeTaxPercent, activeTaxAmount, activeTotal, payAmount, activeChange, nomorMeja) {
        val dummyTxId = 0
        val dummyTx = Transaction(
            id = dummyTxId,
            total = activeTotal,
            bayar = payAmount,
            kembalian = activeChange,
            nomorMeja = if (nomorMeja.isBlank()) null else nomorMeja,
            createdAt = System.currentTimeMillis()
        )
        val dummyItems = currentCart.map { item ->
            TransactionItem(
                transactionId = dummyTxId,
                productId = item.product.id,
                namaProdukSnapshot = item.product.nama,
                hargaSaatItu = item.customHarga,
                qty = item.quantity,
                subtotal = item.subtotal,
                porsiCustom = item.customPortion
            )
        }
        val txWithItems = com.mekarsari.kasir.data.local.dao.TransactionWithItems(dummyTx, dummyItems)
        
        formatter.format(
            shopName = storeName,
            shopAddress = storeAddress,
            shopAddress2 = storeAddress2,
            transactionWithItems = txWithItems,
            customHeader = rHeader,
            customFooter1 = rFooter1,
            customFooter2 = rFooter2,
            spacingTop = rSpacingTop,
            spacingBottom = rSpacingBottom,
            showLogo = settingsMap["show_logo"]?.toBoolean() ?: true,
            showReceiptCode = settingsMap["show_receipt_code"]?.toBoolean() ?: false,
            showSeqNumber = settingsMap["show_seq_number"]?.toBoolean() ?: false,
            showUnitQty = settingsMap["show_unit_qty"]?.toBoolean() ?: false,
            showNomorMeja = settingsMap["show_nomor_meja"]?.toBoolean() ?: true,
            showReceiptNumber = settingsMap["show_receipt_number"]?.toBoolean() ?: true,
            showTotalQty = settingsMap["show_total_qty"]?.toBoolean() ?: false,
            showSignatureSection = settingsMap["show_signature_section"]?.toBoolean() ?: false,
            namaKasir = settingsMap["nama_kasir"] ?: "Kasir 1"
        )
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )

    var handleDragTotal by remember { mutableStateOf(0f) }
    val dismissThresholdPx = 200f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            if (delta > 0) handleDragTotal += delta
                        },
                        onDragStopped = {
                            if (handleDragTotal >= dismissThresholdPx) {
                                scope.launch {
                                    sheetState.hide()
                                    onDismiss()
                                }
                            }
                            handleDragTotal = 0f
                        },
                        onDragStarted = { handleDragTotal = 0f }
                    ),
                contentAlignment = Alignment.Center
            ) {
                BottomSheetDefaults.DragHandle()
            }
        },
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            selectedTab = index
                        },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Detail Pesanan",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = applyTax,
                                        onCheckedChange = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            applyTax = it
                                        }
                                    )
                                    Text("Pajak", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                if (currentCart.isNotEmpty()) {
                                    IconButton(onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        viewModel.clearCart()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Kosongkan",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            if (currentCart.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Keranjang Kosong")
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(currentCart) { item ->
                                        CartItemRow(
                                            item = item,
                                            onIncrement = { viewModel.incrementQuantity(item) },
                                            onDecrement = { viewModel.decrementQuantity(item) },
                                            onEditPrice = { newPrice -> viewModel.updateItemPrice(item, newPrice) },
                                            onEditPortion = { portion -> viewModel.updateItemPortion(item, portion) }
                                        )
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Nomor Meja", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    val tables = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "SR", "SRL")
                                    val chunks = tables.chunked(7)
                                    chunks.forEach { chunk ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            chunk.forEach { tableNum ->
                                                val isSelected = nomorMeja == tableNum
                                                val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

                                                Surface(
                                                    onClick = {
                                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                        if (isSelected) {
                                                            viewModel.setNomorMeja("")
                                                        } else {
                                                            viewModel.setNomorMeja(tableNum)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(36.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = containerColor,
                                                    contentColor = contentColor,
                                                    border = BorderStroke(1.dp, borderColor)
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.fillMaxSize()
                                                    ) {
                                                        Text(
                                                            text = tableNum,
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }
                                            }
                                            repeat(7 - chunk.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Pratinjau Struk & Pembayaran",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        ReceiptPreview(
                            receiptText = receiptStr,
                            logoUri = storeLogoUri,
                            showLogo = settingsMap["show_logo"]?.toBoolean() ?: true,
                            logoWidthChar = settingsMap["logo_width_char"]?.toIntOrNull()?.let { if (it > 0) it else 12 } ?: 12
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = if (payAmount == 0L) "" else payAmount.toString(),
                                    onValueChange = {
                                        val parsed = it.toLongOrNull() ?: 0L
                                        viewModel.setPayAmount(parsed)
                                    },
                                    label = { Text("Jumlah Tunai Diterima") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val quickAmounts = listOf(null, 20000L, 50000L, 100000L)
                                    quickAmounts.forEach { amt ->
                                        val label = if (amt == null) "Uang Pas" else CurrencyUtil.formatRupiah(amt).replace("Rp ", "")
                                        val value = amt ?: activeTotal

                                        OutlinedButton(
                                            onClick = {
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                viewModel.setPayAmount(value)
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Text(label, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Kembalian", fontWeight = FontWeight.Bold)
                                    val successColor = if (payAmount >= activeTotal) Color(0xFF1B5E20) else MaterialTheme.colorScheme.error
                                    Text(
                                        text = CurrencyUtil.formatRupiah(activeChange),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = successColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showPrintConfirmDialog = true
                    },
                    enabled = canCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Bayar & Cetak Struk", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        scope.launch {
                            val result = viewModel.checkout(activeTaxPercent, nomorMeja)
                            if (result.isSuccess) {
                                onPaymentSuccess(result.getOrNull() ?: 0)
                            } else {
                                Toast.makeText(context, "Checkout gagal: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = canCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Simpan Transaksi", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showPrintConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPrintConfirmDialog = false },
            title = { Text("Konfirmasi Cetak Struk") },
            text = { Text("Apakah Anda yakin ingin menyelesaikan pembayaran dan mencetak struk transaksi? Pastikan kertas printer thermal tersedia.") },
            confirmButton = {
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showPrintConfirmDialog = false
                        scope.launch {
                            val macAddress = viewModel.getPrinterMac()
                            val logoUri = viewModel.getLogoUri()
                            val logoBitmap = com.mekarsari.kasir.printer.BitmapHelper.loadBitmapFromUri(context, logoUri)

                            val savedPayAmount = payAmount
                            val savedChange = activeChange
                            val cartSnapshot = currentCart.toList()

                            val result = viewModel.checkout(activeTaxPercent, nomorMeja)
                            if (result.isSuccess) {
                                val newTxId = result.getOrNull() ?: 0

                                if (macAddress.isNotEmpty()) {
                                    val finalTxWithItems = com.mekarsari.kasir.data.local.dao.TransactionWithItems(
                                        transaction = Transaction(
                                            id = newTxId,
                                            total = activeTotal,
                                            bayar = savedPayAmount,
                                            kembalian = savedChange,
                                            nomorMeja = if (nomorMeja.isBlank()) null else nomorMeja,
                                            createdAt = System.currentTimeMillis()
                                        ),
                                        items = cartSnapshot.map { item ->
                                            TransactionItem(
                                                transactionId = newTxId,
                                                productId = item.product.id,
                                                namaProdukSnapshot = item.product.nama,
                                                hargaSaatItu = item.customHarga,
                                                qty = item.quantity,
                                                subtotal = item.subtotal,
                                                porsiCustom = item.customPortion
                                            )
                                        }
                                    )
                                    val receiptStrFinal = formatter.format(
                                        shopName = storeName,
                                        shopAddress = storeAddress,
                                        shopAddress2 = storeAddress2,
                                        transactionWithItems = finalTxWithItems,
                                        customHeader = rHeader,
                                        customFooter1 = rFooter1,
                                        customFooter2 = rFooter2,
                                        spacingTop = rSpacingTop,
                                        spacingBottom = rSpacingBottom,
                                        showLogo = settingsMap["show_logo"]?.toBoolean() ?: true,
                                        showReceiptCode = settingsMap["show_receipt_code"]?.toBoolean() ?: false,
                                        showSeqNumber = settingsMap["show_seq_number"]?.toBoolean() ?: false,
                                        showUnitQty = settingsMap["show_unit_qty"]?.toBoolean() ?: false,
                                        showNomorMeja = settingsMap["show_nomor_meja"]?.toBoolean() ?: true,
                                        showReceiptNumber = settingsMap["show_receipt_number"]?.toBoolean() ?: true,
                                        showTotalQty = settingsMap["show_total_qty"]?.toBoolean() ?: false,
                                        showSignatureSection = settingsMap["show_signature_section"]?.toBoolean() ?: false,
                                        namaKasir = settingsMap["nama_kasir"] ?: "Kasir 1"
                                    )
                                    val logoWidth = settingsMap["logo_width_char"]?.toIntOrNull()?.let { if (it > 0) it else 12 } ?: 12
                                    printerManager.printReceipt(macAddress, receiptStrFinal, logoBitmap, logoWidth)
                                } else {
                                    Toast.makeText(context, "Printer belum diatur, struk gagal dicetak", Toast.LENGTH_SHORT).show()
                                }

                                onPaymentSuccess(newTxId)
                            } else {
                                Toast.makeText(context, "Checkout gagal: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Cetak")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showPrintConfirmDialog = false
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
