package com.mekarsari.kasir.domain.usecase

import com.mekarsari.kasir.domain.model.CartItem

class CalculateTotalUseCase {
    operator fun invoke(items: List<CartItem>): Long {
        return items.sumOf { it.subtotal }
    }
}
