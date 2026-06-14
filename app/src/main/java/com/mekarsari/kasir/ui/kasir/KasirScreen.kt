package com.mekarsari.kasir.ui.kasir

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KasirScreen(
    viewModel: KasirViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val subtotal by viewModel.subtotal.collectAsState()
    val total by viewModel.total.collectAsState()
    val taxPercentage by viewModel.taxPercentage.collectAsState()

    val currentGroupSortOption by viewModel.groupSortOption.collectAsState()
    var showPaymentSheet by remember { mutableStateOf(false) }

    val groupedProducts = remember(filteredProducts, currentGroupSortOption) {
        if (currentGroupSortOption == KasirViewModel.GroupSortOption.CATEGORY) {
            filteredProducts.groupBy { 
                val kat = it.kategori?.trim()
                if (kat.isNullOrEmpty()) {
                    "Lainnya"
                } else {
                    kat.lowercase().replaceFirstChar { c -> c.uppercase() }
                }
            }.toSortedMap()
        } else {
            sortedMapOf("" to filteredProducts.sortedBy { it.nama })
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (cart.isNotEmpty()) 80.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Search on top, GroupBy chips below
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Cari menu berdasarkan nama / jenis...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currentGroupSortOption == KasirViewModel.GroupSortOption.CATEGORY,
                        onClick = { viewModel.setGroupSortOption(KasirViewModel.GroupSortOption.CATEGORY) },
                        label = { Text("Grup Jenis") }
                    )
                    FilterChip(
                        selected = currentGroupSortOption == KasirViewModel.GroupSortOption.ALPHABETICAL,
                        onClick = { viewModel.setGroupSortOption(KasirViewModel.GroupSortOption.ALPHABETICAL) },
                        label = { Text("Urut A-Z") }
                    )
                }
            }

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Produk tidak ditemukan")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    groupedProducts.forEach { (category, productList) ->
                        if (currentGroupSortOption == KasirViewModel.GroupSortOption.CATEGORY && category.isNotEmpty()) {
                            stickyHeader {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = category.uppercase(),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        items(productList, key = { it.id }) { product ->
                            CompactProductRow(
                                product = product,
                                showCategoryLabel = currentGroupSortOption != KasirViewModel.GroupSortOption.CATEGORY,
                                onClick = { viewModel.addToCart(product) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Bubble at the bottom
        if (cart.isNotEmpty()) {
            val totalQty = cart.sumOf { it.quantity }
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$totalQty Item Terpilih",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        val bubbleSubtitle = if (taxPercentage > 0.0) {
                            "Subtotal: ${formatRupiah(subtotal)} | Total (+PPN): ${formatRupiah(total)}"
                        } else {
                            "Total: ${formatRupiah(total)}"
                        }
                        Text(
                            text = bubbleSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            maxLines = 2
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { showPaymentSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Bayar", fontWeight = FontWeight.Bold)
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
fun CompactProductRow(
    product: Product,
    showCategoryLabel: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.nama,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (showCategoryLabel && !product.kategori.isNullOrEmpty()) {
                    Text(
                        text = product.kategori,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Text(
                text = formatRupiah(product.harga),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
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
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit harga",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
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

    val currentCart by viewModel.cart.collectAsState()
    val initialTaxPercentage by viewModel.taxPercentage.collectAsState()
    val nomorMeja by viewModel.nomorMeja.collectAsState()

    var applyTax by remember { mutableStateOf(initialTaxPercentage > 0.0) }
    var customTaxPercentText by remember { mutableStateOf("") }

    LaunchedEffect(initialTaxPercentage) {
        customTaxPercentText = initialTaxPercentage.toString()
        applyTax = initialTaxPercentage > 0.0
    }

    val isTaxInputError = applyTax && customTaxPercentText.toDoubleOrNull() == null

    val activeTaxPercent = if (applyTax) {
        customTaxPercentText.toDoubleOrNull() ?: 0.0
    } else {
        0.0
    }

    val activeSubtotal = currentCart.sumOf { it.subtotal }
    val activeTaxAmount = (activeSubtotal * (activeTaxPercent / 100.0)).toLong()
    val activeTotal = activeSubtotal + activeTaxAmount

    val payAmount by viewModel.payAmount.collectAsState()
    val activeChange = if (payAmount >= activeTotal) payAmount - activeTotal else 0L

    var storeName by remember { mutableStateOf("Mekar Sari") }
    var storeAddress by remember { mutableStateOf("") }
    var rHeader by remember { mutableStateOf("") }
    var rFooter1 by remember { mutableStateOf("") }
    var rFooter2 by remember { mutableStateOf("") }
    var rSpacingTop by remember { mutableStateOf(1) }
    var rSpacingBottom by remember { mutableStateOf(4) }

    // On sheet open: default pay = exact total (uang pas) + load store/receipt settings
    LaunchedEffect(Unit) {
        // Compute initTotal using initialTaxPercentage (avoid forward-ref to activeSubtotal state)
        val initSubtotal = currentCart.sumOf { it.subtotal }
        val initTaxAmt = (initSubtotal * (initialTaxPercentage / 100.0)).toLong()
        viewModel.setPayAmount(initSubtotal + initTaxAmt)

        val details = viewModel.getStoreDetails()
        storeName = details.first
        storeAddress = details.second
        val rSettings = viewModel.getReceiptSettings()
        rHeader = rSettings["receipt_header"] ?: ""
        rFooter1 = rSettings["receipt_footer1"] ?: ""
        rFooter2 = rSettings["receipt_footer2"] ?: ""
        rSpacingTop = rSettings["receipt_spacing_top"]?.toIntOrNull() ?: 1
        rSpacingBottom = rSettings["receipt_spacing_bottom"]?.toIntOrNull() ?: 4
    }

    val printerManager = remember { BluetoothPrinterManager(context) }
    val formatter = remember { ReceiptFormatter() }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Pesanan", "Pratinjau & Bayar")
    val canCheckout = payAmount >= activeTotal && currentCart.isNotEmpty() && !isTaxInputError

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Tab selector
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    // ── Tab 0: Pesanan ──────────────────────────────
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
                            if (currentCart.isNotEmpty()) {
                                TextButton(onClick = { viewModel.clearCart() }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Kosongkan",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium
                                    )
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
                                            onEditPrice = { newPrice -> viewModel.updateItemPrice(item, newPrice) }
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
                                OutlinedTextField(
                                    value = nomorMeja,
                                    onValueChange = { viewModel.setNomorMeja(it) },
                                    label = { Text("Nomor Meja") },
                                    placeholder = { Text("Contoh: 05, VIP 2") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = applyTax,
                                            onCheckedChange = { applyTax = it }
                                        )
                                        Text("Terapkan Pajak", fontWeight = FontWeight.SemiBold)
                                    }

                                    if (applyTax) {
                                        OutlinedTextField(
                                            value = customTaxPercentText,
                                            onValueChange = { customTaxPercentText = it },
                                            label = { Text("%") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.width(80.dp),
                                            singleLine = true,
                                            isError = isTaxInputError
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Tab 1: Pratinjau & Bayar ────────────────────
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
                            shopName = storeName,
                            shopAddress = storeAddress,
                            customHeader = rHeader,
                            customFooter1 = rFooter1,
                            customFooter2 = rFooter2,
                            cartItems = currentCart,
                            subtotal = activeSubtotal,
                            taxPercentage = activeTaxPercent,
                            taxAmount = activeTaxAmount,
                            total = activeTotal,
                            payAmount = payAmount,
                            change = activeChange,
                            nomorMeja = nomorMeja,
                            spacingTop = rSpacingTop,
                            spacingBottom = rSpacingBottom
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
                                        val label = if (amt == null) "Uang Pas" else formatRupiah(amt).replace("Rp ", "")
                                        val value = amt ?: activeTotal

                                        OutlinedButton(
                                            onClick = { viewModel.setPayAmount(value) },
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
                                    Text(
                                        text = formatRupiah(activeChange),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (payAmount >= activeTotal) SuccessGreen else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Always-visible action buttons at the bottom — stacked so text never clips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val macAddress = viewModel.getPrinterMac()
                            val logoUri = viewModel.getLogoUri()
                            val logoBitmap = com.mekarsari.kasir.printer.BitmapHelper.loadBitmapFromUri(context, logoUri)

                            // Snapshot cart BEFORE checkout() clears it
                            val cartSnapshot = currentCart.toList()

                            val result = viewModel.checkout(activeTaxPercent, nomorMeja)
                            if (result.isSuccess) {
                                val newTxId = result.getOrNull() ?: 0

                                if (macAddress.isNotEmpty()) {
                                    val finalTxWithItems = com.mekarsari.kasir.data.local.dao.TransactionWithItems(
                                        transaction = Transaction(
                                            id = newTxId,
                                            total = activeTotal,
                                            bayar = payAmount,
                                            kembalian = activeChange,
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
                                                subtotal = item.subtotal
                                            )
                                        }
                                    )
                                    val receiptStr = formatter.format(
                                        storeName, storeAddress, finalTxWithItems,
                                        rHeader, rFooter1, rFooter2,
                                        spacingTop = rSpacingTop,
                                        spacingBottom = rSpacingBottom
                                    )
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
}

@Composable
fun ReceiptPreview(
    shopName: String,
    shopAddress: String,
    customHeader: String,
    customFooter1: String,
    customFooter2: String,
    cartItems: List<CartItem>,
    subtotal: Long,
    taxPercentage: Double,
    taxAmount: Long,
    total: Long,
    payAmount: Long,
    change: Long,
    nomorMeja: String = "",
    spacingTop: Int = 1,
    spacingBottom: Int = 4
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(spacingTop) {
                Spacer(modifier = Modifier.height(8.dp))
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

            Text(
                text = "================================",
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1
            )

            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateStr = sdf.format(Date())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tgl: $dateStr", fontSize = 10.sp, color = Color.Black)
                Text("No: TX#(Baru)", fontSize = 10.sp, color = Color.Black)
            }
            if (nomorMeja.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Text("Meja: $nomorMeja", fontSize = 10.sp, color = Color.Black)
                }
            }

            Text(
                text = "--------------------------------",
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1
            )

            // Items List
            cartItems.forEach { item ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item.product.nama,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "  ${item.quantity} x ${formatRupiah(item.customHarga)}",
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
            if (taxAmount > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", fontSize = 11.sp, color = Color.Black)
                    Text(formatRupiah(subtotal), fontSize = 11.sp, color = Color.Black)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pajak ($taxPercentage%)", fontSize = 11.sp, color = Color.Black)
                    Text(formatRupiah(taxAmount), fontSize = 11.sp, color = Color.Black)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(formatRupiah(total), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bayar", fontSize = 11.sp, color = Color.Black)
                Text(formatRupiah(payAmount), fontSize = 11.sp, color = Color.Black)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kembali", fontSize = 11.sp, color = Color.Black)
                Text(formatRupiah(change), fontSize = 11.sp, color = Color.Black)
            }

            Text(
                text = "================================",
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1
            )

            if (customFooter1.isNotEmpty()) {
                Text(
                    text = customFooter1,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray
                )
            }
            if (customFooter2.isNotEmpty()) {
                Text(
                    text = customFooter2,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray
                )
            }
            repeat(spacingBottom) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

fun formatRupiah(value: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    format.maximumFractionDigits = 0
    return format.format(value).replace("Rp", "Rp ")
}
