package com.mekarsari.kasir.ui.produk

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mekarsari.kasir.data.local.entity.Product
import java.text.NumberFormat
import java.util.Locale
import com.mekarsari.kasir.ui.theme.appTextFieldColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProdukScreen(
    viewModel: ProdukViewModel,
    onNavigateToForm: (productId: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val products by viewModel.allProducts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Semua") }
    var sortByAZ by remember { mutableStateOf(false) }
    val collapsedCategories = remember { mutableStateMapOf<String, Boolean>() }

    val categories = remember(products) {
        listOf("Semua") + products.mapNotNull { it.kategori }.distinct().sorted()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategoryFilter) {
        products.filter { product ->
            val matchesSearch = product.nama.contains(searchQuery, ignoreCase = true) ||
                    (product.kategori?.contains(searchQuery, ignoreCase = true) ?: false)
            val matchesCategory = selectedCategoryFilter == "Semua" || product.kategori == selectedCategoryFilter
            matchesSearch && matchesCategory
        }
    }

    val customProductOrder by viewModel.customProductOrder.collectAsState()

    val sortedProducts = remember(filteredProducts, customProductOrder, sortByAZ) {
        if (sortByAZ) {
            filteredProducts.sortedBy { it.nama.lowercase() }
        } else {
            val orderMap = customProductOrder.withIndex().associate { it.value to it.index }
            filteredProducts.sortedWith(compareBy { orderMap[it.id] ?: Int.MAX_VALUE })
        }
    }

    val groupedProducts = remember(sortedProducts) {
        sortedProducts.groupBy { 
            val kat = it.kategori?.trim()
            if (kat.isNullOrEmpty()) {
                "Lainnya"
            } else {
                kat.lowercase().replaceFirstChar { c -> c.uppercase() }
            }
        }.toSortedMap()
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Daftar Produk", "Urutkan Menu")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { onNavigateToForm(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Produk")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kelola Produk",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (selectedTab == 0) {
                // ── TAB 1: Kelola Produk (Normal View) ──────────────────────────────
                // Filter Controls — search full-width on top, dropdowns below
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari produk...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = appTextFieldColors()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category Filter Dropdown
                        var categoryMenuExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { categoryMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedCategoryFilter, maxLines = 1)
                            }
                            DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            selectedCategoryFilter = cat
                                            categoryMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Symmetrical Sort A-Z Toggle Button
                        OutlinedButton(
                            onClick = { sortByAZ = !sortByAZ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (sortByAZ) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (sortByAZ) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (sortByAZ) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text(if (sortByAZ) "✓ Urutan: A-Z" else "Urutan: A-Z", maxLines = 1)
                        }
                    }
                }

                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (products.isEmpty()) "Belum ada produk. Tambah produk baru!" else "Produk tidak ditemukan",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedProducts.forEach { (category, productList) ->
                            val isCollapsed = collapsedCategories[category] ?: false
                            if (category.isNotEmpty()) {
                                stickyHeader {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { collapsedCategories[category] = !isCollapsed },
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.small
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

                            if (!isCollapsed) {
                                items(productList, key = { it.id }) { product ->
                                    ProductDetailRow(
                                        product = product,
                                        onEdit = { onNavigateToForm(product.id) },
                                        onDelete = { viewModel.deleteProduct(product) },
                                        modifier = Modifier.animateItemPlacement()
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ── TAB 2: Urutkan Menu (Reorder Mode) ──────────────────────────────
                if (products.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tidak ada produk untuk diurutkan", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    val sortedAll = remember(products, customProductOrder) {
                        val orderMap = customProductOrder.withIndex().associate { it.value to it.index }
                        products.sortedWith(compareBy { orderMap[it.id] ?: Int.MAX_VALUE })
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedAll, key = { it.id }) { product ->
                            ReorderProductRow(
                                product = product,
                                onMoveUp = { viewModel.moveProductUp(product) },
                                onMoveDown = { viewModel.moveProductDown(product) },
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDetailRow(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    format.maximumFractionDigits = 0
    val hargaFormatted = format.format(product.harga).replace("Rp", "Rp ")

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Produk") },
            text = { Text("Yakin ingin menghapus \"${product.nama}\"? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.nama,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = hargaFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Stok: ${product.stok}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (product.stok > 0) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
                    )
                    if (!product.kategori.isNullOrEmpty()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(product.kategori) },
                            enabled = false,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ReorderProductRow(
    product: Product,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.nama,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMoveUp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Pindah Ke Atas",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Pindah Ke Bawah",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
