package com.mdevice.benchmark.benchmark

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin

/**
 * Bai kiem tra render don gian bang OpenGL ES 2.0.
 *
 * Ve mot luoi tam giac xoay lien tuc (khoang 2000 tam giac moi khung) trong
 * 5 giay, dem so khung ve duoc de tinh FPS va ti le jank (khung > 33.4ms).
 * Day la bai do suc manh render thuc cua GPU tren thiet bi, khong phai
 * mo phong bang so lieu co dinh.
 */
class GpuBenchmarkView(context: Context) : GLSurfaceView(context) {

    data class GpuResult(val fps: Double, val jankPct: Double, val frames: Int, val durationMs: Long)

    @Volatile
    var result: GpuResult? = null
        private set

    private val renderer = BenchmarkRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun runTest(durationMs: Long = 5000L, onDone: (GpuResult) -> Unit) {
        renderer.start(durationMs) { r ->
            result = r
            post { onDone(r) }
        }
    }

    private class BenchmarkRenderer : Renderer {
        private var program = 0
        private var vertexBuffer: FloatBuffer? = null
        private var startNanos: Long = -1L
        private var durationMs: Long = 5000L
        private var frameCount = 0
        private var jankCount = 0
        private var lastFrameNanos: Long = -1L
        private var callback: ((GpuResult) -> Unit)? = null
        private var finished = false
        private var angle = 0f

        fun start(durationMs: Long, callback: (GpuResult) -> Unit) {
            this.durationMs = durationMs
            this.callback = callback
            startNanos = -1L
            frameCount = 0
            jankCount = 0
            lastFrameNanos = -1L
            finished = false
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0.05f, 0.07f, 0.10f, 1.0f)
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
            program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            // Luoi tam giac: 2000 tam giac ~ 6000 dinh.
            val vertices = FloatArray(2000 * 3 * 2)
            var idx = 0
            var x = -1f
            var y = 1f
            for (i in 0 until 2000) {
                val size = 0.04f
                vertices[idx++] = x
                vertices[idx++] = y
                vertices[idx++] = x + size
                vertices[idx++] = y
                vertices[idx++] = x + size / 2f
                vertices[idx++] = y - size
                x += 0.06f
                if (x > 1f) {
                    x = -1f
                    y -= 0.08f
                    if (y < -1f) y = 1f
                }
            }
            val buffer = ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            buffer.put(vertices).position(0)
            vertexBuffer = buffer
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
        }

        override fun onDrawFrame(gl: GL10?) {
            val now = System.nanoTime()
            if (startNanos < 0) startNanos = now
            if (lastFrameNanos >= 0) {
                val deltaMs = (now - lastFrameNanos) / 1_000_000.0
                if (deltaMs > 33.4) jankCount++
            }
            lastFrameNanos = now
            frameCount++
            angle += 0.02f
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(program)
            val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            val colorHandle = GLES20.glGetUniformLocation(program, "uColor")
            val angleHandle = GLES20.glGetUniformLocation(program, "uAngle")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
            GLES20.glUniform4f(colorHandle, 0.2f, 0.6f, 1.0f, 1.0f)
            GLES20.glUniform1f(angleHandle, angle)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 2000 * 3)
            GLES20.glDisableVertexAttribArray(positionHandle)
            val elapsedMs = (now - startNanos) / 1_000_000.0
            if (!finished && elapsedMs >= durationMs && callback != null) {
                finished = true
                val fps = if (elapsedMs > 0) frameCount * 1000.0 / elapsedMs else 0.0
                val jankPct = if (frameCount > 0) jankCount * 100.0 / frameCount else 0.0
                val r = GpuResult(fps, jankPct, frameCount, elapsedMs.toLong())
                callback?.invoke(r)
            }
        }

        private fun loadShader(type: Int, code: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, code)
            GLES20.glCompileShader(shader)
            return shader
        }

        companion object {
            // Xoay dinh quanh tam trong vertex shader bang ma tran xoay 2D.
            const val VERTEX_SHADER = """
                attribute vec2 aPosition;
                uniform float uAngle;
                void main() {
                    float c = cos(uAngle);
                    float s = sin(uAngle);
                    vec2 rotated = vec2(
                        aPosition.x * c - aPosition.y * s,
                        aPosition.x * s + aPosition.y * c
                    );
                    gl_Position = vec4(rotated * 0.9, 0.0, 1.0);
                }
            """
            const val FRAGMENT_SHADER = """
                precision mediump float;
                uniform vec4 uColor;
                void main() {
                    gl_FragColor = uColor;
                }
            """
        }
    }
}
