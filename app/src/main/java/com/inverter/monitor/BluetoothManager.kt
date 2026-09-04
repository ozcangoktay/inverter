package com.inverter.monitor

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Uygulama genelinde tek bir Bluetooth SPP bağlantısını paylaşan singleton.
 * MainActivity (izleme) ve SettingsActivity (ayarlar) aynı bağlantıyı kullanır.
 */
object BluetoothManager {

    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var socket: BluetoothSocket? = null
    var inputStream: InputStream? = null
        private set
    private var outputStream: OutputStream? = null

    val isConnected = AtomicBoolean(false)

    @Suppress("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        return try {
            val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
            sock.connect()
            socket = sock
            inputStream = sock.inputStream
            outputStream = sock.outputStream
            isConnected.set(true)
            true
        } catch (e: IOException) {
            Log.e("BluetoothManager", "Bağlantı hatası: ${e.message}")
            false
        }
    }

    fun disconnect() {
        try { inputStream?.close() } catch (_: IOException) {}
        try { outputStream?.close() } catch (_: IOException) {}
        try { socket?.close() } catch (_: IOException) {}
        inputStream = null
        outputStream = null
        socket = null
        isConnected.set(false)
    }

    // Orijinal APK'daki gibi: "A<kod>" -> "<değer>" -> "Z\r\n" şeklinde 3 ayrı gönderim
    fun sendConfig(code: String, value: String) {
        if (!isConnected.get()) return
        Thread {
            try {
                outputStream?.write("A$code".toByteArray())
                outputStream?.flush()
                Thread.sleep(80)
                outputStream?.write(value.toByteArray())
                outputStream?.flush()
                Thread.sleep(80)
                outputStream?.write("Z\r\n".toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                Log.e("BluetoothManager", "Komut gönderme hatası: ${e.message}")
            }
        }.start()
    }
}
