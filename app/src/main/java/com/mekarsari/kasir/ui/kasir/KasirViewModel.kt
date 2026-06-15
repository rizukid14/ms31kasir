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
        DEFAULT,
        ALPHABETICAL
    }

    private val _groupSortOption = MutableStateFlow(GroupSortOption.DEFAULT)
    val groupSortOption: StateFlow<GroupSortOption> = _groupSortOption.asStateFlow()

    fun setGroupSortOption(option: GroupSortOption) {
        _groupSortOption.value = option
    }

    private val _nomorMeja = MutableStateFlow("")
    val nomorMeja: StateFlow<String> = _nomorMeja.asStateFlow()

    fun setNomorMeja(nomor: String) {
        _nomorMeja.value = nomor
    }

    private val _editingTransactionId = MutableStateFlow<Int?>(null)
    val editingTransactionId: StateFlow<Int?> = _editingTransactionId.asStateFlow()

    private var editingTransactionCreatedAt: Long = 0L

    fun startEditingTransaction(txWithItems: com.mekarsari.kasir.data.local.dao.TransactionWithItems) {
        _editingTransactionId.value = txWithItems.transaction.id
        editingTransactionCreatedAt = txWithItems.transaction.createdAt
        
        // Map Snapshot items to CartItems
        val newCart = txWithItems.items.map { item ->
            val matchedProduct = products.value.find { it.id == item.productId }
                ?: Product(
                    id = item.productId,
                    nama = item.namaProdukSnapshot.replace(" (1/2 Porsi)", ""),
                    harga = item.hargaSaatItu,
                    stok = 999,
                    kategori = ""
                )
            val isHalf = item.namaProdukSnapshot.contains("(1/2 Porsi)")
            CartItem(
                product = matchedProduct,
                quantity = item.qty,
                customHarga = item.hargaSaatItu,
                isHalfPortion = isHalf
            )
        }
        _cart.value = newCart
        _nomorMeja.value = txWithItems.transaction.nomorMeja ?: ""
        _payAmount.value = txWithItems.transaction.bayar
    }

    fun cancelEditing() {
        _editingTransactionId.value = null
        clearCart()
        _nomorMeja.value = ""
    }

    fun deleteEditingTransaction(onSuccess: () -> Unit) {
        val id = _editingTransactionId.value ?: return
        viewModelScope.launch {
            transactionRepository.deleteTransactionById(id)
            cancelEditing()
            onSuccess()
        }
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

    val customProductOrder: StateFlow<List<Int>> = settingRepository.allSettings
        .map { list ->
            val orderStr = list.firstOrNull { it.key == "custom_product_order" }?.value ?: ""
            if (orderStr.isEmpty()) emptyList() else orderStr.split(",").mapNotNull { it.toIntOrNull() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        val index = currentList.indexOfFirst { it.product.id == product.id && !it.isHalfPortion }
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
        val index = currentList.indexOfFirst { it.product.id == item.product.id && it.isHalfPortion == item.isHalfPortion }
        if (index >= 0) {
            currentList[index] = item.copy(quantity = item.quantity + 1)
            _cart.value = currentList
        }
    }

    fun decrementQuantity(item: CartItem) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == item.product.id && it.isHalfPortion == item.isHalfPortion }
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
        val index = currentList.indexOfFirst { it.product.id == item.product.id && it.isHalfPortion == item.isHalfPortion }
        if (index >= 0) {
            currentList[index] = item.copy(customHarga = newPrice)
            _cart.value = currentList
        }
    }

    fun toggleHalfPortion(item: CartItem) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == item.product.id && it.isHalfPortion == item.isHalfPortion }
        if (index >= 0) {
            val oldItem = currentList[index]
            val isHalf = !oldItem.isHalfPortion
            val newPrice = if (isHalf) {
                oldItem.product.harga / 2
            } else {
                oldItem.product.harga
            }
            
            val targetIndex = currentList.indexOfFirst { it.product.id == item.product.id && it.isHalfPortion == isHalf }
            if (targetIndex >= 0 && targetIndex != index) {
                val targetItem = currentList[targetIndex]
                currentList[targetIndex] = targetItem.copy(quantity = targetItem.quantity + oldItem.quantity)
                currentList.removeAt(index)
            } else {
                currentList[index] = oldItem.copy(
                    isHalfPortion = isHalf,
                    customHarga = newPrice
                )
            }
            _cart.value = currentList
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _payAmount.value = 0L
    }

    suspend fun checkout(customTaxPercentage: Double? = null, customNomorMeja: String? = null): Result<Int> {
        val cartItems = _cart.value
        if (cartItems.isEmpty()) {
            return Result.failure(Exception("Keranjang belanja kosong"))
        }
        val subtotalVal = cartItems.sumOf { it.subtotal }
        val taxRate = customTaxPercentage ?: taxPercentage.value
        val taxAmt = (subtotalVal * (taxRate / 100.0)).toLong()
        val totalVal = subtotalVal + taxAmt
        val payVal = _payAmount.value
        val changeVal = calculateChangeUseCase(totalVal, payVal)
        val mesa = customNomorMeja ?: _nomorMeja.value

        if (payVal < totalVal) {
            return Result.failure(Exception("Nominal pembayaran kurang"))
        }

        try {
            val editId = _editingTransactionId.value
            // Read nama kasir at checkout time — this becomes the immutable snapshot
            val namaKasirSnapshot = settingRepository.getSettingValue("nama_kasir")?.takeIf { it.isNotBlank() }

            val items = cartItems.map { item ->
                val nameSnapshot = if (item.isHalfPortion) {
                    "${item.product.nama} (1/2 Porsi)"
                } else {
                    item.product.nama
                }
                TransactionItem(
                    transactionId = editId ?: 0,
                    productId = item.product.id,
                    namaProdukSnapshot = nameSnapshot,
                    hargaSaatItu = item.customHarga,
                    qty = item.quantity,
                    subtotal = item.subtotal
                )
            }

            val transactionId = if (editId != null) {
                val transaction = Transaction(
                    id = editId,
                    total = totalVal,
                    bayar = payVal,
                    kembalian = changeVal,
                    metodePembayaran = "cash",
                    nomorMeja = if (mesa.isBlank()) null else mesa,
                    namaKasir = namaKasirSnapshot,
                    createdAt = editingTransactionCreatedAt
                )
                transactionRepository.updateTransactionWithItems(transaction, items)
                editId
            } else {
                val transaction = Transaction(
                    total = totalVal,
                    bayar = payVal,
                    kembalian = changeVal,
                    metodePembayaran = "cash",
                    nomorMeja = if (mesa.isBlank()) null else mesa,
                    namaKasir = namaKasirSnapshot
                )
                transactionRepository.insertTransactionWithItems(transaction, items).toInt()
            }

            // Note: We clear the cart AFTER retrieving the ID to allow subsequent receipt generation if needed.
            // But let's clear it here as it represents checkout completion.
            clearCart()
            _nomorMeja.value = ""
            _editingTransactionId.value = null

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

    suspend fun getStoreDetails(): Triple<String, String, String> {
        val name = settingRepository.getSettingValue("nama_toko") ?: "Mekar Sari"
        val address = settingRepository.getSettingValue("alamat_toko") ?: ""
        val address2 = settingRepository.getSettingValue("alamat_toko2") ?: ""
        return Triple(name, address, address2)
    }

    suspend fun getReceiptSettings(): Map<String, String> {
        return mapOf(
            "receipt_header" to (settingRepository.getSettingValue("receipt_header") ?: "Selamat Datang!"),
            "receipt_footer1" to (settingRepository.getSettingValue("receipt_footer1") ?: "Terima Kasih!"),
            "receipt_footer2" to (settingRepository.getSettingValue("receipt_footer2") ?: "RM. Mekar Sari Cilacap"),
            "receipt_spacing_top" to (settingRepository.getSettingValue("receipt_spacing_top") ?: "1"),
            "receipt_spacing_bottom" to (settingRepository.getSettingValue("receipt_spacing_bottom") ?: "4"),
            "show_logo" to (settingRepository.getSettingValue("show_logo") ?: "true"),
            "logo_width_char" to (settingRepository.getSettingValue("logo_width_char") ?: "12"),
            "show_receipt_code" to (settingRepository.getSettingValue("show_receipt_code") ?: "false"),
            "show_seq_number" to (settingRepository.getSettingValue("show_seq_number") ?: "false"),
            "show_unit_qty" to (settingRepository.getSettingValue("show_unit_qty") ?: "false"),
            "show_nomor_meja" to (settingRepository.getSettingValue("show_nomor_meja") ?: "true"),
            "show_receipt_number" to (settingRepository.getSettingValue("show_receipt_number") ?: "true"),
            "show_total_qty" to (settingRepository.getSettingValue("show_total_qty") ?: "false"),
            "show_signature_section" to (settingRepository.getSettingValue("show_signature_section") ?: "false"),
            "nama_kasir" to (settingRepository.getSettingValue("nama_kasir") ?: "Kasir 1"),
            "alamat_toko2" to (settingRepository.getSettingValue("alamat_toko2") ?: "")
        )
    }
}
