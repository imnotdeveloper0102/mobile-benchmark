package com.mdevice.benchmark

import com.mdevice.benchmark.util.SysFs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SysFsParserTest {

    @Test
    fun parseThermalTemp_milliCelsius() {
        assertEquals(42.0, SysFs.parseThermalTempMilliC(42000L) ?: -1.0, 0.001)
    }

    @Test
    fun parseThermalTemp_plainCelsius() {
        assertEquals(42.0, SysFs.parseThermalTempMilliC(42L) ?: -1.0, 0.001)
    }

    @Test
    fun parseThermalTemp_null_isNull() {
        assertNull(SysFs.parseThermalTempMilliC(null))
    }

    @Test
    fun parseThermalTemp_outOfRange_isNull() {
        assertNull(SysFs.parseThermalTempMilliC(500000L))
        assertNull(SysFs.parseThermalTempMilliC(-100000L))
    }

    @Test
    fun parseFreq_khzToMhz() {
        assertEquals(1800.0, SysFs.parseFreqKhzToMhz(1800000L) ?: -1.0, 0.001)
    }

    @Test
    fun parseFreq_zero_isNull() {
        assertNull(SysFs.parseFreqKhzToMhz(0L))
        assertNull(SysFs.parseFreqKhzToMhz(null))
    }

    @Test
    fun procStat_loadCalculation() {
        val first = SysFs.ProcStatCpu(idle = 1000L, total = 5000L)
        val second = SysFs.ProcStatCpu(idle = 1100L, total = 6000L)
        // total delta 1000, idle delta 100 -> load 90%.
        assertEquals(90.0, second.loadSince(first) ?: -1.0, 0.001)
    }

    @Test
    fun procStat_zeroDelta_isNull() {
        val same = SysFs.ProcStatCpu(idle = 1000L, total = 5000L)
        assertNull(same.loadSince(same))
    }

    @Test
    fun readText_missingFile_isNull() {
        assertNull(SysFs.readText("/sys/khong-ton-tai/xyz"))
    }
}
