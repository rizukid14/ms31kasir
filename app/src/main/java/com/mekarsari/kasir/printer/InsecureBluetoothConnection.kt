package com.mekarsari.kasir.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import java.io.IOException
import java.util.UUID

class InsecureBluetoothConnection(device: BluetoothDevice) : BluetoothConnection(device) {

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    override fun connect(): InsecureBluetoothConnection {
        if (this.isConnected) {
            return this
        }
        val device = this.device ?: throw EscPosConnectionException("Bluetooth device is null.")
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val uuid = try {
            this.deviceUUID ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        } catch (e: Exception) {
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        }

        var socket: BluetoothSocket? = null
        try {
            // 1. Attempt standard secure connection
            socket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothAdapter?.cancelDiscovery()
            socket.connect()
        } catch (e: Exception) {
            try {
                socket?.close()
            } catch (closeEx: Exception) {
                // Ignore
            }
            // 2. Fallback to insecure connection
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                bluetoothAdapter?.cancelDiscovery()
                socket.connect()
            } catch (insecureEx: Exception) {
                try {
                    socket?.close()
                } catch (closeEx: Exception) {
                    // Ignore
                }
                
                // 3. Last resort fallback: Create socket via reflection directly to Port 1 (frequently needed for cheap Chinese printers)
                try {
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    socket = m.invoke(device, 1) as BluetoothSocket
                    bluetoothAdapter?.cancelDiscovery()
                    socket.connect()
                } catch (reflectEx: Exception) {
                    try {
                        socket?.close()
                    } catch (cEx: Exception) {}
                    throw EscPosConnectionException("Failed to connect to Bluetooth device: " + reflectEx.message)
                }
            }
        }

        // 3. Set the socket and outputStream fields in the parent classes using reflection
        try {
            // Set socket field in BluetoothConnection class
            val socketField = BluetoothConnection::class.java.getDeclaredField("socket")
            socketField.isAccessible = true
            socketField.set(this, socket)

            // Set outputStream field in DeviceConnection class (parent of BluetoothConnection)
            val deviceConnClass = com.dantsu.escposprinter.connection.DeviceConnection::class.java
            val outputStreamField = deviceConnClass.getDeclaredField("outputStream")
            outputStreamField.isAccessible = true
            outputStreamField.set(this, socket?.outputStream)
        } catch (reflectEx: Exception) {
            try {
                socket?.close()
            } catch (closeEx: Exception) {}
            throw EscPosConnectionException("Failed to set connection fields via reflection: " + reflectEx.message)
        }

        return this
    }
}
