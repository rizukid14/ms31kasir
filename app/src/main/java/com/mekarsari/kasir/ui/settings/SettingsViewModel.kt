package com.mekarsari.kasir.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mekarsari.kasir.data.repository.SettingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingRepository: SettingRepository
) : ViewModel() {

    private val _namaToko = MutableStateFlow("Mekar Sari")
    val namaToko: StateFlow<String> = _namaToko.asStateFlow()

    private val _alamatToko = MutableStateFlow("Jl. Gatot Subroto No. 31, Cilacap")
    val alamatToko: StateFlow<String> = _alamatToko.asStateFlow()

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

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingRepository.getSettingValue("nama_toko")?.let { _namaToko.value = it }
            settingRepository.getSettingValue("alamat_toko")?.let { _alamatToko.value = it }
            settingRepository.getSettingValue("printer_mac")?.let { _printerMac.value = it }
            settingRepository.getSettingValue("app_theme")?.let { _selectedTheme.value = it }
            settingRepository.getSettingValue("pajak_persen")?.toDoubleOrNull()?.let { _pajakPersen.value = it }
            settingRepository.getSettingValue("logo_uri")?.let { _logoUri.value = it }
            settingRepository.getSettingValue("receipt_header")?.let { _receiptHeader.value = it }
            settingRepository.getSettingValue("receipt_footer1")?.let { _receiptFooter1.value = it }
            settingRepository.getSettingValue("receipt_footer2")?.let { _receiptFooter2.value = it }
            settingRepository.getSettingValue("receipt_spacing_top")?.toIntOrNull()?.let { _receiptSpacingTop.value = it }
            settingRepository.getSettingValue("receipt_spacing_bottom")?.toIntOrNull()?.let { _receiptSpacingBottom.value = it }
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
}
