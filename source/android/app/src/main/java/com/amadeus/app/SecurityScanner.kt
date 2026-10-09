package com.amadeus.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class AppRisk(
    val name: String,
    val packageName: String,
    val score: Int,
    val level: String,
    val reasons: List<String>,
    val installer: String
)

data class StatusItem(
    val title: String,
    val detail: String,
    val ok: Boolean
)

object SecurityScanner {

    private val dangerousPermissions = mapOf(
        "android.permission.READ_SMS" to Pair(3, "Meminta izin membaca SMS"),
        "android.permission.RECEIVE_SMS" to Pair(3, "Meminta izin menerima SMS"),
        "android.permission.SEND_SMS" to Pair(3, "Meminta izin mengirim SMS"),
        "android.permission.READ_CALL_LOG" to Pair(2, "Meminta izin membaca riwayat panggilan"),
        "android.permission.RECORD_AUDIO" to Pair(2, "Meminta izin merekam suara"),
        "android.permission.CAMERA" to Pair(1, "Meminta izin kamera"),
        "android.permission.ACCESS_FINE_LOCATION" to Pair(1, "Meminta izin lokasi tepat"),
        "android.permission.ACCESS_BACKGROUND_LOCATION" to Pair(2, "Meminta izin lokasi di latar belakang"),
        "android.permission.READ_CONTACTS" to Pair(1, "Meminta izin membaca kontak"),
        "android.permission.SYSTEM_ALERT_WINDOW" to Pair(2, "Meminta izin tampil di atas aplikasi lain"),
        "android.permission.REQUEST_INSTALL_PACKAGES" to Pair(2, "Meminta izin memasang aplikasi lain"),
        "android.permission.MANAGE_EXTERNAL_STORAGE" to Pair(2, "Meminta akses ke semua file")
    )

    val trustedInstallers = setOf(
        "com.android.vending",
        "com.sec.android.app.samsungapps",
        "com.huawei.appmarket",
        "com.xiaomi.mipicks",
        "com.xiaomi.market",
        "com.heytap.market",
        "com.oppo.market",
        "com.vivo.appstore",
        "com.amazon.venezia"
    )

    @Suppress("DEPRECATION")
    private fun installedPackages(pm: PackageManager): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }
    }

    private fun installerOf(pm: PackageManager, packageName: String): String? {
        return try {
            pm.getInstallSourceInfo(packageName).installingPackageName
        } catch (e: Exception) {
            null
        }
    }

    private fun accessibilityPackages(context: Context): Set<String> {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .mapNotNull { it.resolveInfo?.serviceInfo?.packageName }
            .toSet()
    }

    private fun adminPackages(context: Context): Set<String> {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.activeAdmins?.map { it.packageName }?.toSet() ?: emptySet()
    }

    private fun levelOf(score: Int): String {
        return when {
            score >= 8 -> "TINGGI"
            score >= 5 -> "SEDANG"
            else -> "RENDAH"
        }
    }

    fun scanApps(context: Context): List<AppRisk> {
        val pm = context.packageManager
        val accessibility = accessibilityPackages(context)
        val admins = adminPackages(context)
        val results = mutableListOf<AppRisk>()

        for (pkg in installedPackages(pm)) {
            val info = pkg.applicationInfo ?: continue
            if ((info.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
            if (pkg.packageName == context.packageName) continue

            val reasons = mutableListOf<String>()
            var permScore = 0

            val requested = pkg.requestedPermissions?.toSet() ?: emptySet()
            for ((perm, rule) in dangerousPermissions) {
                if (perm in requested) {
                    permScore += rule.first
                    reasons.add(rule.second)
                }
            }
            var score = minOf(permScore, 5)

            val installer = installerOf(pm, pkg.packageName)
            val sideloaded = installer == null || installer !in trustedInstallers
            if (sideloaded) {
                score += 2
                reasons.add("Dipasang dari luar toko aplikasi resmi")
                val hasSms = requested.any { it.endsWith("_SMS") }
                if (hasSms) {
                    score += 3
                    reasons.add("Kombinasi berisiko: akses SMS dari sumber tak dikenal")
                }
            }

            if (pkg.packageName in accessibility) {
                score += 5
                reasons.add("Layanan Aksesibilitas aktif (bisa membaca isi layar)")
            }

            if (pkg.packageName in admins) {
                score += 5
                reasons.add("Menjadi administrator perangkat")
            }

            results.add(
                AppRisk(
                    name = pm.getApplicationLabel(info).toString(),
                    packageName = pkg.packageName,
                    score = score,
                    level = levelOf(score),
                    reasons = reasons,
                    installer = installer ?: "tidak diketahui"
                )
            )
        }
        return results.sortedByDescending { it.score }
    }

    private fun looksRooted(): Boolean {
        val paths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/app/Superuser.apk",
            "/data/local/bin/su",
            "/data/local/xbin/su"
        )
        val hasSu = paths.any { File(it).exists() }
        val testKeys = Build.TAGS?.contains("test-keys") == true
        return hasSu || testKeys
    }

    fun deviceStatus(context: Context): List<StatusItem> {
        val items = mutableListOf<StatusItem>()

        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val locked = km.isDeviceSecure
        items.add(
            StatusItem(
                "Kunci layar",
                if (locked) "Aktif (PIN, pola, atau sandi)" else "Belum diatur. Segera atur kunci layar.",
                locked
            )
        )

        val usbDebug = Settings.Global.getInt(
            context.contentResolver, Settings.Global.ADB_ENABLED, 0
        ) == 1
        items.add(
            StatusItem(
                "USB debugging",
                if (usbDebug) "Menyala. Matikan jika tidak dipakai." else "Mati",
                !usbDebug
            )
        )

        val rooted = looksRooted()
        items.add(
            StatusItem(
                "Indikasi root",
                if (rooted) "Ada tanda-tanda HP di-root" else "Tidak terdeteksi",
                !rooted
            )
        )

        val patch = Build.VERSION.SECURITY_PATCH
        val days = try {
            ChronoUnit.DAYS.between(LocalDate.parse(patch), LocalDate.now())
        } catch (e: Exception) {
            -1L
        }
        val patchOk = days < 0 || days <= 180
        items.add(
            StatusItem(
                "Tambalan keamanan",
                if (days < 0) patch else "$patch ($days hari lalu)" + if (patchOk) "" else ". Sebaiknya perbarui sistem.",
                patchOk
            )
        )

        items.add(
            StatusItem(
                "Versi Android",
                "Android ${Build.VERSION.RELEASE}",
                true
            )
        )

        return items
    }
}
