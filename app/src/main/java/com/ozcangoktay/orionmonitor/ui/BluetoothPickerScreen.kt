package com.ozcangoktay.orionmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ozcangoktay.orionmonitor.data.ConnectionState

private val SurfaceBlue = Color(0xFF3B4FA8)

@Composable
fun BluetoothPickerScreen(vm: MainViewModel) {
    val connection by vm.connectionState.collectAsState()
    val devices = remember { vm.pairedDevices() }

    LaunchedEffect(connection) {
        if (connection is ConnectionState.Connected) {
            vm.navigate(AppScreen.DASHBOARD)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(SurfaceBlue)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White,
                modifier = Modifier.clickable { vm.navigate(AppScreen.DASHBOARD) })
            Spacer(Modifier.width(8.dp))
            Text("Bluetooth cihazları", color = Color.White, fontSize = 16.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Telefonunuzun Bluetooth ayarlarından inverter modülüyle önce eşleşmeniz gerekir. " +
                "Eşleşmiş cihazlar aşağıda listelenir.",
            color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp
        )
        Spacer(Modifier.height(12.dp))

        if (devices.isEmpty()) {
            Text("Eşleşmiş cihaz bulunamadı.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        } else {
            devices.forEach { device ->
                val connecting = connection is ConnectionState.Connecting
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
                        Spacer(Modifier.width(8.dp))
                        Text(device.name, color = Color.White, fontSize = 13.sp)
                    }
                    Button(onClick = { vm.connectTo(device.address) }, enabled = !connecting) {
                        Text(if (connecting) "Bağlanıyor…" else "Bağlan")
                    }
                }
            }
        }

        if (connection is ConnectionState.Error) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Bağlantı hatası: ${(connection as ConnectionState.Error).message}",
                color = Color(0xFFFFB3B3), fontSize = 12.sp
            )
        }
    }
}
