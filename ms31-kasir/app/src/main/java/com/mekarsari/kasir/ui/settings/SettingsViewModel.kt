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
}
