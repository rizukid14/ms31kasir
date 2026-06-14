package com.mekarsari.kasir.ui.produk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mekarsari.kasir.data.local.entity.Product
import com.mekarsari.kasir.data.repository.ProductRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProdukViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    val allProducts: StateFlow<List<Product>> = productRepository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun getProductById(id: Int): Product? {
        return productRepository.getProductById(id)
    }

    fun saveProduct(id: Int, nama: String, harga: Long, stok: Int, kategori: String?) {
        viewModelScope.launch {
            val product = Product(id = id, nama = nama, harga = harga, stok = stok, kategori = kategori)
            if (id == 0) {
                productRepository.insertProduct(product)
            } else {
                productRepository.updateProduct(product)
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
        }
    }
}
