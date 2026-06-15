package com.mekarsari.kasir.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mekarsari.kasir.data.repository.ProductRepository
import com.mekarsari.kasir.data.repository.SettingRepository
import com.mekarsari.kasir.data.repository.TransactionRepository
import com.mekarsari.kasir.ui.kasir.KasirViewModel
import com.mekarsari.kasir.ui.produk.ProdukViewModel
import com.mekarsari.kasir.ui.riwayat.RiwayatViewModel
import com.mekarsari.kasir.ui.settings.SettingsViewModel
import com.mekarsari.kasir.ui.laporan.LaporanViewModel

class AppViewModelFactory(
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val settingRepository: SettingRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(KasirViewModel::class.java) -> {
                KasirViewModel(productRepository, transactionRepository, settingRepository) as T
            }
            modelClass.isAssignableFrom(ProdukViewModel::class.java) -> {
                ProdukViewModel(productRepository, settingRepository) as T
            }
            modelClass.isAssignableFrom(RiwayatViewModel::class.java) -> {
                RiwayatViewModel(transactionRepository, settingRepository) as T
            }
            modelClass.isAssignableFrom(LaporanViewModel::class.java) -> {
                LaporanViewModel(transactionRepository, productRepository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(settingRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
