package com.mekarsari.kasir.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val total: Long,
    val bayar: Long,
    val kembalian: Long,
    @ColumnInfo(name = "metode_pembayaran") val metodePembayaran: String = "cash",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
