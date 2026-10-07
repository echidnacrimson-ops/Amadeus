package com.amadeus.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs

data class QuickInfo(
    val ramPct: Int,
    val storagePct: Int,
    val batteryPct: Int,
    val tempC: Float
)

object HomeData {
    fun quick(context: Context): QuickInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val ramPct = if (mi.totalMem > 0) (((mi.totalMem - mi.availMem) * 100) / mi.totalMem).toInt() else 0

        val stat = StatFs(Environment.getDataDirectory().path)
        val storagePct = if (stat.totalBytes > 0) {
            (((stat.totalBytes - stat.availableBytes) * 100) / stat.totalBytes).toInt()
        } else {
            0
        }

        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val tempC = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

        return QuickInfo(ramPct, storagePct, batteryPct, tempC)
    }
}
