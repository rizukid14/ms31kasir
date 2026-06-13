package com.mekarsari.kasir.ui.navigation

sealed class Screen(val route: String) {
    object Kasir : Screen("kasir")
    object Produk : Screen("produk")
    object ProdukForm : Screen("produk_form?productId={productId}") {
        fun createRoute(productId: Int?) = "produk_form?productId=$productId"
    }
    object Riwayat : Screen("riwayat")
    object RiwayatDetail : Screen("riwayat_detail/{transactionId}") {
        fun createRoute(transactionId: Int) = "riwayat_detail/$transactionId"
    }
    object Laporan : Screen("laporan")
    object Settings : Screen("settings")
}
