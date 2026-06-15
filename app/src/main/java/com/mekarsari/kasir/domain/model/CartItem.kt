package com.mekarsari.kasir.domain.model

import com.mekarsari.kasir.data.local.entity.Product

data class CartItem(
    val product: Product,
    val quantity: Int,
    val customHarga: Long = product.harga,
    val customPortion: Double? = null
) {
    val subtotal: Long
        get() = customHarga * quantity
}
