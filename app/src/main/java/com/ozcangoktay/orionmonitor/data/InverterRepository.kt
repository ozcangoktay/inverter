package com.ozcangoktay.orionmonitor.data

import com.ozcangoktay.orionmonitor.bluetooth.PairedDevice
import com.ozcangoktay.orionmonitor.bluetooth.SppBluetoothLink
import com.ozcangoktay.orionmonitor.modbus.ModbusRtu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState
    data class Error(val message: String) : ConnectionState
}

class InverterRepository {
    private val link = SppBluetoothLink()
    private var pollJob: Job? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _snapshot = MutableStateFlow<InverterSnapshot?>(null)
    val snapshot: StateFlow<InverterSnapshot?> = _snapshot.asStateFlow()

    fun pairedDevices(): List<PairedDevice> = link.pairedDevices()

    /** "0" tuşu: bağlantıyı ve anlık okuma döngüsünü tamamen keser. */
    fun disconnect() {
        pollJob?.cancel()
        pollJob = null
        link.disconnect()
        _connectionState.value = ConnectionState.Disconnected
        _snapshot.value = null
    }

    /** "1" tuşu sonrası cihaz seçilince çağrılır. */
    fun connectAndStartPolling(scope: CoroutineScope, address: String) {
        pollJob?.cancel()
        _connectionState.value = ConnectionState.Connecting

        pollJob = scope.launch(Dispatchers.IO) {
            try {
                link.connect(address)
                _connectionState.value = ConnectionState.Connected
            } catch (t: Throwable) {
                _connectionState.value = ConnectionState.Error(t.message ?: "Bağlantı hatası")
                return@launch
            }

            while (link.isConnected) {
                val ok = pollOnce()
                if (!ok) {
                    // Art arda birkaç başarısız okuma bağlantının koptuğunu gösterir.
                }
                delay(2000)
            }
        }
    }

    private fun pollOnce(): Boolean {
        val input = link.input ?: return false
        val output = link.output ?: return false
        return try {
            val request = ModbusRtu.buildReadHoldingRegisters(
                RegisterMap.SLAVE_ID, RegisterMap.START_ADDRESS, RegisterMap.COUNT
            )
            ModbusRtu.write(output, request)
            val regs = ModbusRtu.readResponseRegisters(input, RegisterMap.COUNT) ?: return false
            _snapshot.value = InverterSnapshot.fromRegisters(regs)
            true
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Ayarlar ekranındaki bir değeri cihaza yazar (function code 0x06).
     * Örnek: writeSetting(RegisterMap.REG_OUTPUT_PRIORITY, 1) // Şebeke
     */
    suspend fun writeSetting(register: Int, value: Int): Boolean = withContext(Dispatchers.IO) {
        val input = link.input ?: return@withContext false
        val output = link.output ?: return@withContext false
        try {
            val frame = ModbusRtu.buildWriteSingleRegister(RegisterMap.SLAVE_ID, register, value)
            ModbusRtu.write(output, frame)
            ModbusRtu.readWriteAck(input)
        } catch (t: Throwable) {
            false
        }
    }
}
