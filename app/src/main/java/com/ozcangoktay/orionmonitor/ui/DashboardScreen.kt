package com.ozcangoktay.orionmonitor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ozcangoktay.orionmonitor.data.ConnectionState
import com.ozcangoktay.orionmonitor.data.InverterSnapshot
import com.ozcangoktay.orionmonitor.data.StatusBit
import java.util.Locale

private val SurfaceBlue = Color(0xFF3B4FA8)
private val CardBlue = Color(0xFF4C60C4)
private val Amber = Color(0xFFFFC94D)
private val Green = Color(0xFF4DE0A8)
private val Red = Color(0xFFFF7A7A)
private val Orange = Color(0xFFFF9E70)

@Composable
fun DashboardScreen(vm: MainViewModel) {
    val connection by vm.connectionState.collectAsState()
    val snap by vm.snapshot.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(SurfaceBlue)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = when (connection) {
                    is ConnectionState.Connected -> "Bağlı"
                    is ConnectionState.Connecting -> "Bağlanıyor…"
                    is ConnectionState.Error -> "Hata"
                    ConnectionState.Disconnected -> "Bağlı değil"
                },
                color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp
            )
            Icon(Icons.Default.Notifications, contentDescription = "Alarmlar",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.clickable { vm.navigate(AppScreen.ALARMS) })
        }

        Spacer(Modifier.height(8.dp))
        Text("Özcan Göktay", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(8.dp))

        if (snap == null) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    if (connection is ConnectionState.Connected) "Veri bekleniyor…" else "Bağlantı yok — altta \"1\" ile cihaz seçin",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp
                )
            }
        } else {
            val s = snap!!
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        MetricCard(
                            icon = Icons.Default.WbSunny, color = Amber,
                            powerText = "${"%.2f".format(s.pvChargePowerW / 1000.0)} kW",
                            subText = "${s.pvVoltage.toInt()} V · ${"%.1f".format(s.pvCurrent)} A",
                            label = "PV üretim"
                        )
                        MetricCard(
                            icon = Icons.Default.ElectricalServices,
                            color = if (s.hasGrid) Green else Color.White.copy(alpha = 0.4f),
                            powerText = "${"%.2f".format(s.gridChargePowerW / 1000.0)} kW",
                            subText = "${s.gridVoltage.toInt()} V",
                            label = if (s.hasGrid) "Şebeke" else "Şebeke yok"
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        MetricCard(
                            icon = Icons.Default.BatteryChargingFull,
                            color = if (s.isDischarging) Red else Green,
                            powerText = "${"%.2f".format(s.battPowerW / 1000.0)} kW",
                            subText = "${s.battVoltage} V · %${s.battSoc}",
                            label = if (s.isDischarging) "Akü (deşarj)" else "Akü (şarj)"
                        )
                        MetricCard(
                            icon = Icons.Default.Home, color = Orange,
                            powerText = "${"%.2f".format(s.invPowerW / 1000.0)} kW",
                            subText = "${s.invOutputVoltage.toInt()} V · %${s.loadRatioPercent}",
                            label = "Ev yükü"
                        )
                    }
                }

                val running = StatusBit.isSet(s.statusWord, StatusBit.INVERTER_RUNNING)
                val statusColor = if (!running) Red else if (StatusBit.isSet(s.statusWord, StatusBit.SLEEP_MODE_ACTIVE)) Color(0xFFFFC107) else Green
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (running) "İnverter" else "OFF", color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${s.inverterTempC}°C", color = statusColor, fontSize = 11.sp)
                    }
                }
            }

            val activeFaults = StatusBit.values().filter { it.isFault && StatusBit.isSet(s.statusWord, it) }
            if (activeFaults.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Red.copy(alpha = 0.2f))
                        .clickable { vm.navigate(AppScreen.ALARMS) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (activeFaults.size == 1) activeFaults[0].label else "${activeFaults.size} aktif alarm",
                        color = Red, fontSize = 12.sp, modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Cihaz bağlantısı: 0 = kes, 1 = cihaz seç ve bağlan
        Text("Cihaz bağlantısı", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { vm.disconnect() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (connection == ConnectionState.Disconnected) Red.copy(alpha = 0.25f) else CardBlue)
            ) { Text("0", fontWeight = FontWeight.Bold) }
            Button(
                onClick = { vm.navigate(AppScreen.BLUETOOTH) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (connection is ConnectionState.Connected) Green.copy(alpha = 0.25f) else CardBlue)
            ) { Text("1", fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            BottomNavItem(Icons.Default.Speed, "İzleme", selected = true) { vm.navigate(AppScreen.DASHBOARD) }
            BottomNavItem(Icons.Default.Settings, "Ayarlar", selected = false) { vm.navigate(AppScreen.SETTINGS) }
            BottomNavItem(Icons.Default.Notifications, "Alarmlar", selected = false) { vm.navigate(AppScreen.ALARMS) }
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Color.White else Color.White.copy(alpha = 0.6f))
        Text(label, color = if (selected) Color.White else Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

@Composable
private fun RowScope.MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    powerText: String,
    subText: String,
    label: String
) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                drawArc(
                    color = Color.White.copy(alpha = 0.15f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = -90f, sweepAngle = 270f, useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Icon(icon, contentDescription = label, tint = color)
        }
        Text(powerText, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(subText, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
    }
}
