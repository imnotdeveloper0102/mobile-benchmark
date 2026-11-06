package com.mdevice.benchmark.metrics

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.opengl.EGL14
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Thong tin thiet bi (chi doc, khong can quyen dac biet).
 * GPU renderer doc qua EGL14: tao EGL context tam, doc GL_RENDERER, giai
 * phong ngay. Neu that bai (thiet bi khong ho tro EGL) thi tra null.
 */
class DeviceInfoCollector(private val context: Context) {

    data class ScreenInfo(
        val widthPx: Int,
        val heightPx: Int,
        val densityDpi: Int,
        val refreshRateHz: Float,
        val hdr: Boolean,
    )

    data class Result(
        val model: String,
        val manufacturer: String,
        val brand: String,
        val socModel: String?,
        val androidVersion: String,
        val sdkInt: Int,
        val securityPatch: String,
        val buildId: String,
        val board: String,
        val hardware: String,
        val gpuRenderer: String?,
        val screen: ScreenInfo,
        val screenDensity: Float,
        val totalRamBytes: Long,
        val cpuAbi: List<String>,
        val kernelVersion: String?,
        val sensors: List<String>,
    )

    fun gpuRenderer(): String? {
        var display: android.opengl.EGLDisplay? = null
        var surface: android.opengl.EGLSurface? = null
        var eglContext: android.opengl.EGLContext? = null
        return try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return null
            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) return null
            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0)) return null
            if (numConfigs[0] <= 0 || configs[0] == null) return null
            val surfaceAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
            surface = EGL14.eglCreatePbufferSurface(display, configs[0], surfaceAttribs, 0)
            if (surface == EGL14.EGL_NO_SURFACE) return null
            val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            eglContext = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) return null
            if (!EGL14.eglMakeCurrent(display, surface, surface, eglContext)) return null
            android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER)
        } catch (_: Exception) {
            null
        } finally {
            try {
                if (display != null && display != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(
                        display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT,
                    )
                    if (surface != null && surface != EGL14.EGL_NO_SURFACE) {
                        EGL14.eglDestroySurface(display, surface)
                    }
                    if (eglContext != null && eglContext != EGL14.EGL_NO_CONTEXT) {
                        EGL14.eglDestroyContext(display, eglContext)
                    }
                    EGL14.eglTerminate(display)
                }
            } catch (_: Exception) { }
        }
    }

    fun collect(): Result {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(metrics)
        @Suppress("DEPRECATION")
        val refresh = wm.defaultDisplay.refreshRate
        val hdr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            val display = wm.defaultDisplay
            display.hdrCapabilities != null
        } else false
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = try {
            sensorManager.getSensorList(Sensor.TYPE_ALL).map { it.name }.distinct().sorted()
        } catch (_: Exception) {
            emptyList()
        }
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try { Build.SOC_MODEL } catch (_: Exception) { null }
        } else null
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        val totalRam = try {
            activityManager.getMemoryInfo(memInfo)
            memInfo.totalMem
        } catch (_: Exception) { 0L }
        val kernel = try {
            java.io.BufferedReader(java.io.FileReader("/proc/version")).use { it.readLine() }
        } catch (_: Exception) { null }
        return Result(
            model = Build.MODEL ?: "Khong ro",
            manufacturer = Build.MANUFACTURER ?: "Khong ro",
            brand = Build.BRAND ?: "Khong ro",
            socModel = soc,
            androidVersion = Build.VERSION.RELEASE ?: "Khong ro",
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH ?: "Khong ro",
            buildId = Build.ID ?: "Khong ro",
            board = Build.BOARD ?: "Khong ro",
            hardware = Build.HARDWARE ?: "Khong ro",
            gpuRenderer = gpuRenderer(),
            screen = ScreenInfo(
                widthPx = metrics.widthPixels,
                heightPx = metrics.heightPixels,
                densityDpi = metrics.densityDpi,
                refreshRateHz = refresh,
                hdr = hdr,
            ),
            screenDensity = metrics.density,
            totalRamBytes = totalRam,
            cpuAbi = Build.SUPPORTED_ABIS?.toList() ?: emptyList(),
            kernelVersion = kernel,
            sensors = sensors,
        )
    }
}
