package com.ozcangoktay.orionmonitor.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozcangoktay.orionmonitor.bluetooth.PairedDevice
import com.ozcangoktay.orionmonitor.data.ConnectionState
import com.ozcangoktay.orionmonitor.data.InverterRepository
import com.ozcangoktay.orionmonitor.data.RegisterMap
import kotlinx.coroutines.launch

enum class AppScreen { DASHBOARD, SETTINGS, ALARMS, BLUETOOTH }

class MainViewModel : ViewModel() {
    private val repo = InverterRepository()

    val connectionState = repo.connectionState
    val snapshot = repo.snapshot

    // Compose bu değişikliği doğrudan izleyip yeniden çizebilsin diye
    // mutableStateOf kullanıyoruz (ViewModel içinde kullanmak güvenlidir).
    var currentScreen by mutableStateOf(AppScreen.DASHBOARD)
        private set

    fun navigate(to: AppScreen) {
        currentScreen = to
    }

    fun pairedDevices(): List<PairedDevice> = repo.pairedDevices()

    /** Ana ekrandaki "0" tuşu. */
    fun disconnect() = repo.disconnect()

    /** Cihaz listesinde "Bağlan"a basınca. */
    fun connectTo(address: String) {
        repo.connectAndStartPolling(viewModelScope, address)
    }

    fun setOutputPriorityGrid(useGrid: Boolean) {
        viewModelScope.launch {
            repo.writeSetting(RegisterMap.REG_OUTPUT_PRIORITY, if (useGrid) 1 else 0)
        }
    }

    fun setChargePriority(value: Int) {
        // 1=PV, 2=Şebeke, 3=Her ikisi
        viewModelScope.launch { repo.writeSetting(RegisterMap.REG_CHARGE_PRIORITY, value) }
    }

    fun setMaxGridChargeCurrent(amps: Int) {
        viewModelScope.launch { repo.writeSetting(RegisterMap.REG_MAX_GRID_CHARGE_A, amps) }
    }

    fun setMpptFactor(value: Int) {
        viewModelScope.launch { repo.writeSetting(RegisterMap.REG_MPPT_FACTOR, value) }
    }

    fun setSolarPhase(value: Int) {
        viewModelScope.launch { repo.writeSetting(RegisterMap.REG_SOLAR_PHASE, value) }
    }
}
