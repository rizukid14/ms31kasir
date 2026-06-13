package com.mekarsari.kasir.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mekarsari.kasir.data.local.dao.ProductDao
import com.mekarsari.kasir.data.local.dao.SettingDao
import com.mekarsari.kasir.data.local.dao.TransactionDao
import com.mekarsari.kasir.data.local.entity.Product
import com.mekarsari.kasir.data.local.entity.SettingEntry
import com.mekarsari.kasir.data.local.entity.Transaction
import com.mekarsari.kasir.data.local.entity.TransactionItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Product::class, Transaction::class, TransactionItem::class, SettingEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mekarsari_kasir_db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.settingDao(), database.productDao())
                }
            }
        }

        suspend fun populateDatabase(settingDao: SettingDao, productDao: ProductDao) {
            // Seed settings
            settingDao.insertSetting(SettingEntry("nama_toko", "Mekar Sari"))
            settingDao.insertSetting(SettingEntry("alamat_toko", "Jl. Gatot Subroto No. 31, Cilacap"))
            settingDao.insertSetting(SettingEntry("printer_mac", ""))

            // Seed initial products
            productDao.insertProduct(Product(nama = "Nasi Goreng", harga = 15000L, stok = 50, kategori = "Makanan"))
            productDao.insertProduct(Product(nama = "Es Teh", harga = 3000L, stok = 100, kategori = "Minuman"))
            productDao.insertProduct(Product(nama = "Ayam Goreng", harga = 18000L, stok = 30, kategori = "Makanan"))
            productDao.insertProduct(Product(nama = "Kopi Hitam", harga = 5000L, stok = 80, kategori = "Minuman"))
            productDao.insertProduct(Product(nama = "Mie Goreng", harga = 12000L, stok = 40, kategori = "Makanan"))
            productDao.insertProduct(Product(nama = "Es Jeruk", harga = 4000L, stok = 90, kategori = "Minuman"))
        }
    }
}
