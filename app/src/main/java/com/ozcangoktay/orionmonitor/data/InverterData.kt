package com.ozcangoktay.orionmonitor.data

import com.ozcangoktay.orionmonitor.modbus.toSigned16

/**
 * Kaynak: kullanıcının paylaştığı "SV1500/3000/5000 INVERTER Kullanım Kılavuzu"
 * s.47 MODBUS bölümü. Register numaraları 40000 tabanlı (holding register),
 * bu yüzden gerçek Modbus adresi = (40000 sabiti) - 40001 = 39999 ofsetiyle DEĞİL;
 * çoğu üreticide "40000" gösterimi = Modbus adresi 0 demektir (1-tabanlı değil).
 * Burada REGISTER_BASE = 0 kabul ediyoruz; cihaz yanıt vermezse REGISTER_BASE'i
 * 40001 tabanına göre kaydırıp (yani 1 azaltıp/artırıp) tekrar deneyin — bu,
 * üreticiden üreticiye değişen yaygın bir kayma noktasıdır.
 */
object RegisterMap {
    const val SLAVE_ID = 1 // Kılavuzda slave ID belirtilmemiş; çoğu tekli cihazda 1'dir. Bağlanamazsa deneyin: 0, 2.

    const val START_ADDRESS = 0 // 40000 -> yerel adres 0
    const val COUNT = 25        // 40000..40024

    // --- Salt okunur ölçüm register'ları (COUNT içindeki index'leri) ---
    const val IDX_BATT_VOLTAGE = 0        // 40000, x10V
    const val IDX_BATT_DISCHARGE_CURRENT = 1 // 40001, x10A
    const val IDX_BATT_CHARGE_CURRENT = 2 // 40002, x10A
    const val IDX_BATT_POWER_SIGNED = 3   // 40003, +-W (işaretli)
    const val IDX_BATT_CURRENT_PERCENT = 4 // 40004
    const val IDX_BATT_SOC = 5            // 40005, şarj seviyesi %
    const val IDX_INVERTER_TEMP = 6       // 40006
    const val IDX_STATUS_WORD = 7         // 40007, bit alanı (Durum Bilgisi Tablosu)
    const val IDX_BATT_TEMP = 8           // 40008 (kılavuzda var, arayüzde göstermiyoruz)
    const val IDX_INV_OUTPUT_VOLTAGE = 9  // 40009
    const val IDX_INV_CURRENT = 10        // 40010
    const val IDX_INV_POWER = 11          // 40011
    const val IDX_LOAD_RATIO = 12         // 40012, çıkış yükünün maksimum yüke oranı %
    const val IDX_INV_GRID_MODE = 13      // 40013, 1/0
    const val IDX_GRID_CHARGE_POWER = 14  // 40014
    const val IDX_GRID_VOLTAGE = 15       // 40015
    const val IDX_PV_VOLTAGE = 19         // 40019
    const val IDX_PV_CURRENT = 20         // 40020
    const val IDX_PV_CHARGE_POWER = 21    // 40021

    // --- Yazılabilir (R/W) register'lar ---
    const val REG_OUTPUT_PRIORITY = 16    // 40016: 0=İnverter, 1=Şebeke
    const val REG_CHARGE_PRIORITY = 17    // 40017: 1=PV, 2=Şebeke, 3=Her ikisi
    const val REG_MAX_GRID_CHARGE_A = 18  // 40018: 5..30 A
    const val REG_MPPT_FACTOR = 22        // 40022: 0..300
    const val REG_SOLAR_PHASE = 23        // 40023: 1..4
    const val REG_SOLAR_CHARGE_STATE = 24 // 40024: 1/0 (salt okunur gibi görünüyor ama kılavuzda R/W işareti yok)

    // Kılavuzda YOK: akü tipi ve DOD kesme/bağlanma seviyeleri register'ları
    // dokümante edilmemiş. Orijinal uygulamanın ekranında bu alanlar vardı,
    // muhtemelen farklı (belgelenmemiş) bir register bloğunda. Bu yüzden bu
    // ikisi şimdilik sadece yerel (cihaza yazılmayan) arayüz alanları olarak
    // bırakıldı — gerçek register'ları bulunca InverterRepository.writeSetting
    // içine eklenmesi yeterli.
}

data class InverterSnapshot(
    val battVoltage: Double,
    val battDischargeCurrent: Double,
    val battChargeCurrent: Double,
    val battPowerW: Int,
    val battCurrentPercent: Int,
    val battSoc: Int,
    val inverterTempC: Int,
    val statusWord: Int,
    val invOutputVoltage: Double,
    val invCurrent: Double,
    val invPowerW: Int,
    val loadRatioPercent: Int,
    val invOnGridMode: Boolean,
    val gridChargePowerW: Int,
    val gridVoltage: Double,
    val pvVoltage: Double,
    val pvCurrent: Double,
    val pvChargePowerW: Int
) {
    val isDischarging: Boolean get() = battPowerW < 0
    val isCharging: Boolean get() = battPowerW > 0
    val hasGrid: Boolean get() = gridVoltage > 5.0

    companion object {
        fun fromRegisters(regs: IntArray): InverterSnapshot {
            fun r(i: Int) = regs[i]
            return InverterSnapshot(
                battVoltage = r(RegisterMap.IDX_BATT_VOLTAGE) / 10.0,
                battDischargeCurrent = r(RegisterMap.IDX_BATT_DISCHARGE_CURRENT) / 10.0,
                battChargeCurrent = r(RegisterMap.IDX_BATT_CHARGE_CURRENT) / 10.0,
                battPowerW = r(RegisterMap.IDX_BATT_POWER_SIGNED).toSigned16(),
                battCurrentPercent = r(RegisterMap.IDX_BATT_CURRENT_PERCENT),
                battSoc = r(RegisterMap.IDX_BATT_SOC),
                inverterTempC = r(RegisterMap.IDX_INVERTER_TEMP),
                statusWord = r(RegisterMap.IDX_STATUS_WORD),
                invOutputVoltage = r(RegisterMap.IDX_INV_OUTPUT_VOLTAGE).toDouble(),
                invCurrent = r(RegisterMap.IDX_INV_CURRENT).toDouble(),
                invPowerW = r(RegisterMap.IDX_INV_POWER),
                loadRatioPercent = r(RegisterMap.IDX_LOAD_RATIO),
                invOnGridMode = r(RegisterMap.IDX_INV_GRID_MODE) == 1,
                gridChargePowerW = r(RegisterMap.IDX_GRID_CHARGE_POWER),
                gridVoltage = r(RegisterMap.IDX_GRID_VOLTAGE).toDouble(),
                pvVoltage = r(RegisterMap.IDX_PV_VOLTAGE).toDouble(),
                pvCurrent = r(RegisterMap.IDX_PV_CURRENT).toDouble(),
                pvChargePowerW = r(RegisterMap.IDX_PV_CHARGE_POWER)
            )
        }
    }
}

/** Kılavuzdaki "Durum Bilgisi Tablosu" (register 40007) bit eşlemesi. */
enum class StatusBit(val bit: Int, val label: String, val isFault: Boolean) {
    INVERTER_RUNNING(0, "İnverter çalışıyor", false),
    OVER_TEMPERATURE(1, "Aşırı sıcaklık", true),
    OVERLOAD_OVER_200_ACTIVE(2, "%200'den büyük aşırı yük var", false),
    OVERLOAD_100_200_ACTIVE(3, "%100–%200 arasında aşırı yük var", false),
    FAN_FAULT(4, "Fan arızası", true),
    HIGH_BATTERY_VOLTAGE_FAULT(5, "Yüksek batarya voltajı arızası", true),
    HIGH_AC_OUTPUT_VOLTAGE_FAULT(6, "Yüksek AC çıkış voltajı arızası", true),
    LOW_BATTERY_VOLTAGE_FAULT(7, "Düşük batarya voltajı arızası", true),
    SLEEP_MODE_ACTIVE(8, "Uyku modu aktif", false),
    LOW_AC_OUTPUT_VOLTAGE_FAULT(9, "Düşük AC çıkış gerilimi arızası", true),
    OVERLOAD_100_200_FAULT(10, "Aşırı yük arızası (%100–200)", true),
    OVERLOAD_OVER_200_FAULT(11, "Aşırı yük arızası (>%200)", true),
    TRIGGER_MODE_NO_TRIGGER(12, "Tetikleme modu aktif, tetikleme yok", false),
    GENERATOR_MODE_CHARGING(13, "Jeneratör modu aktif, akü şarj yapılıyor", false),
    GRID_PRIORITY_ACTIVE(14, "Şebeke öncelik modu aktif, çıkış şebekeden besleniyor", false);

    companion object {
        fun isSet(statusWord: Int, bit: StatusBit): Boolean = (statusWord shr bit.bit) and 1 == 1
    }
}
