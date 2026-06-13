package com.mekarsari.kasir.domain.usecase

class CalculateChangeUseCase {
    operator fun invoke(total: Long, payAmount: Long): Long {
        val change = payAmount - total
        return if (change < 0) 0L else change
    }
}
