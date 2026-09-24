package com.ozcangoktay.orionmonitor.modbus

import java.io.InputStream
import java.io.OutputStream

/**
 * Standart Modbus RTU çerçeveleme.
 * Kılavuzda belirtilen ayarlar: 9600 baud, 8 bit, 1 stop, parity yok.
 * Bluetooth modülü (HC-05 benzeri) şeffaf bir seri köprü olduğu için
 * ham baytları doğrudan BluetoothSocket InputStream/OutputStream üzerinden
 * gönderip alıyoruz.
 */
object ModbusRtu {

    /** Modbus RTU standart CRC16 (polinom 0xA001, başlangıç 0xFFFF). */
    fun crc16(data: ByteArray, length: Int = data.size): Int {
        var crc = 0xFFFF
        for (i in 0 until length) {
            crc = crc xor (data[i].toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 0x0001 != 0) {
                    (crc shr 1) xor 0xA001
                } else {
                    crc shr 1
                }
            }
        }
        return crc and 0xFFFF
    }

    /** Function code 0x03: Read Holding Registers isteği oluşturur. */
    fun buildReadHoldingRegisters(slaveId: Int, startAddress: Int, quantity: Int): ByteArray {
        val frame = ByteArray(6)
        frame[0] = slaveId.toByte()
        frame[1] = 0x03
        frame[2] = (startAddress shr 8).toByte()
        frame[3] = (startAddress and 0xFF).toByte()
        frame[4] = (quantity shr 8).toByte()
        frame[5] = (quantity and 0xFF).toByte()
        return appendCrc(frame)
    }

    /** Function code 0x06: Write Single Register isteği oluşturur. */
    fun buildWriteSingleRegister(slaveId: Int, address: Int, value: Int): ByteArray {
        val frame = ByteArray(6)
        frame[0] = slaveId.toByte()
        frame[1] = 0x06
        frame[2] = (address shr 8).toByte()
        frame[3] = (address and 0xFF).toByte()
        frame[4] = (value shr 8).toByte()
        frame[5] = (value and 0xFF).toByte()
        return appendCrc(frame)
    }

    private fun appendCrc(frame: ByteArray): ByteArray {
        val crc = crc16(frame, frame.size)
        return frame + byteArrayOf((crc and 0xFF).toByte(), (crc shr 8).toByte())
    }

    /**
     * 0x03 yanıtını okur: [slaveId][func=0x03][byteCount][data...][crcLo][crcHi]
     * Zaman aşımı ~1.5s; cihazdan yanıt gelmezse null döner.
     */
    fun readResponseRegisters(input: InputStream, expectedRegisterCount: Int): IntArray? {
        val header = readExact(input, 3) ?: return null
        val slaveId = header[0].toInt() and 0xFF
        val func = header[1].toInt() and 0xFF
        val byteCount = header[2].toInt() and 0xFF

        if (func == 0x83 || func == 0x86) {
            // Modbus exception yanıtı: [slaveId][func|0x80][exceptionCode][crc]
            readExact(input, 3)
            return null
        }
        if (byteCount != expectedRegisterCount * 2) return null

        val data = readExact(input, byteCount + 2) ?: return null // +2 = CRC
        val full = header + data
        val crcReceived = (data[byteCount].toInt() and 0xFF) or ((data[byteCount + 1].toInt() and 0xFF) shl 8)
        val crcCalc = crc16(full, full.size - 2)
        if (crcReceived != crcCalc) return null

        val regs = IntArray(expectedRegisterCount)
        for (i in 0 until expectedRegisterCount) {
            val hi = data[i * 2].toInt() and 0xFF
            val lo = data[i * 2 + 1].toInt() and 0xFF
            regs[i] = (hi shl 8) or lo
        }
        return regs
    }

    /** 0x06 yazma yanıtı cihazın isteği yankılamasıdır; sadece uzunluk kontrolü yeterli. */
    fun readWriteAck(input: InputStream): Boolean {
        val resp = readExact(input, 8) ?: return false
        return resp.size == 8
    }

    private fun readExact(input: InputStream, count: Int, timeoutMs: Long = 1500): ByteArray? {
        val buf = ByteArray(count)
        var read = 0
        val start = System.currentTimeMillis()
        while (read < count) {
            if (System.currentTimeMillis() - start > timeoutMs) return null
            val n = input.read(buf, read, count - read)
            if (n < 0) return null
            read += n
        }
        return buf
    }

    fun write(output: OutputStream, frame: ByteArray) {
        output.write(frame)
        output.flush()
    }
}

/** Bir 16-bit register'ı işaretli (signed) tam sayıya çevirir (+-W gibi alanlar için). */
fun Int.toSigned16(): Int = if (this and 0x8000 != 0) this - 0x10000 else this
