package com.mdevice.benchmark.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject

/**
 * View overlay nho gon: FPS / CPU / RAM / nhiet.
 * Keo tha bang mot ngon tay (ACTION_MOVE doi toa do WindowManager.LayoutParams).
 * Cau hinh truyen vao bang JSON: showFps, showCpu, showRam, showTemp,
 * alpha (0.3-1.0), size ("small" | "medium" | "large").
 */
class OverlayView(context: Context) : LinearLayout(context) {

    private val fpsText = TextView(context)
    private val cpuText = TextView(context)
    private val ramText = TextView(context)
    private val tempText = TextView(context)

    var showFps = true
    var showCpu = true
    var showRam = true
    var showTemp = true

    private var dragStartX = 0f
    private var dragStartY = 0f
    private var paramsStartX = 0
    private var paramsStartY = 0
    var onDrag: ((x: Int, y: Int) -> Unit)? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.argb(200, 10, 12, 16))
        setPadding(dp(10), dp(8), dp(10), dp(8))
        for (view in listOf(fpsText, cpuText, ramText, tempText)) {
            view.setTextColor(Color.WHITE)
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            addView(view)
        }
        applyConfig(JSONObject())
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    fun applyConfig(config: JSONObject) {
        showFps = config.optBoolean("showFps", true)
        showCpu = config.optBoolean("showCpu", true)
        showRam = config.optBoolean("showRam", true)
        showTemp = config.optBoolean("showTemp", true)
        val alpha = config.optDouble("alpha", 0.85).toFloat().coerceIn(0.3f, 1.0f)
        setAlpha(alpha)
        val size = config.optString("size", "medium")
        val textSize = when (size) {
            "small" -> 11f
            "large" -> 14f
            else -> 12f
        }
        for (view in listOf(fpsText, cpuText, ramText, tempText)) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        }
        refreshVisibility()
    }

    private fun refreshVisibility() {
        fpsText.visibility = if (showFps) View.VISIBLE else View.GONE
        cpuText.visibility = if (showCpu) View.VISIBLE else View.GONE
        ramText.visibility = if (showRam) View.VISIBLE else View.GONE
        tempText.visibility = if (showTemp) View.VISIBLE else View.GONE
    }

    fun update(snapshot: JSONObject) {
        val cpu = snapshot.optJSONObject("cpu")
        val memory = snapshot.optJSONObject("memory")
        val thermal = snapshot.optJSONObject("thermal")
        val fps = snapshot.optJSONObject("fps")
        if (showFps && fps != null) {
            fpsText.text = "FPS: %.0f".format(fps.optDouble("fps", 0.0))
        }
        if (showCpu && cpu != null) {
            val load = cpu.optDouble("loadEstimatePct", Double.NaN)
            cpuText.text = if (load.isNaN()) "CPU: khong kha dung" else "CPU: %.0f%%".format(load)
        }
        if (showRam && memory != null) {
            val usedGb = memory.optLong("usedBytes", 0L) / 1_073_741_824.0
            val totalGb = memory.optLong("totalBytes", 0L) / 1_073_741_824.0
            ramText.text = "RAM: %.1f/%.1f GB".format(usedGb, totalGb)
        }
        if (showTemp && thermal != null) {
            val temp = thermal.optDouble("batteryTempC", Double.NaN)
            tempText.text = if (temp.isNaN()) "Nhiet: khong kha dung" else "Nhiet: %.1f°C".format(temp)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = event.rawX
                dragStartY = event.rawY
                val params = layoutParams as? WindowManager.LayoutParams
                paramsStartX = params?.x ?: 0
                paramsStartY = params?.y ?: 0
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - dragStartX).toInt()
                val dy = (event.rawY - dragStartY).toInt()
                onDrag?.invoke(paramsStartX + dx, paramsStartY + dy)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    companion object {
        fun layoutParams(): WindowManager.LayoutParams {
            return WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 200
            }
        }
    }
}
