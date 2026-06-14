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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mekarsari.kasir.data.local.entity.Product
import java.text.NumberFormat
import java.util.Locale

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
    var groupByKategori by remember { mutableStateOf(false) }

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

    val groupedProducts = remember(filteredProducts, groupByKategori) {
        if (groupByKategori) {
            filteredProducts.groupBy { 
                val kat = it.kategori?.trim()
                if (kat.isNullOrEmpty()) {
                    "Lainnya"
                } else {
                    kat.lowercase().replaceFirstChar { c -> c.uppercase() }
                }
            }.toSortedMap()
        } else {
            sortedMapOf("" to filteredProducts)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToForm(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Produk")
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
            Text(
                text = "Kelola Produk",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

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
                    singleLine = true
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

                    // Group By Category Toggle
                    FilterChip(
                        selected = groupByKategori,
                        onClick = { groupByKategori = !groupByKategori },
                        label = { Text("Grup Jenis") }
                    )
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
                        if (groupByKategori && category.isNotEmpty()) {
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
                            ProductDetailRow(
                                product = product,
                                onEdit = { onNavigateToForm(product.id) },
                                onDelete = { viewModel.deleteProduct(product) }
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
    onDelete: () -> Unit
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
