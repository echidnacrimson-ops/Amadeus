package com.amadeus.app

import android.net.Uri

data class LinkVerdict(
    val level: String,
    val reasons: List<String>
)

object LinkChecker {

    private val shorteners = setOf(
        "bit.ly", "tinyurl.com", "s.id", "cutt.ly", "t.ly",
        "is.gd", "rebrand.ly", "shorturl.at", "goo.gl", "ow.ly"
    )

    private val badTld = setOf(
        "xyz", "top", "click", "zip", "mov", "tk", "ml", "ga",
        "cf", "gq", "icu", "buzz", "cyou", "monster", "rest", "loan"
    )

    private val gamblingWords = setOf("slot", "gacor", "togel", "judi", "casino", "maxwin")

    private val phishWords = setOf(
        "login", "verify", "verifikasi", "secure", "akun",
        "update", "hadiah", "undian", "klaim", "bonus"
    )

    private val brands = mapOf(
        "whatsapp" to setOf("whatsapp.com", "whatsapp.net", "wa.me"),
        "facebook" to setOf("facebook.com", "fb.com", "fb.me"),
        "instagram" to setOf("instagram.com"),
        "dana" to setOf("dana.id"),
        "shopee" to setOf(
            "shopee.co.id", "shopee.com", "shopee.sg", "shopee.com.my",
            "shopee.ph", "shopee.vn", "shopee.co.th", "shp.ee"
        ),
        "tokopedia" to setOf("tokopedia.com", "tokopedia.link"),
        "gopay" to setOf("gopay.co.id", "gojek.com"),
        "telegram" to setOf("telegram.org", "telegram.me", "t.me"),
        "netflix" to setOf("netflix.com"),
        "paypal" to setOf("paypal.com", "paypal.me"),
        "tiktok" to setOf("tiktok.com"),
        "google" to setOf("google.com", "google.co.id")
    )

    private val ipRegex = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")

    private fun baseDomain(host: String): String {
        val parts = host.split(".")
        if (parts.size <= 2) return host
        val secondLevel = setOf("co", "com", "go", "or", "ac", "net", "web", "sch", "my")
        val last = parts.last()
        val prev = parts[parts.size - 2]
        return if (last.length == 2 && prev in secondLevel) {
            parts.takeLast(3).joinToString(".")
        } else {
            parts.takeLast(2).joinToString(".")
        }
    }

    fun check(input: String): LinkVerdict {
        val raw = input.trim()
        if (raw.isEmpty()) {
            return LinkVerdict("KOSONG", listOf("Tempel alamat tautan dulu."))
        }

        val hadScheme = raw.contains("://")
        val uri = Uri.parse(if (hadScheme) raw else "http://$raw")
        val host = uri.host?.lowercase()
        if (host.isNullOrEmpty()) {
            return LinkVerdict("TIDAK VALID", listOf("Alamat tautan tidak dapat dibaca."))
        }

        var score = 0
        val reasons = mutableListOf<String>()
        val path = uri.path?.lowercase() ?: ""
        val tokens = host.split('.', '-')

        if (hadScheme && uri.scheme == "http") {
            score += 2
            reasons.add("Tidak memakai HTTPS, data yang dikirim bisa disadap")
        }
        if (ipRegex.matches(host)) {
            score += 4
            reasons.add("Alamat berupa angka IP, bukan nama situs")
        }
        if (host.contains("xn--")) {
            score += 4
            reasons.add("Memakai karakter yang menyerupai huruf lain (punycode)")
        }
        if (uri.userInfo != null) {
            score += 4
            reasons.add("Ada tanda @ di alamat, trik untuk menyamarkan tujuan asli")
        }
        if (host in shorteners) {
            score += 2
            reasons.add("Tautan pendek, tujuan aslinya tersembunyi")
        }
        val tld = host.substringAfterLast('.')
        if (tld in badTld) {
            score += 3
            reasons.add("Domain .$tld sering dipakai situs penipuan")
        }
        if (host.split('.').size >= 5) {
            score += 2
            reasons.add("Terlalu banyak subdomain")
        }
        if (path.endsWith(".apk")) {
            score += 4
            reasons.add("Mengunduh file APK langsung")
        }
        if (gamblingWords.any { host.contains(it) }) {
            score += 3
            reasons.add("Alamat terkait judi online")
        }
        if (tokens.any { it in phishWords }) {
            score += 2
            reasons.add("Memakai kata yang sering dipakai penipuan (login, verifikasi, hadiah, dsb)")
        }
        val base = baseDomain(host)
        for ((brand, official) in brands) {
            if (brand in tokens && base !in official) {
                score += 4
                reasons.add("Memakai nama \"$brand\" tetapi bukan situs resminya")
                break
            }
        }

        val level = when {
            score >= 6 -> "SANGAT CURIGA"
            score >= 3 -> "CURIGA"
            else -> "TIDAK ADA POLA MENCURIGAKAN"
        }
        if (reasons.isEmpty()) {
            reasons.add("Tidak ada pola penipuan yang dikenali. Ini bukan jaminan situs aman.")
        }
        return LinkVerdict(level, reasons)
    }
}
