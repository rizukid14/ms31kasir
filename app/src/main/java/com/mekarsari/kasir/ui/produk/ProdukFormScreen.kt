package com.mekarsari.kasir.ui.produk

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdukFormScreen(
    productId: Int?,
    viewModel: ProdukViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var nama by remember { mutableStateOf("") }
    var hargaString by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf("") }
    var stokString by remember { mutableStateOf("0") }

    var isEditMode by remember { mutableStateOf(false) }
    
    var showErrors by remember { mutableStateOf(false) }
    val isNamaError = showErrors && nama.isBlank()
    val isHargaError = showErrors && (hargaString.isBlank() || hargaString.toLongOrNull() == null || (hargaString.toLongOrNull() ?: 0L) <= 0L)

    LaunchedEffect(productId) {
        if (productId != null && productId != 0) {
            isEditMode = true
            scope.launch {
                val product = viewModel.getProductById(productId)
                if (product != null) {
                    nama = product.nama
                    hargaString = product.harga.toString()
                    kategori = product.kategori ?: ""
                    stokString = product.stok.toString()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Produk" else "Tambah Produk") },
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = nama,
                onValueChange = { 
                    nama = it 
                    if (it.isNotBlank()) showErrors = false
                },
                label = { Text("Nama Produk") },
                isError = isNamaError,
                supportingText = if (isNamaError) { { Text("Nama produk tidak boleh kosong") } } else null,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = hargaString,
                onValueChange = { 
                    hargaString = it 
                    val parsed = it.toLongOrNull()
                    if (it.isNotBlank() && parsed != null && parsed > 0L) showErrors = false
                },
                label = { Text("Harga (Rupiah)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = isHargaError,
                supportingText = if (isHargaError) { { Text("Harga produk wajib diisi dan harus lebih dari 0") } } else null,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = kategori,
                onValueChange = { kategori = it },
                label = { Text("Kategori (Opsional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = stokString,
                onValueChange = { stokString = it },
                label = { Text("Stok Produk") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val harga = hargaString.toLongOrNull()
                    val stok = stokString.toIntOrNull() ?: 0
                    if (nama.isBlank() || hargaString.isBlank() || harga == null || harga <= 0L) {
                        showErrors = true
                        Toast.makeText(context, "Mohon lengkapi data produk dengan benar", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.saveProduct(
                            id = productId ?: 0,
                            nama = nama,
                            harga = harga,
                            stok = stok,
                            kategori = kategori.ifEmpty { null }
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditMode) "Simpan Perubahan" else "Tambah Produk",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
