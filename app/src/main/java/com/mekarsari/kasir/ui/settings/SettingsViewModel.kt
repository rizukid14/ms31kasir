package com.mekarsari.kasir.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mekarsari.kasir.data.local.AppDatabase
import com.mekarsari.kasir.data.local.DatabaseBackupManager
import com.mekarsari.kasir.data.repository.SettingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BackupRestoreStatus {
    object Idle : BackupRestoreStatus
    object Loading : BackupRestoreStatus
    data class Success(val message: String) : BackupRestoreStatus
    data class Error(val errorMsg: String) : BackupRestoreStatus
}

class SettingsViewModel(
    private val settingRepository: SettingRepository,
    private val database: AppDatabase
) : ViewModel() {

    private val _backupStatus = MutableStateFlow<BackupRestoreStatus>(BackupRestoreStatus.Idle)
    val backupStatus: StateFlow<BackupRestoreStatus> = _backupStatus.asStateFlow()

    fun resetBackupStatus() {
        _backupStatus.value = BackupRestoreStatus.Idle
    }

    fun backupDatabase(context: Context, outputUri: Uri) {
        _backupStatus.value = BackupRestoreStatus.Loading
        viewModelScope.launch {
            val result = DatabaseBackupManager.backupDatabase(context, outputUri, database)
            if (result.isSuccess) {
                _backupStatus.value = BackupRestoreStatus.Success("Backup database berhasil disimpan!")
            } else {
                _backupStatus.value = BackupRestoreStatus.Error(result.exceptionOrNull()?.message ?: "Gagal melakukan backup.")
            }
        }
    }

    fun restoreDatabase(context: Context, inputUri: Uri) {
        _backupStatus.value = BackupRestoreStatus.Loading
        viewModelScope.launch {
            val result = DatabaseBackupManager.restoreDatabase(context, inputUri, database)
            if (result.isSuccess) {
                _backupStatus.value = BackupRestoreStatus.Success("Restore database berhasil! Aplikasi akan ditutup, silakan buka kembali.")
            } else {
                _backupStatus.value = BackupRestoreStatus.Error(result.exceptionOrNull()?.message ?: "Gagal melakukan restore.")
            }
        }
    }

    private val _namaToko = MutableStateFlow("Mekar Sari")
    val namaToko: StateFlow<String> = _namaToko.asStateFlow()

    private val _alamatToko = MutableStateFlow("Jl. Gatot Subroto No. 31, Cilacap")
    val alamatToko: StateFlow<String> = _alamatToko.asStateFlow()

    private val _alamatToko2 = MutableStateFlow("HP  088 200 750 6424\nTelp. 0282 532487")
    val alamatToko2: StateFlow<String> = _alamatToko2.asStateFlow()

    private val _printerMac = MutableStateFlow("")
    val printerMac: StateFlow<String> = _printerMac.asStateFlow()

    private val _selectedTheme = MutableStateFlow("ORANGE")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    private val _pajakPersen = MutableStateFlow(0.0)
    val pajakPersen: StateFlow<Double> = _pajakPersen.asStateFlow()

    private val _logoUri = MutableStateFlow("")
    val logoUri: StateFlow<String> = _logoUri.asStateFlow()

    private val _receiptHeader = MutableStateFlow("Selamat Datang!")
    val receiptHeader: StateFlow<String> = _receiptHeader.asStateFlow()

    private val _receiptFooter1 = MutableStateFlow("Terima Kasih!")
    val receiptFooter1: StateFlow<String> = _receiptFooter1.asStateFlow()

    private val _receiptFooter2 = MutableStateFlow("RM. Mekar Sari Cilacap")
    val receiptFooter2: StateFlow<String> = _receiptFooter2.asStateFlow()

    private val _receiptSpacingTop = MutableStateFlow(1)
    val receiptSpacingTop: StateFlow<Int> = _receiptSpacingTop.asStateFlow()

    private val _receiptSpacingBottom = MutableStateFlow(4)
    val receiptSpacingBottom: StateFlow<Int> = _receiptSpacingBottom.asStateFlow()

    private val _showLogo = MutableStateFlow(true)
    val showLogo: StateFlow<Boolean> = _showLogo.asStateFlow()

    private val _imagePrintMode = MutableStateFlow("A")
    val imagePrintMode: StateFlow<String> = _imagePrintMode.asStateFlow()

    private val _logoWidthChar = MutableStateFlow(12)
    val logoWidthChar: StateFlow<Int> = _logoWidthChar.asStateFlow()

    private val _showReceiptCode = MutableStateFlow(false)
    val showReceiptCode: StateFlow<Boolean> = _showReceiptCode.asStateFlow()

    private val _showSeqNumber = MutableStateFlow(false)
    val showSeqNumber: StateFlow<Boolean> = _showSeqNumber.asStateFlow()

    private val _showUnitQty = MutableStateFlow(false)
    val showUnitQty: StateFlow<Boolean> = _showUnitQty.asStateFlow()

    private val _showNomorMeja = MutableStateFlow(true)
    val showNomorMeja: StateFlow<Boolean> = _showNomorMeja.asStateFlow()

    private val _showReceiptNumber = MutableStateFlow(true)
    val showReceiptNumber: StateFlow<Boolean> = _showReceiptNumber.asStateFlow()

    private val _showTotalQty = MutableStateFlow(false)
    val showTotalQty: StateFlow<Boolean> = _showTotalQty.asStateFlow()

    private val _showSignatureSection = MutableStateFlow(false)
    val showSignatureSection: StateFlow<Boolean> = _showSignatureSection.asStateFlow()

    private val _namaKasir = MutableStateFlow("Kasir 1")
    val namaKasir: StateFlow<String> = _namaKasir.asStateFlow()



    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingRepository.getSettingValue("nama_toko")?.let { _namaToko.value = it }
            settingRepository.getSettingValue("alamat_toko")?.let { _alamatToko.value = it }
            settingRepository.getSettingValue("alamat_toko2")?.let { _alamatToko2.value = it }
            settingRepository.getSettingValue("printer_mac")?.let { _printerMac.value = it }
            settingRepository.getSettingValue("app_theme")?.let { _selectedTheme.value = it }
            settingRepository.getSettingValue("pajak_persen")?.toDoubleOrNull()?.let { _pajakPersen.value = it }
            settingRepository.getSettingValue("logo_uri")?.let { _logoUri.value = it }
            settingRepository.getSettingValue("receipt_header")?.let { _receiptHeader.value = it }
            settingRepository.getSettingValue("receipt_footer1")?.let { _receiptFooter1.value = it }
            settingRepository.getSettingValue("receipt_footer2")?.let { _receiptFooter2.value = it }
            settingRepository.getSettingValue("receipt_spacing_top")?.toIntOrNull()?.let { _receiptSpacingTop.value = it }
            settingRepository.getSettingValue("receipt_spacing_bottom")?.toIntOrNull()?.let { _receiptSpacingBottom.value = it }
            settingRepository.getSettingValue("show_logo")?.let { _showLogo.value = it == "true" }
            settingRepository.getSettingValue("image_print_mode")?.let { _imagePrintMode.value = it }
            settingRepository.getSettingValue("logo_width_char")?.toIntOrNull()?.let { _logoWidthChar.value = it }
            settingRepository.getSettingValue("show_receipt_code")?.let { _showReceiptCode.value = it == "true" }
            settingRepository.getSettingValue("show_seq_number")?.let { _showSeqNumber.value = it == "true" }
            settingRepository.getSettingValue("show_unit_qty")?.let { _showUnitQty.value = it == "true" }
            settingRepository.getSettingValue("show_nomor_meja")?.let { _showNomorMeja.value = it == "true" }
            settingRepository.getSettingValue("show_receipt_number")?.let { _showReceiptNumber.value = it == "true" }
            settingRepository.getSettingValue("show_total_qty")?.let { _showTotalQty.value = it == "true" }
            settingRepository.getSettingValue("show_signature_section")?.let { _showSignatureSection.value = it == "true" }
            settingRepository.getSettingValue("nama_kasir")?.let { _namaKasir.value = it }

        }
    }

    fun saveNamaToko(name: String) {
        _namaToko.value = name
        viewModelScope.launch {
            settingRepository.saveSetting("nama_toko", name)
        }
    }

    fun saveAlamatToko(address: String) {
        _alamatToko.value = address
        viewModelScope.launch {
            settingRepository.saveSetting("alamat_toko", address)
        }
    }

    fun saveAlamatToko2(address2: String) {
        _alamatToko2.value = address2
        viewModelScope.launch {
            settingRepository.saveSetting("alamat_toko2", address2)
        }
    }

    fun savePrinterMac(mac: String) {
        _printerMac.value = mac
        viewModelScope.launch {
            settingRepository.saveSetting("printer_mac", mac)
        }
    }

    fun saveTheme(themeName: String) {
        _selectedTheme.value = themeName
        viewModelScope.launch {
            settingRepository.saveSetting("app_theme", themeName)
        }
    }

    fun savePajakPersen(persen: Double) {
        _pajakPersen.value = persen
        viewModelScope.launch {
            settingRepository.saveSetting("pajak_persen", persen.toString())
        }
    }

    fun saveLogoUri(uri: String) {
        _logoUri.value = uri
        viewModelScope.launch {
            settingRepository.saveSetting("logo_uri", uri)
        }
    }

    fun saveReceiptHeader(value: String) {
        _receiptHeader.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("receipt_header", value)
        }
    }

    fun saveReceiptFooter1(value: String) {
        _receiptFooter1.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("receipt_footer1", value)
        }
    }

    fun saveReceiptFooter2(value: String) {
        _receiptFooter2.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("receipt_footer2", value)
        }
    }

    fun saveReceiptSpacingTop(value: Int) {
        _receiptSpacingTop.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("receipt_spacing_top", value.toString())
        }
    }

    fun saveReceiptSpacingBottom(value: Int) {
        _receiptSpacingBottom.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("receipt_spacing_bottom", value.toString())
        }
    }

    fun saveShowLogo(value: Boolean) {
        _showLogo.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("show_logo", value.toString())
        }
    }

    fun saveImagePrintMode(value: String) {
        _imagePrintMode.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("image_print_mode", value)
        }
    }

    fun saveLogoWidthChar(value: Int) {
        _logoWidthChar.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("logo_width_char", value.toString())
        }
    }

    fun saveShowReceiptCode(value: Boolean) {
        _showReceiptCode.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("show_receipt_code", value.toString())
        }
    }

    fun saveShowSeqNumber(value: Boolean) {
        _showSeqNumber.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("show_seq_number", value.toString())
        }
    }

    fun saveShowUnitQty(value: Boolean) {
        _showUnitQty.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("show_unit_qty", value.toString())
        }
    }

    fun saveShowNomorMeja(value: Boolean) {
        _showNomorMeja.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("show_nomor_meja", value.toString())
        }
    }

    fun saveShowReceiptNumber(value: Boolean) {
        _showReceiptNumber.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("show_receipt_number", value.toString())
        }
    }

    fun saveShowTotalQty(value: Boolean) {
        _showTotalQty.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("show_total_qty", value.toString())
        }
    }

    fun saveShowSignatureSection(value: Boolean) {
        _showSignatureSection.value = value
        viewModelScope.launch {
            settingRepository.saveSetting("show_signature_section", value.toString())
        }
    }

    fun saveNamaKasir(name: String) {
        _namaKasir.value = name
        viewModelScope.launch {
            settingRepository.saveSetting("nama_kasir", name)
        }
    }
}
