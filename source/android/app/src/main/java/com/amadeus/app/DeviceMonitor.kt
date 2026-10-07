package com.amadeus.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs

object DeviceMonitor {

    private fun thermalText(status: Int): String {
        return when (status) {
            PowerManager.THERMAL_STATUS_NONE -> "Normal"
            PowerManager.THERMAL_STATUS_LIGHT -> "Sedikit hangat"
            PowerManager.THERMAL_STATUS_MODERATE -> "Hangat, performa mulai dibatasi"
            PowerManager.THERMAL_STATUS_SEVERE -> "Panas, performa dibatasi"
            PowerManager.THERMAL_STATUS_CRITICAL -> "Sangat panas"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "Darurat panas"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "HP akan mati karena panas"
            else -> "Tidak diketahui"
        }
    }

    private fun availMb(am: ActivityManager): Long {
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.availMem / 1048576L
    }

    fun snapshot(context: Context): List<StatusItem> {
        val items = mutableListOf<StatusItem>()

        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val tempC = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val healthCode = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Baik"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Terlalu panas"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Rusak"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Tegangan berlebih"
            BatteryManager.BATTERY_HEALTH_COLD -> "Terlalu dingin"
            else -> "Tidak diketahui"
        }

        items.add(
            StatusItem(
                "Baterai",
                if (pct < 0) "Tidak diketahui" else "$pct%" + if (charging) " (sedang mengisi)" else "",
                pct < 0 || pct >= 20 || charging
            )
        )

        val tempText = String.format("%.1f", tempC) + " °C"
        val tempDetail = when {
            tempC >= 45f -> "$tempText. Panas, hentikan pemakaian sejenak."
            tempC >= 40f -> "$tempText. Hangat."
            else -> "$tempText. Normal."
        }
        items.add(StatusItem("Suhu baterai", tempDetail, tempC < 40f))
        items.add(StatusItem("Kesehatan baterai", health, healthCode == BatteryManager.BATTERY_HEALTH_GOOD))

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermal = pm.currentThermalStatus
        items.add(StatusItem("Status panas HP", thermalText(thermal), thermal <= PowerManager.THERMAL_STATUS_LIGHT))

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val totalMb = mi.totalMem / 1048576L
        val usedMb = totalMb - mi.availMem / 1048576L
        val ramPct = if (totalMb > 0) (usedMb * 100 / totalMb).toInt() else 0
        items.add(StatusItem("RAM", "Terpakai $usedMb MB dari $totalMb MB ($ramPct%)", ramPct < 85))

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalGb = stat.totalBytes / 1073741824.0
        val freeGb = stat.availableBytes / 1073741824.0
        val usedGb = totalGb - freeGb
        items.add(
            StatusItem(
                "Penyimpanan",
                "Terpakai " + String.format("%.1f", usedGb) + " GB dari " + String.format("%.1f", totalGb) + " GB",
                freeGb / totalGb > 0.1
            )
        )

        return items
    }

    @Suppress("DEPRECATION")
    private fun userPackages(context: Context): List<String> {
        val pm = context.packageManager
        val apps = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            pm.getInstalledApplications(0)
        }
        return apps
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName }
            .map { it.packageName }
    }

    fun boostRam(context: Context): Pair<Int, Long> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val before = availMb(am)
        val targets = userPackages(context)
        for (p in targets) {
            am.killBackgroundProcesses(p)
        }
        Thread.sleep(1000)
        val after = availMb(am)
        return Pair(targets.size, maxOf(0L, after - before))
    }
}
