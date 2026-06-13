package com.mekarsari.kasir.printer

import com.mekarsari.kasir.data.local.dao.TransactionWithItems
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReceiptFormatter {

    fun format(
        shopName: String,
        shopAddress: String,
        transactionWithItems: TransactionWithItems
    ): String {
        val transaction = transactionWithItems.transaction
        val items = transactionWithItems.items

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateString = sdf.format(Date(transaction.createdAt))

        val sb = StringBuilder()
        sb.append("[LOGO]\n")
        sb.append("[C]<b>$shopName</b>\n")
        if (shopAddress.isNotEmpty()) {
            sb.append("[C]$shopAddress\n")
        }
        sb.append("[C]--------------------------------\n")
        sb.append("[L]Tgl: $dateString\n")
        sb.append("[L]No: TX#${transaction.id}\n")
        sb.append("[C]--------------------------------\n")

        for (item in items) {
            val name = item.namaProdukSnapshot
            sb.append("[L]$name\n")
            sb.append("[L]  ${item.qty} x ${formatRupiah(item.hargaSaatItu)}[R]${formatRupiah(item.subtotal)}\n")
        }

        val subtotal = items.sumOf { it.subtotal }
        val taxAmount = transaction.total - subtotal

        sb.append("[C]--------------------------------\n")
        if (taxAmount > 0) {
            val taxPercent = Math.round((taxAmount.toDouble() / subtotal.toDouble() * 100.0) * 10.0) / 10.0
            sb.append("[L]Subtotal[R]${formatRupiah(subtotal)}\n")
            sb.append("[L]Pajak ($taxPercent%)[R]${formatRupiah(taxAmount)}\n")
        }
        sb.append("[L]<b>Total</b>[R]<b>${formatRupiah(transaction.total)}</b>\n")
        sb.append("[L]Bayar[R]${formatRupiah(transaction.bayar)}\n")
        sb.append("[L]Kembali[R]${formatRupiah(transaction.kembalian)}\n")
        sb.append("[C]--------------------------------\n")
        sb.append("[C]Terima Kasih!\n")
        sb.append("[C]Mekar Sari Cilacap\n\n\n\n") // Padding for paper tear-off

        return sb.toString()
    }

    fun formatRupiah(value: Long): String {
        return String.format(Locale("in", "ID"), "%,d", value).replace(',', '.')
    }
}
