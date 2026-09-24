package com.ozcangoktay.orionmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ozcangoktay.orionmonitor.data.StatusBit

private val SurfaceBlue = Color(0xFF3B4FA8)

@Composable
fun AlarmsScreen(vm: MainViewModel) {
    val snap by vm.snapshot.collectAsState()
    val statusWord = snap?.statusWord ?: 0

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
            Text("Alarmlar ve durum", color = Color.White, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))

        Text("ARIZALAR", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        StatusBit.values().filter { it.isFault }.forEach { bit ->
            AlarmRow(bit.label, StatusBit.isSet(statusWord, bit), faultStyle = true)
        }

        Spacer(Modifier.height(14.dp))
        Text("DURUM BİLGİLERİ", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        StatusBit.values().filter { !it.isFault }.forEach { bit ->
            AlarmRow(bit.label, StatusBit.isSet(statusWord, bit), faultStyle = false)
        }
    }
}

@Composable
private fun AlarmRow(label: String, active: Boolean, faultStyle: Boolean) {
    val dotColor = if (!active) Color(0xFF2ec478) else if (faultStyle) Color(0xFFFF5A5A) else Color(0xFF7FB8FF)
    val textColor = if (!active) Color(0xFF8CF0BB) else if (faultStyle) Color(0xFFFFB3B3) else Color(0xFFBFE0FF)
    val statusText = if (active) "Aktif" else if (faultStyle) "Normal" else "Kapalı"

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(8.dp))
            Text(label, color = Color.White, fontSize = 13.sp)
        }
        Text(statusText, color = textColor, fontSize = 11.sp)
    }
}
