package com.inverter.monitor

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/*
  Orion Solar İnverter - Özel İzleme Uygulaması
  ===============================================
  HC-04 modülüne standart Klasik Bluetooth SPP (RFCOMM) üzerinden
  doğrudan bağlanır. Arayüz, inverterin fiziksel LCD ekranındaki yarım
  daire göstergeleri ve akış oklarını taklit eder.

  Protokol (APK analizinden çözüldü):
   - Cihaz sürekli ':' ile ayrılmış tek satırlık telemetri yayınlıyor
   - Alan 3: INV_V, 4: INV_P, 5: INV_LOAD, 6: GRID_V, 7: GRID_CHG_P,
     8: ACU_V, 9: ACU_P, 10: ACU_SOC, 11: PV_V, 12: PV_P, 13: TEMP,
     14: STATUS (bitmask)

  Kullanmadan önce:
   1) Telefonda Ayarlar > Bluetooth'tan "ORION_SOLAR" cihazını EŞLEŞTİRİN.
   2) Uygulamayı açın, "Bağlan" butonuna basın.
*/

class MainActivity : AppCompatActivity() {

    // GERÇEK CİHAZ ADI DOĞRULANDI: "ORION_SOLAR" (orijinal APK ekran görüntüsünden).
    private val TARGET_DEVICE_NAME_HINT = "ORION_SOLAR"

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val keepReading = AtomicBoolean(false)
    private var readThread: Thread? = null

    // Gauge max değerleri (görsel ölçek için - gerçek donanım kapasitesine göre ayarlanabilir)
    private val MAX_PV_W = 3000f
    private val MAX_GRID_W = 3000f
    private val MAX_LOAD_W = 3000f
    private val MAX_ACU_W = 3000f

    // UI referansları
    private lateinit var tvStatus: TextView
    private lateinit var tvAtsOut: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvFlags: TextView
    private lateinit var btnConnect: Button
    private lateinit var btnDisconnect: Button
    private lateinit var btnSettings: ImageButton
    private lateinit var lineChart: LineChart

    private lateinit var tvToggleOn: TextView
    private lateinit var tvToggleOff: TextView

    private lateinit var gaugePv: GaugeView
    private lateinit var gaugeGrid: GaugeView
    private lateinit var gaugeLoad: GaugeView
    private lateinit var gaugeAcu: GaugeView

    private lateinit var tvPvP: TextView
    private lateinit var tvGridP: TextView
    private lateinit var tvLoadP: TextView
    private lateinit var tvAcuP: TextView

    private lateinit var tvPvVA: TextView
    private lateinit var tvGridVA: TextView
    private lateinit var tvLoadVA: TextView
    private lateinit var tvAcuVA: TextView

    private lateinit var tvAcuSoc: TextView

    private lateinit var chevPv: LinearLayout
    private lateinit var chevGrid: LinearLayout
    private lateinit var chevLoad: LinearLayout
    private lateinit var chevAcu: LinearLayout

    private val chevronAnimators = mutableListOf<ValueAnimator>()

    // Geçmiş veri (grafik için) - kayan pencere
    private val maxHistoryPoints = 120 // ~2 dakika (1 örnek/sn varsayımıyla)
    private val invPHistory = ArrayList<Entry>()
    private val pvPHistory = ArrayList<Entry>()
    private val gridPHistory = ArrayList<Entry>()
    private var sampleIndex = 0f

    // STATUS bitmask etiketleri (APK'dan çözülen)
    private val statusFlags = listOf(
        1L to "SISTEM_RUNNING",
        2L to "OVER_TEMPERATURE_FAULT",
        4L to "OWERLOAD200",
        8L to "OWERLOAD100",
        16L to "FAN_FAULT",
        64L to "HIGH_BATTERY_VOLTAGE_FAULT",
        128L to "LOW_BATTERY_VOLTAGE_FAULT",
        256L to "OWERLOAD100_FAULT",
        512L to "LOW_OUTPUT_VOLTAGE_FAULT",
        1024L to "OWERLOAD200_FAULT",
        2048L to "POWER_SAVING",
        8192L to "GENERATOR_MODE",
        16384L to "OSP_GRID"
    )

    private val permissionRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvAtsOut = findViewById(R.id.tvAtsOut)
        tvTemp = findViewById(R.id.tvTemp)
        tvFlags = findViewById(R.id.tvFlags)
        btnConnect = findViewById(R.id.btnConnect)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        btnSettings = findViewById(R.id.btnSettings)
        lineChart = findViewById(R.id.chartHistory)

        tvToggleOn = findViewById(R.id.tvToggleOn)
        tvToggleOff = findViewById(R.id.tvToggleOff)

        gaugePv = findViewById(R.id.gaugePv)
        gaugeGrid = findViewById(R.id.gaugeGrid)
        gaugeLoad = findViewById(R.id.gaugeLoad)
        gaugeAcu = findViewById(R.id.gaugeAcu)

        tvPvP = findViewById(R.id.tvPvP)
        tvGridP = findViewById(R.id.tvGridP)
        tvLoadP = findViewById(R.id.tvLoadP)
        tvAcuP = findViewById(R.id.tvAcuP)

        tvPvVA = findViewById(R.id.tvPvVA)
        tvGridVA = findViewById(R.id.tvGridVA)
        tvLoadVA = findViewById(R.id.tvLoadVA)
        tvAcuVA = findViewById(R.id.tvAcuVA)

        tvAcuSoc = findViewById(R.id.tvAcuSoc)

        chevPv = findViewById(R.id.chevPv)
        chevGrid = findViewById(R.id.chevGrid)
        chevLoad = findViewById(R.id.chevLoad)
        chevAcu = findViewById(R.id.chevAcu)

        setupChart()
        startChevronAnimations()

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        btnConnect.setOnClickListener { onConnectClicked() }
        btnDisconnect.setOnClickListener { disconnectFromDevice() }
        btnSettings.setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
        }

        val toggleContainer = findViewById<LinearLayout>(R.id.btToggleContainer)
        toggleContainer.setOnClickListener {
            if (BluetoothManager.isConnected.get()) {
                disconnectFromDevice()
            } else {
                onConnectClicked()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateConnectionUi(BluetoothManager.isConnected.get())
        if (BluetoothManager.isConnected.get() && (readThread == null || readThread?.isAlive != true)) {
            startReadLoop()
        }
    }

    // ---------------- BAĞLANTI GÖSTERGE GÜNCELLEME ----------------
    private fun updateConnectionUi(connected: Boolean) {
        if (connected) {
            tvStatus.text = "CONNECTED"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_connected))
            tvToggleOn.setBackgroundColor(ContextCompat.getColor(this, R.color.status_connected))
            tvToggleOn.setTextColor(ContextCompat.getColor(this, R.color.bg_dark))
            tvToggleOff.setBackgroundColor(Color.TRANSPARENT)
            tvToggleOff.setTextColor(ContextCompat.getColor(this, R.color.lcd_muted))
        } else {
            tvStatus.text = "DISCONNECTED"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_disconnected))
            tvToggleOn.setBackgroundColor(Color.TRANSPARENT)
            tvToggleOn.setTextColor(ContextCompat.getColor(this, R.color.lcd_muted))
            tvToggleOff.setBackgroundColor(ContextCompat.getColor(this, R.color.btn_disconnect))
            tvToggleOff.setTextColor(ContextCompat.getColor(this, R.color.bg_dark))
        }
    }

    // ---------------- CHEVRON (AKIŞ OKU) ANİMASYONU ----------------
    // Her sütunda 3 ok var, üstteki widget'taki gibi sırayla parlayıp sönerek
    // akış yönünü hissettiriyor (yukarı akan sütunlarda üstten alta, şebekede tersi).
    private fun startChevronAnimations() {
        animateChevronGroup(chevPv, reverse = false)
        animateChevronGroup(chevGrid, reverse = true)
        animateChevronGroup(chevLoad, reverse = false)
        animateChevronGroup(chevAcu, reverse = false)
    }

    private fun animateChevronGroup(group: LinearLayout, reverse: Boolean) {
        val children = (0 until group.childCount).map { group.getChildAt(it) }
        val ordered = if (reverse) children.reversed() else children
        ordered.forEachIndexed { index, view ->
            val animator = ObjectAnimator.ofFloat(view, "alpha", 0.25f, 1f, 0.25f).apply {
                duration = 1200
                startDelay = index * 150L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
            animator.start()
            chevronAnimators.add(animator)
        }
    }

    private fun stopChevronAnimations() {
        chevronAnimators.forEach { it.cancel() }
        chevronAnimators.clear()
    }

    // ---------------- GRAFİK ----------------
    private fun setupChart() {
        lineChart.description.isEnabled = false
        lineChart.legend.textColor = Color.parseColor("#e2e8f7")
        lineChart.xAxis.textColor = Color.parseColor("#e2e8f7")
        lineChart.axisLeft.textColor = Color.parseColor("#e2e8f7")
        lineChart.axisRight.isEnabled = false
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.setNoDataText("Veri bekleniyor...")
        lineChart.setNoDataTextColor(Color.parseColor("#e2e8f7"))
        lineChart.invalidate()
    }

    private fun addHistoryPoint(invP: Float, pvP: Float, gridP: Float) {
        invPHistory.add(Entry(sampleIndex, invP))
        pvPHistory.add(Entry(sampleIndex, pvP))
        gridPHistory.add(Entry(sampleIndex, gridP))
        sampleIndex += 1f

        if (invPHistory.size > maxHistoryPoints) invPHistory.removeAt(0)
        if (pvPHistory.size > maxHistoryPoints) pvPHistory.removeAt(0)
        if (gridPHistory.size > maxHistoryPoints) gridPHistory.removeAt(0)

        updateChart()
    }

    private fun updateChart() {
        val invSet = LineDataSet(ArrayList(invPHistory), "İnverter W").apply {
            color = Color.parseColor("#e24b4a")
            setDrawCircles(false); lineWidth = 1.8f; setDrawValues(false)
        }
        val pvSet = LineDataSet(ArrayList(pvPHistory), "PV W").apply {
            color = Color.parseColor("#f5a623")
            setDrawCircles(false); lineWidth = 1.8f; setDrawValues(false)
        }
        val gridSet = LineDataSet(ArrayList(gridPHistory), "Şebeke W").apply {
            color = Color.parseColor("#7dd3f0")
            setDrawCircles(false); lineWidth = 1.8f; setDrawValues(false)
        }

        val dataSets: List<ILineDataSet> = listOf(invSet, pvSet, gridSet)
        lineChart.data = LineData(dataSets)
        lineChart.invalidate()
    }

    private fun clearChart() {
        invPHistory.clear(); pvPHistory.clear(); gridPHistory.clear()
        sampleIndex = 0f
        lineChart.clear()
        lineChart.invalidate()
    }

    // ---------------- İZİNLER ----------------
    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestRequiredPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        ActivityCompat.requestPermissions(this, perms, permissionRequestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onConnectClicked()
            } else {
                Toast.makeText(this, "Bluetooth izinleri gerekli.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ---------------- BAĞLANTI ----------------
    private fun onConnectClicked() {
        if (!hasRequiredPermissions()) {
            requestRequiredPermissions()
            return
        }
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bu cihazda Bluetooth bulunamadı.", Toast.LENGTH_LONG).show()
            return
        }
        if (bluetoothAdapter?.isEnabled != true) {
            Toast.makeText(this, "Lütfen Bluetooth'u açın.", Toast.LENGTH_LONG).show()
            return
        }
        connectToDevice()
    }

    @Suppress("MissingPermission") // hasRequiredPermissions() ile önceden kontrol ediliyor
    private fun connectToDevice() {
        val target: BluetoothDevice? = bluetoothAdapter?.bondedDevices?.firstOrNull { device ->
            device.name?.contains(TARGET_DEVICE_NAME_HINT, ignoreCase = true) == true
        }

        if (target == null) {
            Toast.makeText(
                this,
                "Eşleştirilmiş cihazlar arasında '$TARGET_DEVICE_NAME_HINT' bulunamadı. " +
                "Önce Android Bluetooth ayarlarından inverterinizi eşleştirin.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        Thread {
            bluetoothAdapter?.cancelDiscovery()
            val ok = BluetoothManager.connect(target)

            if (ok) {
                runOnUiThread {
                    updateConnectionUi(true)
                    Toast.makeText(this, "İnvertere bağlandı.", Toast.LENGTH_SHORT).show()
                }
                startReadLoop()
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Bağlantı başarısız.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun disconnectFromDevice() {
        keepReading.set(false)
        BluetoothManager.disconnect()
        runOnUiThread {
            updateConnectionUi(false)
            clearValues()
            clearChart()
        }
    }

    // ---------------- OKUMA + PARSE ----------------
    private fun startReadLoop() {
        keepReading.set(true)
        readThread = Thread {
            val buffer = StringBuilder()
            val byteBuf = ByteArray(1024)
            while (keepReading.get()) {
                try {
                    val stream = BluetoothManager.inputStream ?: break
                    val bytesRead = stream.read(byteBuf)
                    if (bytesRead > 0) {
                        val chunk = String(byteBuf, 0, bytesRead, Charsets.US_ASCII)
                        for (ch in chunk) {
                            if (ch == '\n' || ch == '\r') {
                                if (buffer.isNotEmpty()) {
                                    val line = buffer.toString()
                                    buffer.clear()
                                    parseAndDisplay(line)
                                }
                            } else {
                                buffer.append(ch)
                                if (buffer.length > 300) buffer.clear() // taşma koruması
                            }
                        }
                    }
                } catch (e: IOException) {
                    Log.e("BT", "Okuma hatası, bağlantı koptu: ${e.message}")
                    keepReading.set(false)
                    runOnUiThread {
                        updateConnectionUi(false)
                        Toast.makeText(this, "Bağlantı koptu.", Toast.LENGTH_SHORT).show()
                    }
                    BluetoothManager.disconnect()
                }
            }
        }
        readThread?.start()
    }

    private fun parseAndDisplay(line: String) {
        val fields = line.split(":")
        if (fields.size < 14) return // eksik/bozuk paket, atla

        fun f(i: Int): Float = fields.getOrNull(i)?.toFloatOrNull() ?: 0f

        val invV = f(2); val invP = f(3); val invLoad = f(4)
        val gridV = f(5); val gridChgP = f(6)
        val acuV = f(7); val acuP = f(8); val acuSoc = f(9)
        val pvV = f(10); val pvP = f(11)
        val temp = f(12)
        val statusBits = fields.getOrNull(13)?.toLongOrNull() ?: 0L

        val acuI = if (acuV > 0.1f) acuP / acuV else 0f
        val pvI = if (pvV > 0.1f) pvP / pvV else 0f
        val gridI = if (gridV > 0.1f) gridChgP / gridV else 0f

        val activeFlags = statusFlags.filter { (bit, _) -> (statusBits and bit) != 0L }
            .joinToString(", ") { it.second }

        val greenColor = Color.parseColor("#3ecf3e")
        val redColor = Color.parseColor("#e05a4a")

        runOnUiThread {
            gaugePv.setValue(pvP, MAX_PV_W, greenColor)
            tvPvP.text = "%.0f W".format(pvP)
            tvPvVA.text = "%.1fV · %.1fA".format(pvV, pvI)

            gaugeGrid.setValue(gridChgP, MAX_GRID_W, greenColor)
            tvGridP.text = "%.0f W".format(gridChgP)
            tvGridVA.text = "%.1fV · %.1fA".format(gridV, gridI)

            gaugeLoad.setValue(invLoad / 100f * MAX_LOAD_W, MAX_LOAD_W, greenColor)
            tvLoadP.text = "%.0f W".format(invP)
            tvLoadVA.text = "%.1fV · %.1fA".format(invV, if (invV > 0.1f) invP / invV else 0f)

            gaugeAcu.setValue(kotlin.math.abs(acuP), MAX_ACU_W, redColor)
            tvAcuP.text = "%.0f W".format(acuP)
            tvAcuVA.text = "%.1fV · %.1fA".format(acuV, acuI)
            tvAcuSoc.text = "%%%.0f".format(acuSoc)

            tvTemp.text = "%.0f°C".format(temp)
            tvFlags.text = if (activeFlags.isEmpty()) "Aktif uyarı yok" else activeFlags
            addHistoryPoint(invP, pvP, gridChgP)
        }
    }

    private fun clearValues() {
        val dash = "-"
        tvPvP.text = "$dash W"; tvPvVA.text = "$dash V · $dash A"
        tvGridP.text = "$dash W"; tvGridVA.text = "$dash V · $dash A"
        tvLoadP.text = "$dash W"; tvLoadVA.text = "$dash V · $dash A"
        tvAcuP.text = "$dash W"; tvAcuVA.text = "$dash V · $dash A"
        tvAcuSoc.text = "%$dash"
        tvAtsOut.text = dash
        tvTemp.text = "$dash °C"
        tvFlags.text = "Aktif uyarı yok"
        gaugePv.setValue(0f, MAX_PV_W, Color.parseColor("#3ecf3e"))
        gaugeGrid.setValue(0f, MAX_GRID_W, Color.parseColor("#3ecf3e"))
        gaugeLoad.setValue(0f, MAX_LOAD_W, Color.parseColor("#3ecf3e"))
        gaugeAcu.setValue(0f, MAX_ACU_W, Color.parseColor("#e05a4a"))
    }

    override fun onDestroy() {
        super.onDestroy()
        keepReading.set(false)
        stopChevronAnimations()
        // Not: Ayarlar ekranına geçerken bağlantının kopmaması için burada
        // BluetoothManager.disconnect() ÇAĞRILMIYOR - sadece okuma thread'i durduruluyor.
        // Gerçek bağlantı kesme işlemi sadece "Kes" butonuyla / toggle ile yapılır.
    }
}
