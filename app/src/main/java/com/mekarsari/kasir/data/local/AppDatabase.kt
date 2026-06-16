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
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

@Database(
    entities = [Product::class, Transaction::class, TransactionItem::class, SettingEntry::class],
    version = 11,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mekarsari_kasir_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback : RoomDatabase.Callback() {

        // onOpen dipanggil setiap kali app dibuka.
        // Seed data hanya diinsert jika tabel masih kosong,
        // sehingga aman untuk update tanpa uninstall.
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            seedIfEmpty(db)
        }

        // onCreate tetap ada sebagai fallback saat DB pertama kali dibuat
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            seedIfEmpty(db)
        }

        private fun seedIfEmpty(db: SupportSQLiteDatabase) {
            db.beginTransaction()
            try {
                // Seed settings (pakai CONFLICT_IGNORE agar tidak timpa setting user)
                insertSettingIfNotExists(db, "nama_toko", "RM. Mekar Sari")
                insertSettingIfNotExists(db, "alamat_toko", "Jl. Gatot Subroto No. 31, Cilacap")
                insertSettingIfNotExists(db, "alamat_toko2", "HP  088 200 750 6424\nTelp. 0282 532487")
                insertSettingIfNotExists(db, "printer_mac", "")
                insertSettingIfNotExists(db, "show_logo", "true")
                insertSettingIfNotExists(db, "image_print_mode", "A")
                insertSettingIfNotExists(db, "logo_width_char", "12")
                insertSettingIfNotExists(db, "show_receipt_code", "false")
                insertSettingIfNotExists(db, "show_seq_number", "false")
                insertSettingIfNotExists(db, "show_unit_qty", "false")
                insertSettingIfNotExists(db, "show_nomor_meja", "true")
                insertSettingIfNotExists(db, "show_receipt_number", "true")
                insertSettingIfNotExists(db, "show_total_qty", "false")
                insertSettingIfNotExists(db, "show_signature_section", "false")

                // Seed produk hanya jika tabel produk masih kosong
                val cursor = db.query("SELECT COUNT(*) FROM products")
                val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
                cursor.close()

                if (count == 0) {
                    // Makanan - Menu Kambing
                    insertProduct(db, "Sate Kambing", 55000L, 50, "Makanan")
                    insertProduct(db, "Gule Kambing", 30000L, 50, "Makanan")
                    insertProduct(db, "Sop Kambing", 35000L, 50, "Makanan")
                    insertProduct(db, "Tongseng Kambing", 55000L, 50, "Makanan")

                    // Makanan - Menu Ayam
                    insertProduct(db, "Sate Ayam", 20000L, 50, "Makanan")
                    insertProduct(db, "Sop Ayam", 30000L, 50, "Makanan")
                    insertProduct(db, "Ayam Goreng", 23000L, 50, "Makanan")
                    insertProduct(db, "Ayam Bakar Manis", 23000L, 50, "Makanan")
                    insertProduct(db, "Ayam Bakar Pedas", 23000L, 50, "Makanan")
                    insertProduct(db, "Pepes Ayam", 23000L, 50, "Makanan")
                    insertProduct(db, "Pepes Jeroan", 23000L, 50, "Makanan")
                    insertProduct(db, "Rempela Ati Goreng", 22000L, 50, "Makanan")
                    insertProduct(db, "Kepala Ayam Goreng", 10000L, 50, "Makanan")

                    // Makanan - Tambahan
                    insertProduct(db, "Pepes Ikan Laut", 20000L, 50, "Makanan")
                    insertProduct(db, "Pepes Tahu", 5000L, 50, "Makanan")
                    insertProduct(db, "Tempe Tahu Goreng", 5000L, 50, "Makanan")
                    insertProduct(db, "Bandeng Presto", 12000L, 50, "Makanan")
                    insertProduct(db, "Telor Dadar / Mata Sapi", 10000L, 50, "Makanan")
                    insertProduct(db, "Sayur Asem", 7000L, 50, "Makanan")
                    insertProduct(db, "Ca Kangkung", 15000L, 50, "Makanan")
                    insertProduct(db, "Ca Tauge", 15000L, 50, "Makanan")
                    insertProduct(db, "Capcay Goreng", 20000L, 50, "Makanan")
                    insertProduct(db, "Capcay Kuah", 20000L, 50, "Makanan")
                    insertProduct(db, "Nasi Putih", 7000L, 50, "Makanan")
                    insertProduct(db, "Sambel Uleg", 5000L, 50, "Makanan")

                    // Lain-lain
                    insertProduct(db, "Sop Iga Sapi", 45000L, 50, "Makanan")
                    insertProduct(db, "Sop Iga Bakar", 45000L, 50, "Makanan")
                    insertProduct(db, "Tongseng Sapi", 55000L, 50, "Makanan")

                    // Paket
                    insertProduct(db, "Paket Nasi Timbel", 39000L, 50, "Paket")

                    // Minuman
                    insertProduct(db, "Es Teh Manis", 5000L, 100, "Minuman")
                    insertProduct(db, "Es Teh Tawar", 3000L, 100, "Minuman")
                    insertProduct(db, "Teh Manis Panas", 5000L, 100, "Minuman")
                    insertProduct(db, "Teh Tawar Panas", 2000L, 100, "Minuman")
                    insertProduct(db, "Es Jeruk", 8000L, 100, "Minuman")
                    insertProduct(db, "Jeruk Panas", 8000L, 100, "Minuman")
                    insertProduct(db, "Es Soda Susu", 10000L, 100, "Minuman")
                    insertProduct(db, "Es Soda Gembira", 10000L, 100, "Minuman")
                    insertProduct(db, "Coca Cola", 8000L, 100, "Minuman")
                    insertProduct(db, "Fanta", 8000L, 100, "Minuman")
                    insertProduct(db, "Sprite", 8000L, 100, "Minuman")
                    insertProduct(db, "Frestea", 8000L, 100, "Minuman")
                    insertProduct(db, "Air Mineral Kecil", 5000L, 100, "Minuman")
                    insertProduct(db, "Air Mineral Besar", 5000L, 100, "Minuman")
                    insertProduct(db, "Kopi", 5000L, 100, "Minuman")
                    insertProduct(db, "Susu", 5000L, 100, "Minuman")
                    insertProduct(db, "Milo", 8000L, 100, "Minuman")
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        // Pakai CONFLICT_IGNORE agar setting yang sudah diubah user tidak tertimpa
        private fun insertSettingIfNotExists(db: SupportSQLiteDatabase, key: String, value: String) {
            val values = ContentValues().apply {
                put("key", key)
                put("value", value)
            }
            db.insert("settings", SQLiteDatabase.CONFLICT_IGNORE, values)
        }

        private fun insertProduct(db: SupportSQLiteDatabase, nama: String, harga: Long, stok: Int, kategori: String) {
            val values = ContentValues().apply {
                put("nama", nama)
                put("harga", harga)
                put("stok", stok)
                put("kategori", kategori)
            }
            db.insert("products", SQLiteDatabase.CONFLICT_REPLACE, values)
        }
    }
}
