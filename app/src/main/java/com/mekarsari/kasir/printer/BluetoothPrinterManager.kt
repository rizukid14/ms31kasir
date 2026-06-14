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

    // List of paired devices (MAC to Name)
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<Pair<String, String>> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return try {
            adapter.bondedDevices.map { it.address to (it.name ?: "Device Tidak Dikenal") }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    suspend fun printReceipt(
        macAddress: String,
        receiptContent: String,
        logoBitmap: Bitmap? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (macAddress.isEmpty()) {
            return@withContext Result.failure(Exception("MAC Address printer belum diatur"))
        }

        val adapter = bluetoothAdapter ?: return@withContext Result.failure(Exception("Bluetooth tidak didukung di perangkat ini"))
        if (!adapter.isEnabled) {
            return@withContext Result.failure(Exception("Bluetooth dinonaktifkan"))
        }

        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
            val connection = BluetoothConnection(device)
            
            // 203 DPI, 48mm width (approx 384 dots), 32 characters per line (typical 58mm printer)
            val printer = EscPosPrinter(connection, 203, 48f, 32)
            
            val contentToPrint = if (logoBitmap != null && receiptContent.contains("[LOGO]")) {
                try {
                    // Resize to fit standard thermal printer (e.g. 120x120px)
                    val resized = Bitmap.createScaledBitmap(logoBitmap, 120, 120, true)
                    val hex = com.dantsu.escposprinter.textparser.PrinterTextParserImg.bitmapToHexadecimalString(printer, resized)
                    receiptContent.replace("[LOGO]", "[C]<img>$hex</img>\n")
                } catch (imgEx: Exception) {
                    receiptContent.replace("[LOGO]", "")
                }
            } else {
                receiptContent.replace("[LOGO]", "")
            }

            printer.printFormattedTextAndCut(contentToPrint)
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(Exception("Izin Bluetooth ditolak. Pastikan izin Bluetooth Connect diberikan."))
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mencetak: ${e.localizedMessage}"))
        }
    }
}
