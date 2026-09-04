package com.inverter.monitor

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/*
  Ayarlar Ekranı
  ===============
  Orijinal "SI Service Tool" APK'sının decompile edilmesiyle çözülen
  10 config seçeneği ve komut protokolü birebir uygulanmıştır.

  Gönderim formatı (orijinal uygulamayla aynı): 3 ayrı yazım
    "A<kod>"  ->  "<değer>"  ->  "Z\r\n"
*/
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val spBatteryType = findViewById<Spinner>(R.id.spBatteryType)
        val spMaxChargeCurrent = findViewById<Spinner>(R.id.spMaxChargeCurrent)
        val spOutputPriority = findViewById<Spinner>(R.id.spOutputPriority)
        val spChargePriority = findViewById<Spinner>(R.id.spChargePriority)
        val spSleepMode = findViewById<Spinner>(R.id.spSleepMode)
        val spMaxGridChargeCurrent = findViewById<Spinner>(R.id.spMaxGridChargeCurrent)
        val spRelayOutput = findViewById<Spinner>(R.id.spRelayOutput)
        val spDodDisconnect = findViewById<Spinner>(R.id.spDodDisconnect)
        val spDodReconnect = findViewById<Spinner>(R.id.spDodReconnect)
        val spWorkingMode = findViewById<Spinner>(R.id.spWorkingMode)

        // --- Orijinal APK'daki seçenek listeleri (birebir) ---
        setupSpinner(spBatteryType, listOf("AGM", "GEL", "FLUID", "LiFEPO4", "CARBON", "USER"))
        setupSpinner(spMaxChargeCurrent, listOf("10A", "20A", "30A", "40A", "50A", "60A"))
        setupSpinner(spOutputPriority, listOf("GRID", "ACU"))
        setupSpinner(spChargePriority, listOf("PV", "GRID", "PV+GRID"))
        setupSpinner(spSleepMode, listOf("ENABLE", "DISABLE"))
        setupSpinner(spMaxGridChargeCurrent, listOf("10A", "15A", "20A", "25A", "30A"))
        setupSpinner(spRelayOutput, listOf("RUN", "FAULT", "START", "LINE OFF"))
        setupSpinner(spDodDisconnect, listOf("%0", "%10", "%20", "%30", "%40", "%50"))
        setupSpinner(spDodReconnect, listOf("%0", "%10", "%20", "%30", "%40", "%50", "%60"))
        setupSpinner(spWorkingMode, listOf("INVERTER", "UPS", "OFF"))

        findViewById<Button>(R.id.btnApplyAll).setOnClickListener {
            if (!BluetoothManager.isConnected.get()) {
                Toast.makeText(this, "Önce invertere bağlanın.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // --- Komut kodları ve değer formülleri (APK'dan çözülen) ---
            BluetoothManager.sendConfig("100", (spBatteryType.selectedItemPosition + 1).toString())
            BluetoothManager.sendConfig("2", ((spMaxChargeCurrent.selectedItemPosition + 1) * 10).toString())
            BluetoothManager.sendConfig("300", spOutputPriority.selectedItemPosition.toString())
            BluetoothManager.sendConfig("400", (spChargePriority.selectedItemPosition + 1).toString())
            BluetoothManager.sendConfig("500", (spSleepMode.selectedItemPosition + 1).toString())
            BluetoothManager.sendConfig("60", ((spMaxGridChargeCurrent.selectedItemPosition + 1) * 5).toString())
            BluetoothManager.sendConfig("700", (spRelayOutput.selectedItemPosition + 1).toString())
            BluetoothManager.sendConfig("800", spDodDisconnect.selectedItemPosition.toString())
            BluetoothManager.sendConfig("900", spDodReconnect.selectedItemPosition.toString())
            BluetoothManager.sendConfig("000", (spWorkingMode.selectedItemPosition + 1).toString())

            Toast.makeText(this, "Ayarlar gönderildi.", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupSpinner(spinner: Spinner, items: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }
}
