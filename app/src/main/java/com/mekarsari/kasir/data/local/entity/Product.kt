package com.mekarsari.kasir.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "nama") val nama: String,
    @ColumnInfo(name = "harga") val harga: Long,
    @ColumnInfo(name = "stok") val stok: Int,
    @ColumnInfo(name = "kategori") val kategori: String? = null
)
