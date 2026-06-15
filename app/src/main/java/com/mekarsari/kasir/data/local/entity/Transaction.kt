package com.mekarsari.kasir.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "total") val total: Long,
    @ColumnInfo(name = "bayar") val bayar: Long,
    @ColumnInfo(name = "kembalian") val kembalian: Long,
    @ColumnInfo(name = "metode_pembayaran") val metodePembayaran: String = "cash",
    @ColumnInfo(name = "nomor_meja") val nomorMeja: String? = null,
    @ColumnInfo(name = "nama_kasir") val namaKasir: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
