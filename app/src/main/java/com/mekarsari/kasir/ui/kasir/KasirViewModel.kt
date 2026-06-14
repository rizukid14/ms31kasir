package com.mekarsari.kasir.ui.kasir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mekarsari.kasir.data.local.entity.Product
import com.mekarsari.kasir.data.local.entity.Transaction
import com.mekarsari.kasir.data.local.entity.TransactionItem
import com.mekarsari.kasir.data.repository.ProductRepository
import com.mekarsari.kasir.data.repository.SettingRepository
import com.mekarsari.kasir.data.repository.TransactionRepository
import com.mekarsari.kasir.domain.model.CartItem
import com.mekarsari.kasir.domain.usecase.CalculateChangeUseCase
import com.mekarsari.kasir.domain.usecase.CalculateTotalUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class KasirViewModel(
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val settingRepository: SettingRepository
) : ViewModel() {

    enum class GroupSortOption {
        CATEGORY,
        ALPHABETICAL
    }

    private val _groupSortOption = MutableStateFlow(GroupSortOption.CATEGORY)
    val groupSortOption: StateFlow<GroupSortOption> = _groupSortOption.asStateFlow()

    fun setGroupSortOption(option: GroupSortOption) {
        _groupSortOption.value = option
    }

    private val _nomorMeja = MutableStateFlow("")
    val nomorMeja: StateFlow<String> = _nomorMeja.asStateFlow()

    fun setNomorMeja(nomor: String) {
        _nomorMeja.value = nomor
    }

    private val calculateTotalUseCase = CalculateTotalUseCase()
    private val calculateChangeUseCase = CalculateChangeUseCase()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Full product list from repository
    val products: StateFlow<List<Product>> = productRepository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered products list based on search query
    val filteredProducts: StateFlow<List<Product>> = combine(products, _searchQuery) { productList, query ->
        if (query.isEmpty()) {
            productList
        } else {
            productList.filter {
                it.nama.contains(query, ignoreCase = true) ||
                        (it.kategori?.contains(query, ignoreCase = true) ?: false)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val subtotal: StateFlow<Long> = _cart.map { calculateTotalUseCase(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val taxPercentage: StateFlow<Double> = settingRepository.allSettings
        .map { list ->
            list.firstOrNull { it.key == "pajak_persen" }?.value?.toDoubleOrNull() ?: 0.0
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val taxAmount: StateFlow<Long> = combine(subtotal, taxPercentage) { sub, tax ->
        (sub * (tax / 100.0)).toLong()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val total: StateFlow<Long> = combine(subtotal, taxAmount) { sub, taxAmt ->
        sub + taxAmt
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _payAmount = MutableStateFlow(0L)
    val payAmount: StateFlow<Long> = _payAmount.asStateFlow()

    val change: StateFlow<Long> = combine(total, _payAmount) { totalVal, payVal ->
        calculateChangeUseCase(totalVal, payVal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToCart(product: Product) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = currentList[index]
            currentList[index] = item.copy(quantity = item.quantity + 1)
        } else {
            currentList.add(CartItem(product, 1))
        }
        _cart.value = currentList
    }

    fun incrementQuantity(item: CartItem) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == item.product.id }
        if (index >= 0) {
            currentList[index] = item.copy(quantity = item.quantity + 1)
            _cart.value = currentList
        }
    }

    fun decrementQuantity(item: CartItem) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == item.product.id }
        if (index >= 0) {
            if (item.quantity > 1) {
                currentList[index] = item.copy(quantity = item.quantity - 1)
            } else {
                currentList.removeAt(index)
            }
            _cart.value = currentList
        }
    }

    fun setPayAmount(amount: Long) {
        _payAmount.value = amount
    }

    fun updateItemPrice(item: CartItem, newPrice: Long) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == item.product.id }
        if (index >= 0) {
            currentList[index] = item.copy(customHarga = newPrice)
            _cart.value = currentList
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _payAmount.value = 0L
    }

    suspend fun checkout(customTaxPercentage: Double? = null, customNomorMeja: String? = null): Result<Int> {
        val cartItems = _cart.value
        val subtotalVal = subtotal.value
        val taxRate = customTaxPercentage ?: taxPercentage.value
        val taxAmt = (subtotalVal * (taxRate / 100.0)).toLong()
        val totalVal = subtotalVal + taxAmt
        val payVal = _payAmount.value
        val changeVal = calculateChangeUseCase(totalVal, payVal)
        val mesa = customNomorMeja ?: _nomorMeja.value

        if (cartItems.isEmpty()) {
            return Result.failure(Exception("Keranjang belanja kosong"))
        }
        if (payVal < totalVal) {
            return Result.failure(Exception("Nominal pembayaran kurang"))
        }

        try {
            val transaction = Transaction(
                total = totalVal,
                bayar = payVal,
                kembalian = changeVal,
                metodePembayaran = "cash",
                nomorMeja = if (mesa.isBlank()) null else mesa
            )
            val items = cartItems.map { item ->
                TransactionItem(
                    transactionId = 0,
                    productId = item.product.id,
                    namaProdukSnapshot = item.product.nama,
                    hargaSaatItu = item.customHarga,
                    qty = item.quantity,
                    subtotal = item.subtotal
                )
            }

            // Save transaction in database
            val transactionId = transactionRepository.insertTransactionWithItems(transaction, items).toInt()

            // Note: We clear the cart AFTER retrieving the ID to allow subsequent receipt generation if needed.
            // But let's clear it here as it represents checkout completion.
            clearCart()
            _nomorMeja.value = ""

            return Result.success(transactionId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun getPrinterMac(): String {
        return settingRepository.getSettingValue("printer_mac") ?: ""
    }

    suspend fun getLogoUri(): String {
        return settingRepository.getSettingValue("logo_uri") ?: ""
    }

    suspend fun getStoreDetails(): Pair<String, String> {
        val name = settingRepository.getSettingValue("nama_toko") ?: "Mekar Sari"
        val address = settingRepository.getSettingValue("alamat_toko") ?: ""
        return name to address
    }

    suspend fun getReceiptSettings(): Map<String, String> {
        return mapOf(
            "receipt_header" to (settingRepository.getSettingValue("receipt_header") ?: "Selamat Datang!"),
            "receipt_footer1" to (settingRepository.getSettingValue("receipt_footer1") ?: "Terima Kasih!"),
            "receipt_footer2" to (settingRepository.getSettingValue("receipt_footer2") ?: "RM. Mekar Sari Cilacap"),
            "receipt_spacing_top" to (settingRepository.getSettingValue("receipt_spacing_top") ?: "1"),
            "receipt_spacing_bottom" to (settingRepository.getSettingValue("receipt_spacing_bottom") ?: "4")
        )
    }
}
