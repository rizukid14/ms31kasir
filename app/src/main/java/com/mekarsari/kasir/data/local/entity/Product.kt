package com.mekarsari.kasir.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val harga: Long,
    val stok: Int,
    val kategori: String? = null
)
