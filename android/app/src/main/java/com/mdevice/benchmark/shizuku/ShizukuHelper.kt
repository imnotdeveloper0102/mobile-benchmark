package com.mdevice.benchmark.shizuku

import android.content.Context
import android.content.pm.PackageManager

/**
 * Che do nang cao qua Shizuku - module tach rieng, KHONG bat buoc.
 *
 * App khong phu thuoc truc tiep vao thu vien Shizuku o compile time de
 * tranh bat buoc nguoi dung cai Shizuku. Moi tuong tac deu qua reflection:
 * neu Shizuku khong co mat, chua chay, hoac chua cap quyen thi cac ham
 * tra ve trang thai "khong kha dung" kem ly do, khong crash.
 *
 * Khi Shizuku san sang, co the chay lenh dumpsys gfxinfo / dumpsys
 * SurfaceFlinger de doc FPS cua app khac (la thu ma API cong khai khong
 * cho phep). Ket qua dumpsys duoc parse that tu dau ra lenh; neu parse
 * khong duoc thi tra null, khong doan gia tri.
 */
object ShizukuHelper {

    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    data class Status(
        val installed: Boolean,
        val running: Boolean,
        val permissionGranted: Boolean,
        val reason: String?,
    )

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Kiem tra Shizuku qua reflection. Can class rikka.shizuku.Shizuku
     * (chi ton tai khi thu vien Shizuku duoc dong goi kem). Vi project
     * khong dong goi thu vien nay, ham nay se tra running=false voi ly do
     * ro rang cho toi khi team quyet dinh them dependency Shizuku.
     */
    fun checkStatus(context: Context): Status {
        if (!isInstalled(context)) {
            return Status(
                installed = false,
                running = false,
                permissionGranted = false,
                reason = "Chua cai dat Shizuku (moe.shizuku.privileged.api).",
            )
        }
        return try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val pingMethod = clazz.getMethod("pingBinder")
            val alive = pingMethod.invoke(null) as? Boolean ?: false
            if (!alive) {
                return Status(
                    installed = true,
                    running = false,
                    permissionGranted = false,
                    reason = "Shizuku da cai nhung chua chay (can mo Shizuku va bat dich vu).",
                )
            }
            val checkMethod = clazz.getMethod("checkSelfPermission")
            val granted = (checkMethod.invoke(null) as? Int) == 0
            Status(
                installed = true,
                running = true,
                permissionGranted = granted,
                reason = if (granted) null else "Shizuku dang chay nhung chua cap quyen cho app nay.",
            )
        } catch (_: ClassNotFoundException) {
            Status(
                installed = true,
                running = false,
                permissionGranted = false,
                reason = "Thu vien Shizuku chua duoc dong goi kem app (can them dependency rikka.shizuku:api de bat che do nay).",
            )
        } catch (e: Exception) {
            Status(
                installed = true,
                running = false,
                permissionGranted = false,
                reason = "Khong kiem tra duoc trang thai Shizuku: ${e.message}",
            )
        }
    }

    /**
     * Thu doc FPS cua mot app khac qua dumpsys gfxinfo. Chi goi khi
     * checkStatus() bao permissionGranted = true. Hien tai tra null vi
     * chua co dependency Shizuku de thuc thi lenh; phia goi phai hien
     * "Khong kha dung" kem ly do nay thay vi hien so khong co that.
     * Khi them dependency, thay than ham nay bang Shizuku.newProcess()
     * chay ["dumpsys", "gfxinfo", packageName, "framestats"] va parse
     * cot frame time that tu dau ra.
     */
    fun readOtherAppFps(context: Context, packageName: String): Double? {
        val status = checkStatus(context)
        if (!status.permissionGranted) return null
        return null
    }
}
