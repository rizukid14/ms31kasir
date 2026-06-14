package com.mekarsari.kasir

import android.app.Application
import com.mekarsari.kasir.data.local.AppDatabase
import com.mekarsari.kasir.data.repository.ProductRepository
import com.mekarsari.kasir.data.repository.SettingRepository
import com.mekarsari.kasir.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class KasirApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    
    val productRepository by lazy { ProductRepository(database.productDao()) }
    val transactionRepository by lazy { TransactionRepository(database.transactionDao()) }
    val settingRepository by lazy { SettingRepository(database.settingDao()) }
}
