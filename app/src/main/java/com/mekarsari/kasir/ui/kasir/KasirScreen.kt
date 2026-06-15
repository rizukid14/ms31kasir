package com.mekarsari.kasir.ui.kasir

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.ui.graphics.asImageBitmap

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

    val customProductOrder by viewModel.customProductOrder.collectAsState()

    val groupedProducts = remember(filteredProducts, currentGroupSortOption, customProductOrder) {
        when (currentGroupSortOption) {
            KasirViewModel.GroupSortOption.DEFAULT -> {
                val orderMap = customProductOrder.withIndex().associate { it.value to it.index }
                val sortedByDefault = filteredProducts.sortedWith(compareBy { orderMap[it.id] ?: Int.MAX_VALUE })
                sortedByDefault.groupBy { 
                    val kat = it.kategori?.trim()
                    if (kat.isNullOrEmpty()) {
                        "Lainnya"
                    } else {
                        kat.lowercase().replaceFirstChar { c -> c.uppercase() }
                    }
                }.toSortedMap()
            }
            KasirViewModel.GroupSortOption.ALPHABETICAL -> {
                sortedMapOf("" to filteredProducts.sortedBy { it.nama })
            }
        }
    }

    val editingTransactionId by viewModel.editingTransactionId.collectAsState()

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
            if (editingTransactionId != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mengedit Transaksi TX#$editingTransactionId",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(
                            onClick = { viewModel.cancelEditing() }
                        ) {
                            Text("Batal", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Header: Search on top, GroupBy chips below
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Cari Menu...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = currentGroupSortOption == KasirViewModel.GroupSortOption.DEFAULT,
                            onClick = { viewModel.setGroupSortOption(KasirViewModel.GroupSortOption.DEFAULT) },
                            label = { Text("DEFAULT", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = currentGroupSortOption == KasirViewModel.GroupSortOption.ALPHABETICAL,
                            onClick = { viewModel.setGroupSortOption(KasirViewModel.GroupSortOption.ALPHABETICAL) },
                            label = { Text("A-Z", fontSize = 11.sp) }
                        )
                    }
                    if (cart.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearCart() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Batal Transaksi",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
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
                        if (currentGroupSortOption == KasirViewModel.GroupSortOption.DEFAULT && category.isNotEmpty()) {
                            stickyHeader {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
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
                             val qty = cart.filter { it.product.id == product.id }.sumOf { it.quantity }
                             CompactProductRow(
                                product = product,
                                quantity = qty,
                                showCategoryLabel = currentGroupSortOption != KasirViewModel.GroupSortOption.DEFAULT,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactProductRow(
    product: Product,
    quantity: Int = 0,
    showCategoryLabel: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = if (quantity > 0) 2.dp else 0.5.dp,
            color = if (quantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (quantity > 0) {
                        Text(
                            text = "${quantity}x ",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = product.nama,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (quantity > 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (showCategoryLabel && !product.kategori.isNullOrEmpty()) {
                    Text(
                        text = product.kategori,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = if (quantity > 0) 0.7f else 1.0f)
                    )
                }
            }
            Text(
                text = formatRupiah(product.harga),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (quantity > 0) 0.7f else 1.0f),
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
    onEditPrice: (Long) -> Unit,
    onEditPortion: (Double?) -> Unit
) {
    var showPriceEditDialog by remember { mutableStateOf(false) }
    var showPortionEditDialog by remember { mutableStateOf(false) }
    var tempPriceText by remember { mutableStateOf(item.customHarga.toString()) }
    var selectedPortion by remember { mutableStateOf<Double?>(item.customPortion) }

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
                    showPriceEditDialog = true
                }
        ) {
            val displayName = when {
                item.customPortion != null -> "${item.product.nama} (${item.customPortion} Porsi)"
                else -> item.product.nama
            }
            Text(
                text = displayName,
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

            // Clickable quantity text to adjust fractional portions
            Box(
                modifier = Modifier
                    .clickable {
                        selectedPortion = item.customPortion
                        showPortionEditDialog = true
                    }
                    .padding(horizontal = 8.dp)
            ) {
                val displayQty = if (item.customPortion != null) {
                    val totalPortion = item.customPortion * item.quantity
                    // Format to remove trailing zeros if it's a whole number (e.g. 4.0 -> 4)
                    if (totalPortion % 1.0 == 0.0) totalPortion.toInt().toString() else totalPortion.toString()
                } else {
                    item.quantity.toString()
                }
                Text(
                    text = displayQty,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.customPortion != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

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

    // Dialog 1: Sesuaikan Harga Barang (Klik Nama / Harga)
    if (showPriceEditDialog) {
        AlertDialog(
            onDismissRequest = { showPriceEditDialog = false },
            title = { Text("Sesuaikan Harga Barang") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Harga Master: ${formatRupiah(item.product.harga)}")
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempPriceText,
                        onValueChange = { tempPriceText = it },
                        label = { Text("Harga Baru (Rupiah)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
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
                        showPriceEditDialog = false
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onEditPrice(item.product.harga)
                        showPriceEditDialog = false
                    }
                ) {
                    Text("Reset Asli")
                }
            }
        )
    }

    // Dialog 2: Sesuaikan Porsi Pecahan (Klik Angka Quantity)
    if (showPortionEditDialog) {
        var tempPortionText by remember { mutableStateOf(item.customPortion?.toString() ?: "1.0") }

        AlertDialog(
            onDismissRequest = { showPortionEditDialog = false },
            title = { Text("Sesuaikan Jumlah Porsi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Harga Master: ${formatRupiah(item.product.harga)} / porsi")
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    OutlinedTextField(
                        value = tempPortionText,
                        onValueChange = { 
                            // Allow digits, dots, and empty string
                            if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) {
                                tempPortionText = it
                            }
                        },
                        label = { Text("Jumlah Porsi (Desimal, contoh: 4.6)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Pilihan Pintasan Cepat:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    
                    // Horizontal scrollable chips for portions 0.1 to 1.0
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val portions = listOf(0.5, 0.7, 1.0, 1.5, 2.5, 3.5, 4.5)
                        items(portions) { p ->
                            @OptIn(ExperimentalMaterial3Api::class)
                            FilterChip(
                                selected = tempPortionText.toDoubleOrNull() == p,
                                onClick = {
                                    tempPortionText = p.toString()
                                },
                                label = { Text(text = if (p == 1.0) "Normal (1.0)" else p.toString(), fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsedPortion = tempPortionText.toDoubleOrNull()
                        if (parsedPortion != null && parsedPortion > 0.0) {
                            val finalPortion = if (parsedPortion == 1.0) null else parsedPortion
                            onEditPortion(finalPortion)
                        }
                        showPortionEditDialog = false
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onEditPortion(null)
                        showPortionEditDialog = false
                    }
                ) {
                    Text("Reset Normal")
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
    var showPrintConfirmDialog by remember { mutableStateOf(false) }

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

    var storeLogoUri by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("Mekar Sari") }
    var storeAddress by remember { mutableStateOf("") }
    var storeAddress2 by remember { mutableStateOf("") }
    var rHeader by remember { mutableStateOf("") }
    var rFooter1 by remember { mutableStateOf("") }
    var rFooter2 by remember { mutableStateOf("") }
    var rSpacingTop by remember { mutableStateOf(1) }
    var rSpacingBottom by remember { mutableStateOf(4) }
    var rSettingsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // On sheet open: load store/receipt settings
    LaunchedEffect(Unit) {
        storeLogoUri = viewModel.getLogoUri()
        val details = viewModel.getStoreDetails()
        storeName = details.first
        storeAddress = details.second
        storeAddress2 = details.third
        val rSettings = viewModel.getReceiptSettings()
        rSettingsMap = rSettings
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
    val canCheckout = payAmount >= activeTotal && currentCart.isNotEmpty() && !isTaxInputError && nomorMeja.isNotEmpty()

    // Sheet cannot be dismissed by swiping the body — only via the custom drag handle below
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )

    // Track total drag distance on the handle so we can dismiss after a threshold
    var handleDragTotal by remember { mutableStateOf(0f) }
    val dismissThresholdPx = 200f // ~80dp worth of drag

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            // Custom drag handle: only this area can swipe-dismiss the sheet
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = applyTax,
                                        onCheckedChange = { applyTax = it }
                                    )
                                    Text("Pajak", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                if (currentCart.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearCart() }) {
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
                            shopAddress2 = storeAddress2,
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
                            spacingBottom = rSpacingBottom,
                            logoUri = storeLogoUri,
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
                        showPrintConfirmDialog = false
                        scope.launch {
                            val macAddress = viewModel.getPrinterMac()
                            val logoUri = viewModel.getLogoUri()
                            val logoBitmap = com.mekarsari.kasir.printer.BitmapHelper.loadBitmapFromUri(context, logoUri)

                            // Capture payment details and snapshot cart BEFORE checkout() clears them
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
                                    val receiptStr = formatter.format(
                                        shopName = storeName,
                                        shopAddress = storeAddress,
                                        shopAddress2 = storeAddress2,
                                        transactionWithItems = finalTxWithItems,
                                        customHeader = rHeader,
                                        customFooter1 = rFooter1,
                                        customFooter2 = rFooter2,
                                        spacingTop = rSpacingTop,
                                        spacingBottom = rSpacingBottom,
                                        showLogo = rSettingsMap["show_logo"]?.toBoolean() ?: true,
                                        showReceiptCode = rSettingsMap["show_receipt_code"]?.toBoolean() ?: false,
                                        showSeqNumber = rSettingsMap["show_seq_number"]?.toBoolean() ?: false,
                                        showUnitQty = rSettingsMap["show_unit_qty"]?.toBoolean() ?: false,
                                        showNomorMeja = rSettingsMap["show_nomor_meja"]?.toBoolean() ?: true,
                                        showReceiptNumber = rSettingsMap["show_receipt_number"]?.toBoolean() ?: true,
                                        showTotalQty = rSettingsMap["show_total_qty"]?.toBoolean() ?: false,
                                        showSignatureSection = rSettingsMap["show_signature_section"]?.toBoolean() ?: true,
                                        namaKasir = rSettingsMap["nama_kasir"] ?: "Kasir 1"
                                    )
                                    val logoWidth = rSettingsMap["logo_width_char"]?.toIntOrNull()?.let { if (it > 0) it else 12 } ?: 12
                                    printerManager.printReceipt(macAddress, receiptStr, logoBitmap, logoWidth)
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
                    onClick = { showPrintConfirmDialog = false }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ReceiptPreview(
    shopName: String,
    shopAddress: String,
    shopAddress2: String = "",
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
    spacingBottom: Int = 4,
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
                repeat(spacingTop) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
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
                        androidx.compose.foundation.Image(
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
                        text = "NO. URUT: 001",
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
                val dateStr = sdfDate.format(Date())
                val timeStr = sdfTime.format(Date())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dateStr, fontSize = 10.sp, color = Color.Black)
                    if (showReceiptNumber) {
                        Text("No: TX#(Baru)", fontSize = 10.sp, color = Color.Black)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(timeStr, fontSize = 10.sp, color = Color.Black)
                    if (namaKasir.isNotEmpty()) {
                        Text("Kasir: $namaKasir", fontSize = 10.sp, color = Color.Black)
                    }
                }

                if (showNomorMeja && nomorMeja.isNotEmpty()) {
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
                        val displayName = item.product.nama
                        Text(
                            text = displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val visualQty = if (item.customPortion != null) {
                                item.customPortion * item.quantity
                            } else {
                                item.quantity.toDouble()
                            }
                            val visualQtyStr = if (visualQty % 1.0 == 0.0) visualQty.toInt().toString() else visualQty.toString()

                            val unitPrice = if (item.customPortion != null) item.product.harga else item.customHarga
                            val qtyText = if (showUnitQty) {
                                val isMinuman = item.product.kategori == "Minuman"
                                val unit = if (isMinuman) "Gelas" else "Porsi"
                                "  $visualQtyStr $unit x ${formatRupiah(unitPrice)}"
                            } else {
                                "  $visualQtyStr x ${formatRupiah(unitPrice)}"
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
                if (showTotalQty) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Kuantitas", fontSize = 11.sp, color = Color.Black)
                        Text("${cartItems.sumOf { it.quantity }}", fontSize = 11.sp, color = Color.Black)
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
                repeat(spacingBottom) {
                    Spacer(modifier = Modifier.height(8.dp))
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
