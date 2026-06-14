package com.mekarsari.kasir.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transaction_id"])]
)
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "transaction_id") val transactionId: Int,
    @ColumnInfo(name = "product_id") val productId: Int,
    @ColumnInfo(name = "nama_produk_snapshot") val namaProdukSnapshot: String,
    @ColumnInfo(name = "harga_saat_itu") val hargaSaatItu: Long,
    val qty: Int,
    val subtotal: Long
)
