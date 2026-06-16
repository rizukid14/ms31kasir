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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    
    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions granted/denied handled at usage time
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request Bluetooth permissions at startup on Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            val needsRequest = permissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needsRequest) {
                requestBluetoothPermissionLauncher.launch(permissions)
            }
        }
        
        val app = application as KasirApp
        val factory = AppViewModelFactory(
            productRepository = app.productRepository,
            transactionRepository = app.transactionRepository,
            settingRepository = app.settingRepository,
            database = app.database
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
