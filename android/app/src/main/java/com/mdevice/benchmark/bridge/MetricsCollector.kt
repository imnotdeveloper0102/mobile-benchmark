package com.mdevice.benchmark.bridge

import android.content.Context
import com.mdevice.benchmark.metrics.BatteryMetrics
import com.mdevice.benchmark.metrics.CpuMetrics
import com.mdevice.benchmark.metrics.DeviceInfoCollector
import com.mdevice.benchmark.metrics.FpsMonitor
import com.mdevice.benchmark.metrics.MemoryMetrics
import com.mdevice.benchmark.metrics.NetworkMetrics
import com.mdevice.benchmark.metrics.StorageMetrics
import com.mdevice.benchmark.metrics.ThermalMetrics
import com.mdevice.benchmark.storage.AppDatabase
import com.mdevice.benchmark.storage.MetricEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gom tat ca metrics thanh 1 snapshot JSON dung hop dong bridge.
 * Truong nao khong doc duoc thi de null trong JSON va them 1 muc vao
 * mang "unavailable" kem ly do. Khong bao gio dien gia tri uoc doan
 * vao cho cua so khong doc duoc.
 */
class MetricsCollector(context: Context) {

    private val appContext = context.applicationContext
    private val cpu = CpuMetrics()
    private val memory = MemoryMetrics(appContext)
    private val thermal = ThermalMetrics(appContext)
    private val battery = BatteryMetrics(appContext)
    private val fpsMonitor = FpsMonitor(appContext)
    private val storage = StorageMetrics(appContext)
    private val network = NetworkMetrics(appContext)
    private val deviceInfo = DeviceInfoCollector(appContext)
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        fpsMonitor.start()
    }

    fun stop() {
        fpsMonitor.stop()
    }

    fun collectSnapshot(): JSONObject {
        val unavailable = JSONArray()
        fun mark(field: String, reason: String) {
            unavailable.put(JSONObject().apply {
                put("field", field)
                put("reason", reason)
            })
        }

        val cpuResult = cpu.collect()
        if (cpuResult.loadEstimatePct == null) {
            mark("cpu.loadEstimatePct", cpuResult.loadNote)
        }
        if (cpuResult.freqMHz.all { it == null }) {
            mark("cpu.freqMHz", "Khong doc duoc /sys/devices/system/cpu/cpuN/cpufreq/scaling_cur_freq tren thiet bi nay.")
        }

        val mem = memory.collect()
        val therm = thermal.collect()
        if (therm.cpuTempC == null) {
            mark("thermal.cpuTempC", "Khong tim thay thermal zone CPU trong /sys/class/thermal.")
        }
        if (therm.gpuTempC == null) {
            mark("thermal.gpuTempC", "Khong tim thay thermal zone GPU trong /sys/class/thermal.")
        }
        if (therm.batteryTempC == null) {
            mark("thermal.batteryTempC", "Khong doc duoc nhiet do pin qua BatteryManager.")
        }

        val batt = battery.collect()
        if (batt.currentNowUa == null) {
            mark("battery.currentNowUa", "BATTERY_PROPERTY_CURRENT_NOW khong duoc ho tro tren thiet bi nay.")
        }
        if (batt.powerWatts == null) {
            mark("battery.powerWatts", "Khong tinh duoc cong suat vi thieu dong hoac dien ap.")
        }
        if (batt.timeRemainingMin == null) {
            mark("battery.timeRemainingMin", "Chi uoc tinh duoc khi dang xa pin va doc duoc dong xa.")
        }

        val fps = fpsMonitor.snapshot()
        val cap = storage.capacity()
        if (cap.externalTotalBytes == null) {
            mark("storage.externalTotalBytes", "Khong co bo nho ngoai hoac khong doc duoc.")
        }

        val net = network.collect()
        if (net.signalDbm == null) {
            mark("network.signalDbm", "Khong doc duoc cuong do tin hieu (can quyen dien thoai hoac khong co song di dong).")
        }

        val freqArray = JSONArray()
        for (f in cpuResult.freqMHz) {
            if (f != null) freqArray.put(f) else freqArray.put(JSONObject.NULL)
        }
        val maxFreqArray = JSONArray()
        for (f in cpuResult.maxFreqMHz) {
            if (f != null) maxFreqArray.put(f) else maxFreqArray.put(JSONObject.NULL)
        }

        return JSONObject().apply {
            put("timestampMs", System.currentTimeMillis())
            put("cpu", JSONObject().apply {
                put("cores", cpuResult.cores)
                put("freqMHz", freqArray)
                put("maxFreqMHz", maxFreqArray)
                if (cpuResult.loadEstimatePct != null) put("loadEstimatePct", cpuResult.loadEstimatePct)
                else put("loadEstimatePct", JSONObject.NULL)
                put("loadMethod", cpuResult.loadMethod)
                put("loadNote", cpuResult.loadNote)
            })
            put("memory", JSONObject().apply {
                put("totalBytes", mem.totalBytes)
                put("availBytes", mem.availBytes)
                put("usedBytes", mem.usedBytes)
                put("lowMemory", mem.lowMemory)
                put("thresholdBytes", mem.thresholdBytes)
                put("appPssBytes", mem.appPssBytes)
            })
            put("thermal", JSONObject().apply {
                if (therm.batteryTempC != null) put("batteryTempC", therm.batteryTempC) else put("batteryTempC", JSONObject.NULL)
                if (therm.cpuTempC != null) put("cpuTempC", therm.cpuTempC) else put("cpuTempC", JSONObject.NULL)
                if (therm.gpuTempC != null) put("gpuTempC", therm.gpuTempC) else put("gpuTempC", JSONObject.NULL)
                put("thermalStatus", therm.thermalStatus)
                put("overheating", therm.overheating)
            })
            put("battery", JSONObject().apply {
                put("levelPct", batt.levelPct)
                put("charging", batt.charging)
                if (batt.currentNowUa != null) put("currentNowUa", batt.currentNowUa) else put("currentNowUa", JSONObject.NULL)
                if (batt.voltageMv != null) put("voltageMv", batt.voltageMv) else put("voltageMv", JSONObject.NULL)
                if (batt.powerWatts != null) put("powerWatts", batt.powerWatts) else put("powerWatts", JSONObject.NULL)
                put("health", batt.health)
                if (batt.timeRemainingMin != null) put("timeRemainingMin", batt.timeRemainingMin) else put("timeRemainingMin", JSONObject.NULL)
            })
            put("fps", JSONObject().apply {
                put("fps", fps.fps)
                put("jankPct", fps.jankPct)
                put("refreshRateHz", fps.refreshRateHz.toDouble())
                put("note", fps.note)
            })
            put("storage", JSONObject().apply {
                put("internalTotalBytes", cap.internalTotalBytes)
                put("internalAvailBytes", cap.internalAvailBytes)
                if (cap.externalTotalBytes != null) put("externalTotalBytes", cap.externalTotalBytes) else put("externalTotalBytes", JSONObject.NULL)
            })
            put("network", JSONObject().apply {
                put("rxBytesPerSec", net.rxBytesPerSec)
                put("txBytesPerSec", net.txBytesPerSec)
                put("type", net.type)
                if (net.signalDbm != null) put("signalDbm", net.signalDbm) else put("signalDbm", JSONObject.NULL)
            })
            put("unavailable", unavailable)
        }
    }

    /**
     * Tra shape phang khop DeviceInfo ben TS (DeviceScreen doc truc tiep
     * cac truong nay, khong doc object screen long nhau). Giu them key
     * cu sdkInt / screen de code cu (neu co) van doc duoc.
     */
    fun collectDeviceInfo(): JSONObject {
        val info = deviceInfo.collect()
        return JSONObject().apply {
            put("model", info.model)
            put("manufacturer", info.manufacturer)
            put("brand", info.brand)
            if (info.socModel != null) put("socModel", info.socModel) else put("socModel", JSONObject.NULL)
            if (info.gpuRenderer != null) put("gpuRenderer", info.gpuRenderer) else put("gpuRenderer", JSONObject.NULL)
            put("androidVersion", info.androidVersion)
            put("apiLevel", info.sdkInt)
            put("sdkInt", info.sdkInt)
            put("securityPatch", info.securityPatch)
            put("buildId", info.buildId)
            put("board", info.board)
            put("hardware", info.hardware)
            put("screenWidthPx", info.screen.widthPx)
            put("screenHeightPx", info.screen.heightPx)
            put("screenDensityDpi", info.screen.densityDpi)
            put("screenDensity", info.screenDensity.toDouble())
            put("hdrCapable", info.screen.hdr)
            put("totalRamBytes", info.totalRamBytes)
            val abi = JSONArray()
            for (a in info.cpuAbi) abi.put(a)
            put("cpuAbi", abi)
            if (info.kernelVersion != null) put("kernelVersion", info.kernelVersion) else put("kernelVersion", JSONObject.NULL)
            put("screen", JSONObject().apply {
                put("widthPx", info.screen.widthPx)
                put("heightPx", info.screen.heightPx)
                put("densityDpi", info.screen.densityDpi)
                put("refreshRateHz", info.screen.refreshRateHz.toDouble())
                put("hdr", info.screen.hdr)
            })
            val sensors = JSONArray()
            for (s in info.sensors) sensors.put(s)
            put("sensors", sensors)
        }
    }

    /** Ghi 1 mau da gop vao Room (1 dong/giay, goi tu service). */
    fun persistSnapshot(snapshot: JSONObject) {
        try {
            val cpuObj = snapshot.optJSONObject("cpu")
            val memObj = snapshot.optJSONObject("memory")
            val thermObj = snapshot.optJSONObject("thermal")
            val battObj = snapshot.optJSONObject("battery")
            val fpsObj = snapshot.optJSONObject("fps")
            val netObj = snapshot.optJSONObject("network")
            val freqArray = cpuObj?.optJSONArray("freqMHz")
            var freqSum = 0.0
            var freqCount = 0
            if (freqArray != null) {
                for (i in 0 until freqArray.length()) {
                    if (!freqArray.isNull(i)) {
                        freqSum += freqArray.optDouble(i)
                        freqCount++
                    }
                }
            }
            val entity = MetricEntity(
                timestampMs = snapshot.optLong("timestampMs", System.currentTimeMillis()),
                cpuLoadPct = if (cpuObj != null && !cpuObj.isNull("loadEstimatePct")) cpuObj.optDouble("loadEstimatePct") else null,
                cpuFreqAvgMHz = if (freqCount > 0) freqSum / freqCount else null,
                ramUsedBytes = memObj?.optLong("usedBytes", 0L) ?: 0L,
                ramTotalBytes = memObj?.optLong("totalBytes", 0L) ?: 0L,
                batteryTempC = if (thermObj != null && !thermObj.isNull("batteryTempC")) thermObj.optDouble("batteryTempC") else null,
                cpuTempC = if (thermObj != null && !thermObj.isNull("cpuTempC")) thermObj.optDouble("cpuTempC") else null,
                batteryLevelPct = battObj?.optInt("levelPct", -1) ?: -1,
                charging = battObj?.optBoolean("charging", false) ?: false,
                fps = fpsObj?.optDouble("fps", 0.0) ?: 0.0,
                rxBytesPerSec = netObj?.optLong("rxBytesPerSec", 0L) ?: 0L,
                txBytesPerSec = netObj?.optLong("txBytesPerSec", 0L) ?: 0L,
            )
            scope.launch {
                try { AppDatabase.get(appContext).metricDao().insert(entity) } catch (_: Exception) { }
            }
        } catch (_: Exception) {
            // Ghi lich su that bai khong duoc lam hong lan lay mau.
        }
    }
}
