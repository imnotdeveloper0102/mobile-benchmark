package com.mdevice.benchmark.metrics

import android.content.Context
import android.view.Choreographer
import android.view.Display
import android.view.WindowManager

/**
 * Do FPS cua CHINH APP bang Choreographer.FrameCallback.
 *
 * Gioi han phai noi ro voi nguoi dung: khong co quyen he thong dac biet thi
 * khong do duoc FPS cua app khac. Muon do FPS app khac can Shizuku
 * (doc dumpsys gfxinfo / SurfaceFlinger) - xem ShizukuHelper.
 *
 * Jank = ti le khung co thoi gian giua 2 frame > 2 * 16.7ms (tuong duong
 * bo qua it nhat 1 khung o 60Hz). Cua so tinh toan la 1 giay gan nhat.
 */
class FpsMonitor(private val context: Context) : Choreographer.FrameCallback {

    data class Result(
        val fps: Double,
        val jankPct: Double,
        val refreshRateHz: Float,
        val note: String,
    )

    private var running = false
    private var lastFrameNanos: Long = -1L
    private var frameCountInWindow = 0
    private var jankCountInWindow = 0
    private var windowStartNanos: Long = -1L
    private var lastFps: Double = 0.0
    private var lastJankPct: Double = 0.0

    @Volatile
    var current: Result = Result(0.0, 0.0, refreshRate(), NOTE)
        private set

    fun refreshRate(): Float {
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display: Display? = wm.defaultDisplay
            display?.refreshRate ?: 60f
        } catch (_: Exception) {
            60f
        }
    }

    fun start() {
        if (running) return
        running = true
        lastFrameNanos = -1L
        windowStartNanos = -1L
        frameCountInWindow = 0
        jankCountInWindow = 0
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stop() {
        running = false
        try { Choreographer.getInstance().removeFrameCallback(this) } catch (_: Exception) { }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        if (lastFrameNanos >= 0) {
            val deltaMs = (frameTimeNanos - lastFrameNanos) / 1_000_000.0
            if (windowStartNanos < 0) windowStartNanos = frameTimeNanos
            frameCountInWindow++
            if (deltaMs > 33.4) jankCountInWindow++
            val windowMs = (frameTimeNanos - windowStartNanos) / 1_000_000.0
            if (windowMs >= 1000.0) {
                val fps = frameCountInWindow * 1000.0 / windowMs
                val jankPct = if (frameCountInWindow > 0) {
                    jankCountInWindow * 100.0 / frameCountInWindow
                } else 0.0
                lastFps = fps
                lastJankPct = jankPct
                current = Result(fps, jankPct, refreshRate(), NOTE)
                frameCountInWindow = 0
                jankCountInWindow = 0
                windowStartNanos = frameTimeNanos
            }
        }
        lastFrameNanos = frameTimeNanos
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun snapshot(): Result = current.copy(fps = lastFps, jankPct = lastJankPct)

    companion object {
        const val NOTE = "Chi do FPS cua chinh app"
    }
}
