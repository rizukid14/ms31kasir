package com.mekarsari.kasir.printer

import com.mekarsari.kasir.data.local.dao.TransactionWithItems
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReceiptFormatter {

    fun format(
        shopName: String,
        shopAddress: String,
        shopAddress2: String = "",
        transactionWithItems: TransactionWithItems,
        customHeader: String = "Selamat Datang!",
        customFooter1: String = "Terima Kasih!",
        customFooter2: String = "RM. Mekar Sari Cilacap",
        spacingTop: Int = 1,
        spacingBottom: Int = 4,
        showLogo: Boolean = true,
        showReceiptCode: Boolean = false,
        showSeqNumber: Boolean = false,
        showUnitQty: Boolean = false,
        showNomorMeja: Boolean = true,
        showReceiptNumber: Boolean = true,
        showTotalQty: Boolean = false,
        showSignatureSection: Boolean = false,
        namaKasir: String = ""
    ): String {
        val transaction = transactionWithItems.transaction
        val items = transactionWithItems.items

        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateOnlyString = sdfDate.format(Date(transaction.createdAt))
        val timeOnlyString = sdfTime.format(Date(transaction.createdAt))

        val sb = StringBuilder()
        for (i in 0 until spacingTop) {
            sb.append("\n")
        }
        if (showLogo) {
            sb.append("[LOGO]\n")
        }
        if (customHeader.isNotEmpty()) {
            customHeader.split("\n").forEach { line ->
                sb.append("[C]$line\n")
            }
        }
        if (shopName.isNotEmpty()) {
            shopName.split("\n").forEach { line ->
                sb.append("[C]<b>$line</b>\n")
            }
        }
        if (shopAddress.isNotEmpty()) {
            shopAddress.split("\n").forEach { line ->
                sb.append("[C]$line\n")
            }
        }
        if (shopAddress2.isNotEmpty()) {
            shopAddress2.split("\n").forEach { line ->
                sb.append("[C]$line\n")
            }
        }
        sb.append("[C]--------------------------------\n")
        if (showSeqNumber) {
            sb.append("[C]<b>NO. URUT: ${String.format("%03d", (transaction.id % 1000).coerceAtLeast(1))}</b>\n")
            sb.append("[C]--------------------------------\n")
        }
        if (showReceiptNumber) {
            sb.append("[L]$dateOnlyString[R]No: TX#${transaction.id}\n")
        } else {
            sb.append("[L]$dateOnlyString\n")
        }
        if (namaKasir.isNotEmpty()) {
            sb.append("[L]$timeOnlyString[R]Kasir: $namaKasir\n")
        } else {
            sb.append("[L]$timeOnlyString\n")
        }
        if (showNomorMeja && !transaction.nomorMeja.isNullOrEmpty()) {
            sb.append("[L]Meja: ${transaction.nomorMeja}\n")
        }
        sb.append("[C]--------------------------------\n")

        for (item in items) {
            val portionRegex = """\(([0-9.]+)\s*Porsi\)""".toRegex()
            val match = portionRegex.find(item.namaProdukSnapshot)
            val customPortion = item.porsiCustom ?: match?.groupValues?.get(1)?.toDoubleOrNull()
            
            val cleanedName = if (item.porsiCustom != null) {
                item.namaProdukSnapshot
            } else if (customPortion != null) {
                val fallbackPortionStr = match?.groupValues?.get(1)
                item.namaProdukSnapshot.replace(" ($fallbackPortionStr Porsi)", "")
            } else {
                item.namaProdukSnapshot
            }
            
            sb.append("[L]$cleanedName\n")
            
            val visualQty = if (customPortion != null) {
                customPortion * item.qty
            } else {
                item.qty.toDouble()
            }
            val visualQtyStr = if (visualQty % 1.0 == 0.0) visualQty.toInt().toString() else visualQty.toString()
            
            val qtyStr = if (showUnitQty) {
                val unit = if (cleanedName.contains("es ", ignoreCase = true) ||
                              cleanedName.contains("teh", ignoreCase = true) ||
                              cleanedName.contains("jeruk", ignoreCase = true) ||
                              cleanedName.contains("kopi", ignoreCase = true) ||
                              cleanedName.contains("milo", ignoreCase = true) ||
                              cleanedName.contains("susu", ignoreCase = true) ||
                              cleanedName.contains("soda", ignoreCase = true) ||
                              cleanedName.contains("cola", ignoreCase = true) ||
                              cleanedName.contains("fanta", ignoreCase = true) ||
                              cleanedName.contains("sprite", ignoreCase = true) ||
                              cleanedName.contains("frestea", ignoreCase = true) ||
                              cleanedName.contains("air ", ignoreCase = true)) {
                    "Gelas"
                } else {
                    "Porsi"
                }
                "$visualQtyStr $unit"
            } else {
                visualQtyStr
            }
            val originalUnitPrice = if (customPortion != null && customPortion > 0.0) {
                Math.round(item.hargaSaatItu.toDouble() / customPortion)
            } else {
                item.hargaSaatItu
            }
            sb.append("[L]  $qtyStr x ${formatRupiah(originalUnitPrice)}[R]${formatRupiah(item.subtotal)}\n")
        }

        val subtotal = items.sumOf { it.subtotal }
        val taxAmount = transaction.total - subtotal

        sb.append("[C]--------------------------------\n")
        if (showTotalQty) {
            val totalQty = items.sumOf { it.qty }
            sb.append("[L]Total Kuantitas[R]$totalQty\n")
            sb.append("[C]--------------------------------\n")
        }
        if (taxAmount > 0) {
            val taxPercent = Math.round((taxAmount.toDouble() / subtotal.toDouble() * 100.0) * 10.0) / 10.0
            sb.append("[L]Subtotal[R]${formatRupiah(subtotal)}\n")
            sb.append("[L]Pajak ($taxPercent%)[R]${formatRupiah(taxAmount)}\n")
        }
        sb.append("[L]<b>Total</b>[R]<b>${formatRupiah(transaction.total)}</b>\n")
        sb.append("[L]Bayar[R]${formatRupiah(transaction.bayar)}\n")
        sb.append("[L]Kembali[R]${formatRupiah(transaction.kembalian)}\n")
        sb.append("[C]--------------------------------\n")
        if (customFooter1.isNotEmpty()) {
            customFooter1.split("\n").forEach { line ->
                sb.append("[C]$line\n")
            }
        }
        if (customFooter2.isNotEmpty()) {
            customFooter2.split("\n").forEach { line ->
                sb.append("[C]$line\n")
            }
        }
        if (showSignatureSection) {
            sb.append("\n")
            sb.append("[C]Tanda Tangan\n")
            sb.append("\n\n")
            sb.append("[C](....................)\n")
        }
        if (showReceiptCode) {
            sb.append("\n")
            sb.append("[C]<qrcode size=\"30\">https://maps.app.goo.gl/9vTmPHFS1rZZ24MbA</qrcode>\n")
        }
        for (i in 0 until spacingBottom) {
            sb.append("\n")
        }

        return sb.toString()
    }

    fun formatDailyClosing(
        shopName: String,
        shopAddress: String,
        shopAddress2: String = "",
        dateStr: String,
        revenue: Long,
        transactionCount: Int,
        averageTicket: Long,
        topProducts: List<com.mekarsari.kasir.ui.laporan.TopProductReport>,
        namaKasir: String
    ): String {
        val sb = StringBuilder()
        sb.append("[C]<b>LAPORAN PENUTUPAN</b>\n")
        sb.append("[C]<b>$shopName</b>\n")
        if (shopAddress.isNotEmpty()) {
            sb.append("[C]$shopAddress\n")
        }
        if (shopAddress2.isNotEmpty()) {
            sb.append("[C]$shopAddress2\n")
        }
        sb.append("[C]--------------------------------\n")
        sb.append("[L]Tanggal[R]$dateStr\n")
        sb.append("[L]Kasir[R]$namaKasir\n")
        sb.append("[C]--------------------------------\n")
        sb.append("[L]Total Omset[R]${formatRupiah(revenue)}\n")
        sb.append("[L]Total Transaksi[R]$transactionCount\n")
        sb.append("[L]Rata-rata/Struk[R]${formatRupiah(averageTicket)}\n")
        sb.append("[C]--------------------------------\n")
        sb.append("[C]<b>5 PRODUK TERLARIS</b>\n")
        topProducts.take(5).forEach { prod ->
            val qtyStr = if (prod.qty % 1.0 == 0.0) prod.qty.toInt().toString() else prod.qty.toString()
            sb.append("[L]${prod.name}[R]$qtyStr ${prod.unit}\n")
        }
        sb.append("[C]--------------------------------\n")
        sb.append("[C]Mekar Sari Kasir\n")
        sb.append("\n\n\n\n")
        return sb.toString()
    }

    fun formatRupiah(value: Long): String {
        return com.mekarsari.kasir.util.CurrencyUtil.formatRupiah(value)
    }
}
