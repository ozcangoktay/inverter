package com.ozcangoktay.orionmonitor

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.ozcangoktay.orionmonitor.ui.*

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Sonuc, Bluetooth ekraninda cihaz listesi bos/dolu cikmasindan anlasilir */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            needed += Manifest.permission.BLUETOOTH_CONNECT
            needed += Manifest.permission.BLUETOOTH_SCAN
        } else {
            needed += Manifest.permission.ACCESS_FINE_LOCATION
        }
        permissionLauncher.launch(needed.toTypedArray())

        setContent { AppRoot(vm) }
    }
}

@Composable
private fun AppRoot(vm: MainViewModel) {
    when (vm.currentScreen) {
        AppScreen.DASHBOARD -> DashboardScreen(vm)
        AppScreen.SETTINGS -> SettingsScreen(vm)
        AppScreen.ALARMS -> AlarmsScreen(vm)
        AppScreen.BLUETOOTH -> BluetoothPickerScreen(vm)
    }
}
