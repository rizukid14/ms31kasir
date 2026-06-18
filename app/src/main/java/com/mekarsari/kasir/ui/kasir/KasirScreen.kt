package com.mekarsari.kasir.ui.kasir

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mekarsari.kasir.data.local.entity.Product
import com.mekarsari.kasir.util.CurrencyUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KasirScreen(
    viewModel: KasirViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val subtotal by viewModel.subtotal.collectAsState()
    val total by viewModel.total.collectAsState()
    val taxPercentage by viewModel.taxPercentage.collectAsState()

    val currentGroupSortOption by viewModel.groupSortOption.collectAsState()
    var showPaymentSheet by remember { mutableStateOf(false) }
    val collapsedCategories = remember { mutableStateMapOf<String, Boolean>() }

    val listState = rememberLazyListState()
    var previousCartSize by remember { mutableStateOf(0) }
    LaunchedEffect(cart) {
        if (cart.isEmpty() && previousCartSize > 0) {
            listState.animateScrollToItem(0)
        }
        previousCartSize = cart.size
    }

    val customProductOrder by viewModel.customProductOrder.collectAsState()
    val popularProductIds by viewModel.popularProductIds.collectAsState()

    val groupedProducts = remember(filteredProducts, currentGroupSortOption, customProductOrder, popularProductIds) {
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
            KasirViewModel.GroupSortOption.TERLARIS -> {
                if (popularProductIds.isEmpty()) {
                    val orderMap = customProductOrder.withIndex().associate { it.value to it.index }
                    val sortedByDefault = filteredProducts.sortedWith(compareBy { orderMap[it.id] ?: Int.MAX_VALUE })
                    sortedMapOf("" to sortedByDefault)
                } else {
                    val popularMap = popularProductIds.withIndex().associate { it.value to it.index }
                    val sortedByPopularity = filteredProducts.sortedWith(
                        compareBy<Product> { popularMap[it.id] ?: Int.MAX_VALUE }
                            .thenBy { it.nama }
                    )
                    sortedMapOf("" to sortedByPopularity)
                }
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
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.cancelEditing()
                            }
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
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.setGroupSortOption(KasirViewModel.GroupSortOption.DEFAULT)
                            },
                            label = { Text("DEFAULT", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = currentGroupSortOption == KasirViewModel.GroupSortOption.ALPHABETICAL,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.setGroupSortOption(KasirViewModel.GroupSortOption.ALPHABETICAL)
                            },
                            label = { Text("A-Z", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = currentGroupSortOption == KasirViewModel.GroupSortOption.TERLARIS,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.setGroupSortOption(KasirViewModel.GroupSortOption.TERLARIS)
                            },
                            label = { Text("TERLARIS", fontSize = 11.sp) }
                        )
                    }
                    if (cart.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.clearCart()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
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
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    groupedProducts.forEach { (category, productList) ->
                        val isCollapsed = collapsedCategories[category] ?: false
                        if (currentGroupSortOption == KasirViewModel.GroupSortOption.DEFAULT && category.isNotEmpty()) {
                            stickyHeader {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            collapsedCategories[category] = !isCollapsed
                                        },
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = category.uppercase(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Icon(
                                            imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                            contentDescription = if (isCollapsed) "Buka" else "Tutup",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (currentGroupSortOption != KasirViewModel.GroupSortOption.DEFAULT || !isCollapsed) {
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
        }

        // Floating Bubble at the bottom
        if (cart.isNotEmpty()) {
            val totalQty = cart.sumOf { it.quantity }
            Card(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showPaymentSheet = true
                },
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
                            "Subtotal: ${CurrencyUtil.formatRupiah(subtotal)} | Total (+PPN): ${CurrencyUtil.formatRupiah(total)}"
                        } else {
                            "Total: ${CurrencyUtil.formatRupiah(total)}"
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
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showPaymentSheet = true
                        },
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
