package com.mekarsari.kasir.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtil {
    private val rupiahFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
        maximumFractionDigits = 0
    }

    fun formatRupiah(value: Long): String {
        return synchronized(rupiahFormat) {
            rupiahFormat.format(value).replace("Rp", "Rp ")
        }
    }
}
