package com.mdevice.benchmark.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.mdevice.benchmark.benchmark.BenchmarkRunner
import com.mdevice.benchmark.metrics.StorageMetrics
import com.mdevice.benchmark.overlay.OverlayService
import com.mdevice.benchmark.service.MetricsService
import com.mdevice.benchmark.storage.AppDatabase
import com.mdevice.benchmark.storage.ExportHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * NativeModule ten BenchmarkModule.
 *
 * Event: onMetricsBatch - gui batch JSON moi 750ms, moi batch chua 1 snapshot.
 * Service lay mau 1Hz; module gom snapshot moi nhat va gui theo chu ky
 * 750ms bang handler rieng de khop hop dong, khong gui tung mau le.
 */
class BenchmarkModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "BenchmarkModule"

    private val collector = MetricsCollector(reactContext)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var batching = false
    private var lastSnapshot: JSONObject? = null

    private val batchReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val batch = intent?.getStringExtra(MetricsService.EXTRA_BATCH) ?: return
            try {
                val array = JSONArray(batch)
                if (array.length() > 0) lastSnapshot = array.getJSONObject(0)
            } catch (_: Exception) { }
        }
    }

    private val batchRunnable = object : Runnable {
        override fun run() {
            if (!batching) return
            val snapshot = lastSnapshot ?: collector.collectSnapshot().also { lastSnapshot = it }
            val params = Arguments.createMap()
            params.putString("batch", JSONArray().put(snapshot).toString())
            sendEvent("onMetricsBatch", params)
            handler.postDelayed(this, 750L)
        }
    }

    init {
        LocalBroadcastManager.getInstance(reactContext).registerReceiver(
            batchReceiver,
            IntentFilter(MetricsService.ACTION_METRICS_BATCH),
        )
    }

    private fun sendEvent(eventName: String, params: WritableMap) {
        try {
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, params)
        } catch (_: Exception) { }
    }

    @ReactMethod
    fun startSampling(intervalMs: Double, promise: Promise) {
        try {
            val interval = intervalMs.toLong().coerceIn(500L, 5000L)
            val prefs = reactContext.getSharedPreferences(MetricsService.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(MetricsService.PREF_INTERVAL_MS, interval)
                .putBoolean(MetricsService.PREF_SAMPLING_ENABLED, true)
                .apply()
            val intent = Intent(reactContext, MetricsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                reactContext.startForegroundService(intent)
            } else {
                reactContext.startService(intent)
            }
            if (!batching) {
                batching = true
                handler.post(batchRunnable)
            }
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("START_SAMPLING_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun stopSampling(promise: Promise) {
        try {
            batching = false
            handler.removeCallbacks(batchRunnable)
            val prefs = reactContext.getSharedPreferences(MetricsService.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(MetricsService.PREF_SAMPLING_ENABLED, false).apply()
            reactContext.stopService(Intent(reactContext, MetricsService::class.java))
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("STOP_SAMPLING_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun getSnapshot(promise: Promise) {
        try {
            val snapshot = collector.collectSnapshot()
            promise.resolve(snapshot.toString())
        } catch (e: Exception) {
            promise.reject("SNAPSHOT_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun getDeviceInfo(promise: Promise) {
        try {
            promise.resolve(collector.collectDeviceInfo().toString())
        } catch (e: Exception) {
            promise.reject("DEVICE_INFO_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun getBenchmarkHistory(promise: Promise) {
        scope.launch {
            try {
                val results = AppDatabase.get(reactContext).benchmarkDao().getAll()
                val array = JSONArray()
                // Khop BenchmarkHistoryItem ben TS. DB chi luu timestampMs
                // (luc bat dau chay) nen endedAtMs lay tam bang startedAtMs.
                for (r in results) {
                    array.put(
                        JSONObject().apply {
                            put("id", r.id.toString())
                            put("startedAtMs", r.timestampMs)
                            put("endedAtMs", r.timestampMs)
                            put("totalScore", r.totalScore)
                            put("cpuScore", r.cpuScore)
                            put("memoryScore", r.memScore)
                            put("storageScore", r.storageScore)
                            put("gpuScore", r.gpuScore)
                            if (r.stressDropPct != null) put("stressDropPct", r.stressDropPct)
                            else put("stressDropPct", JSONObject.NULL)
                            put("deviceModel", r.deviceModel)
                        },
                    )
                }
                promise.resolve(array.toString())
            } catch (e: Exception) {
                promise.reject("HISTORY_FAILED", e.message, e)
            }
        }
    }

    @ReactMethod
    fun runStorageSpeedTest(promise: Promise) {
        scope.launch {
            try {
                val result = StorageMetrics(reactContext).measureSpeed(32)
                if (result == null) {
                    promise.reject("STORAGE_TEST_FAILED", "Khong do duoc toc do luu tru tren thiet bi nay.")
                    return@launch
                }
                val json = JSONObject().apply {
                    put("seqWriteMBps", result.seqWriteMBps)
                    put("seqReadMBps", result.seqReadMBps)
                    put("randomReadIOPS", result.randomReadIOPS)
                    put("randomWriteIOPS", result.randomWriteIOPS)
                    put("fileSizeMB", result.fileSizeMB)
                }
                promise.resolve(json.toString())
            } catch (e: Exception) {
                promise.reject("STORAGE_TEST_FAILED", e.message, e)
            }
        }
    }

    @ReactMethod
    fun requestOverlayPermission(promise: Promise) {
        // Chi KIEM TRA, khong tu mo man hinh cai dat: phia JS mo Settings
        // ACTION_MANAGE_OVERLAY_PERMISSION khi can.
        try {
            val canDraw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(reactContext)
            } else true
            promise.resolve(canDraw)
        } catch (e: Exception) {
            promise.reject("OVERLAY_CHECK_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun startOverlay(configJson: String, promise: Promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(reactContext)) {
                promise.reject("OVERLAY_NO_PERMISSION", "Chua cap quyen hien thi tren ung dung khac.")
                return
            }
            val intent = Intent(reactContext, OverlayService::class.java)
                .putExtra(OverlayService.EXTRA_CONFIG, configJson)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                reactContext.startForegroundService(intent)
            } else {
                reactContext.startService(intent)
            }
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("OVERLAY_START_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun stopOverlay(promise: Promise) {
        try {
            reactContext.stopService(Intent(reactContext, OverlayService::class.java))
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("OVERLAY_STOP_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun setAlertThresholds(json: String, promise: Promise) {
        try {
            val obj = JSONObject(json)
            val prefs = reactContext.getSharedPreferences("benchmark_alerts", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            if (obj.has("tempC")) editor.putFloat("tempC", obj.optDouble("tempC", 45.0).toFloat())
            if (obj.has("ramAvailPct")) editor.putFloat("ramAvailPct", obj.optDouble("ramAvailPct", 10.0).toFloat())
            if (obj.has("batteryPct")) editor.putFloat("batteryPct", obj.optDouble("batteryPct", 15.0).toFloat())
            if (obj.has("enabled")) editor.putBoolean("enabled", obj.optBoolean("enabled", true))
            editor.apply()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("THRESHOLDS_FAILED", e.message, e)
        }
    }

    /**
     * Khop hop dong ben TS: runBenchmark(mode) voi mode =
     * single | multi | full | stress. Chu ky cu runBenchmark(gpuFps,
     * gpuJankPct) da bo vi phia JS khong bao gio truyen 2 so nay, va
     * React Native khong ho tro 2 method cung ten khac chu ky.
     * GPU o mode full hien tra 0 (chua gan GpuBenchmarkView), khong bia FPS.
     */
    @ReactMethod
    fun runBenchmark(mode: String, promise: Promise) {
        scope.launch {
            try {
                val runner = BenchmarkRunner(reactContext)
                val result = runner.runByMode(mode)
                promise.resolve(runner.toJson(result).toString())
            } catch (e: Exception) {
                promise.reject("BENCHMARK_FAILED", e.message, e)
            }
        }
    }

    @ReactMethod
    fun runStressTest(durationMin: Double, promise: Promise) {
        scope.launch {
            try {
                val runner = BenchmarkRunner(reactContext)
                val result = runner.runStress(durationMin.toInt())
                promise.resolve(runner.toJson(result).toString())
            } catch (e: Exception) {
                promise.reject("STRESS_FAILED", e.message, e)
            }
        }
    }

    @ReactMethod
    fun exportHistory(promise: Promise) {
        scope.launch {
            try {
                val db = AppDatabase.get(reactContext)
                val metrics = db.metricDao().getLatest(10000)
                val benchmarks = db.benchmarkDao().getAll()
                val csv = ExportHelper.exportMetricsCsv(reactContext, metrics)
                val json = ExportHelper.exportBenchmarksJson(reactContext, benchmarks)
                val out = JSONObject().apply {
                    put("metricsCsvPath", csv.absolutePath)
                    put("benchmarksJsonPath", json.absolutePath)
                }
                promise.resolve(out.toString())
            } catch (e: Exception) {
                promise.reject("EXPORT_FAILED", e.message, e)
            }
        }
    }

    override fun onCatalystInstanceDestroy() {
        batching = false
        handler.removeCallbacks(batchRunnable)
        try {
            LocalBroadcastManager.getInstance(reactContext).unregisterReceiver(batchReceiver)
        } catch (_: Exception) { }
        collector.stop()
        super.onCatalystInstanceDestroy()
    }
}
