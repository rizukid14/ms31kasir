package com.mekarsari.kasir

import android.app.Application
import com.mekarsari.kasir.data.local.AppDatabase
import com.mekarsari.kasir.data.repository.ProductRepository
import com.mekarsari.kasir.data.repository.SettingRepository
import com.mekarsari.kasir.data.repository.TransactionRepository

class KasirApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    
    val productRepository by lazy { ProductRepository(database.productDao()) }
    val transactionRepository by lazy { TransactionRepository(database.transactionDao()) }
    val settingRepository by lazy { SettingRepository(database.settingDao()) }
}
