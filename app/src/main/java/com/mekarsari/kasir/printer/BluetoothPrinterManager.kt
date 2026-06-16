package com.mekarsari.kasir.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import android.graphics.Bitmap

class BluetoothPrinterManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        @Suppress("DEPRECATION")
        BluetoothAdapter.getDefaultAdapter()
    }

    // List of paired devices (MAC to Name) - filtered to show only printers
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<Pair<String, String>> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return try {
            adapter.bondedDevices
                .filter { device ->
                    val name = device.name ?: ""
                    val bluetoothClass = device.bluetoothClass
                    
                    val isPrinterByName = name.contains("print", ignoreCase = true) ||
                            name.contains("thermal", ignoreCase = true) ||
                            name.contains("pos", ignoreCase = true) ||
                            name.contains("mpt", ignoreCase = true) ||
                            name.contains("rt-", ignoreCase = true) ||
                            name.contains("rpp", ignoreCase = true) ||
                            name.contains("58mm", ignoreCase = true) ||
                            name.contains("80mm", ignoreCase = true)
                            
                    val isPrinter = if (bluetoothClass != null) {
                        val major = bluetoothClass.majorDeviceClass
                        val isExcluded = major == android.bluetooth.BluetoothClass.Device.Major.PHONE ||
                                major == android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO ||
                                major == android.bluetooth.BluetoothClass.Device.Major.COMPUTER ||
                                major == android.bluetooth.BluetoothClass.Device.Major.WEARABLE ||
                                major == android.bluetooth.BluetoothClass.Device.Major.TOY ||
                                major == android.bluetooth.BluetoothClass.Device.Major.HEALTH ||
                                major == android.bluetooth.BluetoothClass.Device.Major.NETWORKING
                                
                        if (isExcluded) {
                            isPrinterByName
                        } else {
                            bluetoothClass.deviceClass == 1664 || // 1664 = IMAGING_PRINTER
                            major == android.bluetooth.BluetoothClass.Device.Major.IMAGING ||
                            bluetoothClass.hasService(android.bluetooth.BluetoothClass.Service.RENDER) ||
                            isPrinterByName
                        }
                    } else {
                        isPrinterByName
                    }
                    
                    isPrinter
                }
                .map { it.address to (it.name ?: "Device Tidak Dikenal") }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    suspend fun printReceipt(
        macAddress: String,
        receiptContent: String,
        logoBitmap: Bitmap? = null,
        logoWidthChar: Int = 12
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (macAddress.isEmpty()) {
            return@withContext Result.failure(Exception("MAC Address printer belum diatur"))
        }

        val adapter = bluetoothAdapter ?: return@withContext Result.failure(Exception("Bluetooth tidak didukung di perangkat ini"))
        if (!adapter.isEnabled) {
            return@withContext Result.failure(Exception("Bluetooth dinonaktifkan"))
        }

        var connection: InsecureBluetoothConnection? = null
        var printer: EscPosPrinter? = null
        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
            connection = InsecureBluetoothConnection(device)
            
            // 203 DPI, 48mm width (approx 384 dots), 32 characters per line (typical 58mm printer)
            printer = EscPosPrinter(connection, 203, 48f, 32)
            
            val contentToPrint = if (logoBitmap != null && receiptContent.contains("[LOGO]")) {
                try {
                    // Resize based on the character width setting (approx 12 dots per character)
                    val pixelSize = (logoWidthChar * 12).coerceIn(48, 384)
                    val resized = Bitmap.createScaledBitmap(logoBitmap, pixelSize, pixelSize, true)
                    val hex = com.dantsu.escposprinter.textparser.PrinterTextParserImg.bitmapToHexadecimalString(printer, resized)
                    receiptContent.replace("[LOGO]", "[C]<img>$hex</img>\n")
                } catch (imgEx: Exception) {
                    val storeName = "RM. MEKAR SARI"
                    receiptContent.replace("[LOGO]", "[C]<b>$storeName</b>\n")
                }
            } else {
                receiptContent.replace("[LOGO]", "")
            }

            printer.printFormattedTextAndCut(contentToPrint)
            // Berikan waktu 1 detik agar buffer Bluetooth terkirim sepenuhnya ke printer sebelum socket ditutup
            kotlinx.coroutines.delay(1000)
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(Exception("Izin Bluetooth ditolak. Pastikan izin Bluetooth Connect diberikan."))
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mencetak: ${e.localizedMessage}"))
        } finally {
            try {
                printer?.disconnectPrinter()
            } catch (ex: Exception) {
                // Ignore
            }
            try {
                connection?.disconnect()
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }
}
