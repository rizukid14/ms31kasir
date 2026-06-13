package com.mekarsari.kasir.ui.riwayat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mekarsari.kasir.data.local.dao.TransactionWithItems
import com.mekarsari.kasir.data.repository.SettingRepository
import com.mekarsari.kasir.data.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RiwayatViewModel(
    private val transactionRepository: TransactionRepository,
    private val settingRepository: SettingRepository
) : ViewModel() {

    val allTransactions: StateFlow<List<TransactionWithItems>> = transactionRepository.allTransactionsWithItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedTransaction = MutableStateFlow<TransactionWithItems?>(null)
    val selectedTransaction: StateFlow<TransactionWithItems?> = _selectedTransaction.asStateFlow()

    fun selectTransaction(id: Int) {
        viewModelScope.launch {
            val tx = transactionRepository.getTransactionWithItemsById(id)
            _selectedTransaction.value = tx
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
}
