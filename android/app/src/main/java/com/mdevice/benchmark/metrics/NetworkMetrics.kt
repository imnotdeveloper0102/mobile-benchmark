package com.mdevice.benchmark.metrics

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.telephony.TelephonyManager

/**
 * Mang: toc do dua tren delta TrafficStats cua toan thiet bi.
 *
 * TrafficStats.getTotalRxBytes / getTotalTxBytes tra ve tong so byte ke tu
 * khi khoi dong may. Lay hieu giua 2 lan lay mau chia cho khoang thoi gian
 * se ra toc do byte/giay. Luu y day la luu luong cua TOAN thiet bi, khong
 * phai rieng app. Neu muon rieng app thi dung getUidRxBytes(uid).
 * Cuong do tin hieu di dong can quyen READ_PHONE_STATE; thieu quyen thi
 * tra null + ly do o tang snapshot, khong doan gia tri.
 */
class NetworkMetrics(private val context: Context) {

    data class Result(
        val rxBytesPerSec: Long,
        val txBytesPerSec: Long,
        val type: String,
        val signalDbm: Int?,
    )

    private var lastRx: Long = -1L
    private var lastTx: Long = -1L
    private var lastTimeMs: Long = -1L

    fun connectionType(): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return "Khong co mang"
                val caps = cm.getNetworkCapabilities(network) ?: return "Khong ro"
                when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Di dong"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                    else -> "Khac"
                }
            } else {
                @Suppress("DEPRECATION")
                val info = cm.activeNetworkInfo
                @Suppress("DEPRECATION")
                if (info?.isConnected == true) info.typeName ?: "Khong ro" else "Khong co mang"
            }
        } catch (_: Exception) {
            "Khong ro"
        }
    }

    fun signalDbm(): Int? {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val cells = tm.allCellInfo ?: return null
            for (cell in cells) {
                if (!cell.isRegistered) continue
                val dbm = cell.cellSignalStrength.dbm
                if (dbm != Int.MAX_VALUE && dbm != 0) return dbm
            }
            null
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun collect(): Result {
        val now = System.currentTimeMillis()
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        var rxPerSec = 0L
        var txPerSec = 0L
        if (lastRx >= 0 && lastTx >= 0 && lastTimeMs > 0 && now > lastTimeMs) {
            val dtSec = (now - lastTimeMs) / 1000.0
            if (dtSec > 0) {
                rxPerSec = ((rx - lastRx) / dtSec).toLong().coerceAtLeast(0L)
                txPerSec = ((tx - lastTx) / dtSec).toLong().coerceAtLeast(0L)
            }
        }
        lastRx = rx
        lastTx = tx
        lastTimeMs = now
        return Result(
            rxBytesPerSec = rxPerSec,
            txBytesPerSec = txPerSec,
            type = connectionType(),
            signalDbm = signalDbm(),
        )
    }
}
