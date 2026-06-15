package com.mekarsari.kasir.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mekarsari.kasir.ui.AppViewModelFactory
import com.mekarsari.kasir.ui.kasir.KasirScreen
import com.mekarsari.kasir.ui.produk.ProdukFormScreen
import com.mekarsari.kasir.ui.produk.ProdukScreen
import com.mekarsari.kasir.ui.riwayat.RiwayatDetailScreen
import com.mekarsari.kasir.ui.riwayat.RiwayatScreen
import com.mekarsari.kasir.ui.settings.SettingsScreen
import com.mekarsari.kasir.ui.settings.SettingsViewModel
import com.mekarsari.kasir.ui.laporan.LaporanScreen

@Composable
fun AppNavHost(
    factory: AppViewModelFactory,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val items = listOf(
        Triple(Screen.Kasir.route, "Kasir", Icons.Default.ShoppingCart),
        Triple(Screen.Produk.route, "Produk", Icons.Default.List),
        Triple(Screen.Riwayat.route, "Riwayat", Icons.Default.Refresh),
        Triple(Screen.Laporan.route, "Laporan", Icons.Default.DateRange),
        Triple(Screen.Settings.route, "Settings", Icons.Default.Settings)
    )

    val kasirViewModel: com.mekarsari.kasir.ui.kasir.KasirViewModel = viewModel(factory = factory)
    val riwayatViewModel: com.mekarsari.kasir.ui.riwayat.RiwayatViewModel = viewModel(factory = factory)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val currentRoute = currentDestination?.route ?: ""
            val showBottomBar = items.any { it.first == currentRoute }

            if (showBottomBar) {
                NavigationBar {
                    items.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Kasir.route,
            modifier = modifier.padding(paddingValues)
        ) {
            composable(Screen.Kasir.route) {
                KasirScreen(viewModel = kasirViewModel)
            }
            
            composable(Screen.Produk.route) {
                ProdukScreen(
                    viewModel = viewModel(factory = factory),
                    onNavigateToForm = { id ->
                        navController.navigate(Screen.ProdukForm.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.ProdukForm.route,
                arguments = listOf(navArgument("productId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val idStr = backStackEntry.arguments?.getString("productId")
                val id = idStr?.toIntOrNull()
                ProdukFormScreen(
                    productId = id,
                    viewModel = viewModel(factory = factory),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Riwayat.route) {
                RiwayatScreen(
                    viewModel = riwayatViewModel,
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.RiwayatDetail.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.RiwayatDetail.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.IntType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("transactionId") ?: 0
                RiwayatDetailScreen(
                    transactionId = id,
                    viewModel = riwayatViewModel,
                    kasirViewModel = kasirViewModel,
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Laporan.route) {
                LaporanScreen(viewModel = viewModel(factory = factory))
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
