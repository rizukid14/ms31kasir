package com.mekarsari.kasir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.mekarsari.kasir.ui.AppViewModelFactory
import com.mekarsari.kasir.ui.navigation.AppNavHost
import com.mekarsari.kasir.ui.settings.SettingsViewModel
import com.mekarsari.kasir.ui.theme.AppTheme
import com.mekarsari.kasir.ui.theme.MekarSariKasirTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as KasirApp
        val factory = AppViewModelFactory(
            productRepository = app.productRepository,
            transactionRepository = app.transactionRepository,
            settingRepository = app.settingRepository
        )

        // Instantiate settingsViewModel globally to read/write persistent theme config
        val settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        setContent {
            val themeStr by settingsViewModel.selectedTheme.collectAsState()
            val currentTheme = remember(themeStr) {
                try {
                    AppTheme.valueOf(themeStr)
                } catch (e: Exception) {
                    AppTheme.ORANGE
                }
            }

            MekarSariKasirTheme(theme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(factory = factory, settingsViewModel = settingsViewModel)
                }
            }
        }
    }
}
