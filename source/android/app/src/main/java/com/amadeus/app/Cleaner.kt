package com.amadeus.app

import android.os.Environment
import java.io.File

data class CleanItem(
    val path: String,
    val sizeBytes: Long,
    val reason: String,
    val selectDefault: Boolean
)

object Cleaner {

    private val junkExt = setOf("tmp", "temp", "log", "dmp", "chk")

    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1073741824L -> String.format("%.2f GB", bytes / 1073741824.0)
            bytes >= 1048576L -> String.format("%.1f MB", bytes / 1048576.0)
            bytes >= 1024L -> (bytes / 1024L).toString() + " KB"
            else -> "$bytes B"
        }
    }

    fun scan(): List<CleanItem> {
        val root = Environment.getExternalStorageDirectory()
        val androidDir = File(root, "Android").path
        val results = mutableListOf<CleanItem>()
        val tree = root.walkTopDown().onEnter { it.path != androidDir }

        for (f in tree) {
            if (results.size >= 300) break
            if (!f.isFile) continue
            val ext = f.extension.lowercase()
            val size = f.length()
            when {
                f.parentFile?.name == ".thumbnails" ->
                    results.add(CleanItem(f.path, size, "Cache thumbnail galeri", true))
                ext in junkExt && size > 0 ->
                    results.add(CleanItem(f.path, size, "File sementara atau log", true))
                ext == "apk" ->
                    results.add(CleanItem(f.path, size, "File APK (mungkin sisa pemasangan)", false))
                size >= 100L * 1024L * 1024L ->
                    results.add(CleanItem(f.path, size, "File besar", false))
            }
        }
        return results.sortedByDescending { it.sizeBytes }
    }

    fun delete(paths: List<String>): Pair<Int, Long> {
        val rootPath = Environment.getExternalStorageDirectory().path
        var count = 0
        var bytes = 0L
        for (p in paths) {
            if (!p.startsWith(rootPath)) continue
            val f = File(p)
            val size = f.length()
            if (f.isFile && f.delete()) {
                count++
                bytes += size
            }
        }
        return Pair(count, bytes)
    }
}
