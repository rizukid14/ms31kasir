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

    val settingsMap: StateFlow<Map<String, String>> = settingRepository.allSettings
        .map { list -> list.associate { it.key to it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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

    fun deleteTransaction(id: Int, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            transactionRepository.deleteTransactionById(id)
            if (_selectedTransaction.value?.transaction?.id == id) {
                _selectedTransaction.value = null
            }
            onDone()
        }
    }
}
