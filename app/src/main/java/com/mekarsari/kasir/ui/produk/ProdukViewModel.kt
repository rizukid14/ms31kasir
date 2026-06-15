package com.mekarsari.kasir.ui.produk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mekarsari.kasir.data.local.entity.Product
import com.mekarsari.kasir.data.repository.ProductRepository
import com.mekarsari.kasir.data.repository.SettingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProdukViewModel(
    private val productRepository: ProductRepository,
    private val settingRepository: SettingRepository
) : ViewModel() {

    val allProducts: StateFlow<List<Product>> = productRepository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val customProductOrder: StateFlow<List<Int>> = settingRepository.allSettings
        .map { list ->
            val orderStr = list.firstOrNull { it.key == "custom_product_order" }?.value ?: ""
            if (orderStr.isEmpty()) emptyList() else orderStr.split(",").mapNotNull { it.toIntOrNull() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun moveProductUp(product: Product) {
        viewModelScope.launch {
            val currentList = allProducts.value.map { it.id }
            val order = customProductOrder.value.toMutableList()
            currentList.forEach { id ->
                if (!order.contains(id)) {
                    order.add(id)
                }
            }
            val cleanOrder = order.filter { currentList.contains(it) }.toMutableList()
            val index = cleanOrder.indexOf(product.id)
            if (index > 0) {
                val temp = cleanOrder[index]
                cleanOrder[index] = cleanOrder[index - 1]
                cleanOrder[index - 1] = temp
                settingRepository.saveSetting("custom_product_order", cleanOrder.joinToString(","))
            }
        }
    }

    fun moveProductDown(product: Product) {
        viewModelScope.launch {
            val currentList = allProducts.value.map { it.id }
            val order = customProductOrder.value.toMutableList()
            currentList.forEach { id ->
                if (!order.contains(id)) {
                    order.add(id)
                }
            }
            val cleanOrder = order.filter { currentList.contains(it) }.toMutableList()
            val index = cleanOrder.indexOf(product.id)
            if (index >= 0 && index < cleanOrder.size - 1) {
                val temp = cleanOrder[index]
                cleanOrder[index] = cleanOrder[index + 1]
                cleanOrder[index + 1] = temp
                settingRepository.saveSetting("custom_product_order", cleanOrder.joinToString(","))
            }
        }
    }

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
