package com.ozcangoktay.orionmonitor.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * HC-05 tarzı modüllerin kullandığı standart Seri Port Profili (SPP) UUID'si.
 * Kılavuzda modülün adı geçmiyor ama "Bluetooth" simgesiyle şeffaf bir
 * RS485<->BT köprüsü olduğu belirtiliyor; bu UUID neredeyse tüm SPP
 * modüllerinde ortaktır.
 */
private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

data class PairedDevice(val name: String, val address: String)

class SppBluetoothLink {
    private var socket: BluetoothSocket? = null
    var input: InputStream? = null
        private set
    var output: OutputStream? = null
        private set

    val isConnected: Boolean get() = socket?.isConnected == true

    @SuppressLint("MissingPermission") // Çağıran taraf BLUETOOTH_CONNECT iznini kontrol etmeli
    fun pairedDevices(): List<PairedDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices.map { PairedDevice(it.name ?: it.address, it.address) }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: error("Bu cihazda Bluetooth donanımı bulunamadı")
        val device: BluetoothDevice = adapter.getRemoteDevice(address)

        adapter.cancelDiscovery()

        val newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
        newSocket.connect()

        socket = newSocket
        input = newSocket.inputStream
        output = newSocket.outputStream
    }

    fun disconnect() {
        runCatching { input?.close() }
        runCatching { output?.close() }
        runCatching { socket?.close() }
        socket = null
        input = null
        output = null
    }
}
