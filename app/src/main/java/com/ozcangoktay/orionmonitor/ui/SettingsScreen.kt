package com.ozcangoktay.orionmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

private val SurfaceBlue = Color(0xFF3B4FA8)
private val CardBlue = Color(0xFF4C60C4)

@Composable
fun SettingsScreen(vm: MainViewModel) {
    var outputGrid by remember { mutableStateOf(false) } // false=İnverter, true=Şebeke
    var chargePriority by remember { mutableIntStateOf(1) } // 1=PV, 2=Şebeke, 3=Her ikisi
    var maxGridCurrent by remember { mutableFloatStateOf(10f) }
    var mpptFactor by remember { mutableFloatStateOf(150f) }
    var solarPhase by remember { mutableIntStateOf(1) }

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
            Text("Sistem ayarları", color = Color.White, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))

        SectionCard(title = "Şarj ve öncelik") {
            SettingRow("Çıkış önceliği") {
                Row {
                    ChoiceChip("İnverter", !outputGrid) { outputGrid = false; vm.setOutputPriorityGrid(false) }
                    Spacer(Modifier.width(6.dp))
                    ChoiceChip("Şebeke", outputGrid) { outputGrid = true; vm.setOutputPriorityGrid(true) }
                }
            }
            SettingRow("Şarj önceliği") {
                Row {
                    ChoiceChip("PV", chargePriority == 1) { chargePriority = 1; vm.setChargePriority(1) }
                    Spacer(Modifier.width(6.dp))
                    ChoiceChip("Şebeke", chargePriority == 2) { chargePriority = 2; vm.setChargePriority(2) }
                    Spacer(Modifier.width(6.dp))
                    ChoiceChip("Her ikisi", chargePriority == 3) { chargePriority = 3; vm.setChargePriority(3) }
                }
            }
            SettingSlider(
                label = "Maks. şebeke şarj akımı", value = maxGridCurrent, range = 5f..30f,
                valueText = "${maxGridCurrent.toInt()} A",
                onChange = { maxGridCurrent = it },
                onChangeFinished = { vm.setMaxGridChargeCurrent(maxGridCurrent.toInt()) }
            )
        }

        SectionCard(title = "Solar şarj") {
            SettingSlider(
                label = "Solar şarj MPPT katsayısı", value = mpptFactor, range = 0f..300f,
                valueText = "${mpptFactor.toInt()}",
                onChange = { mpptFactor = it },
                onChangeFinished = { vm.setMpptFactor(mpptFactor.toInt()) }
            )
            SettingRow("Solar şarj fazı") {
                Row {
                    (1..4).forEach { phase ->
                        ChoiceChip(phase.toString(), solarPhase == phase) {
                            solarPhase = phase; vm.setSolarPhase(phase)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
        }

        Text(
            "Not: Akü tipi ve DOD bağlantı kesme/bağlanma seviyeleri kılavuzun Modbus " +
                "tablosunda yer almıyor, bu yüzden bu sürümde cihaza yazılmıyor. Gerçek " +
                "register adresleri bulununca InverterRepository.writeSetting ile bağlanabilir.",
            color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBlue)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) { content() }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun SettingRow(label: String, control: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 13.sp)
        control()
    }
}

@Composable
private fun SettingSlider(
    label: String, value: Float, range: ClosedFloatingPointRange<Float>, valueText: String,
    onChange: (Float) -> Unit, onChangeFinished: () -> Unit
) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 13.sp)
            Text(valueText, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, onValueChangeFinished = onChangeFinished)
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color.White.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}
