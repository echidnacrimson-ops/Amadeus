package com.amadeus.app

import android.content.Context

data class ChatMessage(val fromUser: Boolean, val text: String)

enum class ChatAction { NONE, BOOST_RAM }

data class Reply(
    val text: String,
    val goTo: Int = -1,
    val action: ChatAction = ChatAction.NONE
)

interface KurisuBrain {
    suspend fun reply(context: Context, input: String): Reply
}

object RuleBrain : KurisuBrain {

    private val urlRegex = Regex("(https?://\\S+|www\\.\\S+)", RegexOption.IGNORE_CASE)
    private val splitter = Regex("[^a-z0-9]+")

    private fun pick(vararg options: String): String = options.random()

    override suspend fun reply(context: Context, input: String): Reply {
        val t = input.lowercase().trim()
        val words = t.split(splitter).toSet()
        fun has(vararg keys: String): Boolean = keys.any { t.contains(it) }
        fun hasWord(vararg keys: String): Boolean = keys.any { it in words }

        val q = HomeData.quick(context)
        val temp = String.format("%.1f", q.tempC)

        val url = urlRegex.find(input)
        if (url != null) {
            val v = LinkChecker.check(url.value)
            val intro = when (v.level) {
                "SANGAT CURIGA" -> "Jangan diklik dulu! Tautan ini punya banyak ciri penipuan."
                "CURIGA" -> "Hmm, tautan ini agak mencurigakan. Hati-hati."
                "TIDAK VALID" -> "Alamatnya tidak bisa kubaca. Coba salin ulang tautannya."
                else -> "Aku tidak menemukan pola penipuan yang jelas. Tapi ini bukan jaminan, tetap waspada."
            }
            return Reply(intro + "\n" + v.reasons.joinToString("\n") { "• $it" })
        }

        if (has("bersihkan ram", "boost", "percepat", "kosongkan ram")) {
            return Reply(
                pick(
                    "Baik, kututup aplikasi latar belakang yang bisa ditutup. Tunggu sebentar...",
                    "Oke, mulai membersihkan RAM. Jangan dipencet-pencet dulu."
                ),
                action = ChatAction.BOOST_RAM
            )
        }

        if (has("virus", "malware", "antivirus", "pindai", "scan")) {
            return Reply(
                "Kubukakan halaman Antivirus. Tekan tombol pindai di sana, dan jangan pindah tab selama prosesnya.",
                goTo = 4
            )
        }

        if (has("izin", "keamanan", "mencurigakan")) {
            return Reply(
                "Kubukakan halaman Keamanan. Di sana kamu bisa melihat status HP dan skor risiko tiap aplikasi.",
                goTo = 1
            )
        }

        if (has("sampah", "bersihkan file", "hapus file")) {
            return Reply(
                "Kubukakan halaman Pembersih. Centang sendiri file yang mau dihapus, aku tidak mau disalahkan kalau salah hapus.",
                goTo = 3
            )
        }

        if (has("suhu", "panas", "hangat")) {
            val comment = when {
                q.tempC >= 45f -> "Panas sekali. Hentikan game, lepas casing, dan jangan mengisi daya dulu."
                q.tempC >= 40f -> "Agak hangat. Kurangi pemakaian berat sebentar."
                else -> "Normal. Tidak ada yang perlu dikhawatirkan."
            }
            return Reply("Suhu baterai sekarang $temp °C. $comment\n(Suhu prosesor tidak bisa kubaca tanpa akses root.)")
        }

        if (has("baterai", "battery")) {
            val comment = when {
                q.batteryPct < 20 -> "Sudah tipis, cepat colok charger."
                q.batteryPct < 50 -> "Masih aman, tapi jangan sampai kehabisan."
                else -> "Masih lega."
            }
            return Reply("Baterai ${q.batteryPct}%. $comment")
        }

        if (hasWord("ram", "lag", "lemot") || has("memori")) {
            val comment = if (q.ramPct >= 80) {
                "Cukup penuh. Ketik \"bersihkan ram\" kalau mau kubantu."
            } else {
                "Masih lega, belum perlu dibersihkan."
            }
            return Reply("RAM terpakai ${q.ramPct}%. $comment")
        }

        if (has("penyimpanan", "storage")) {
            val comment = if (q.storagePct >= 85) {
                "Hampir penuh. Ketik \"sampah\" supaya kubuka halaman Pembersih."
            } else {
                "Masih longgar."
            }
            return Reply("Penyimpanan terpakai ${q.storagePct}%. $comment")
        }

        if (has("status", "kondisi", "cek hp")) {
            val notes = mutableListOf<String>()
            if (q.ramPct >= 85) notes.add("RAM hampir penuh")
            if (q.storagePct >= 90) notes.add("penyimpanan hampir penuh")
            if (q.batteryPct < 20) notes.add("baterai tipis")
            if (q.tempC >= 40f) notes.add("suhu baterai tinggi")
            val verdict = if (notes.isEmpty()) {
                "Semuanya terlihat sehat. Jangan senang dulu, aku tetap memantau."
            } else {
                "Perhatikan: " + notes.joinToString(", ") + "."
            }
            return Reply(
                "Kondisi HP sekarang:\n" +
                    "• RAM terpakai ${q.ramPct}%\n" +
                    "• Penyimpanan terpakai ${q.storagePct}%\n" +
                    "• Baterai ${q.batteryPct}%\n" +
                    "• Suhu baterai $temp °C\n" +
                    verdict
            )
        }

        if (has("game", "gaming", "booster")) {
            return Reply("Fitur gaming booster masih dalam pengembangan. Sabar sedikit, ilmuwan tidak bisa dipaksa buru-buru.")
        }

        if (has("bantuan", "help", "bisa apa", "fitur")) {
            return Reply(
                "Yang bisa kulakukan:\n" +
                    "• Cek kondisi: \"status\", \"suhu\", \"baterai\", \"ram\", \"penyimpanan\"\n" +
                    "• Aksi: \"bersihkan ram\", \"sampah\" (buka Pembersih), \"pindai virus\" (buka Antivirus), \"izin aplikasi\" (buka Keamanan)\n" +
                    "• Tempel sebuah link, nanti kuperiksa polanya\n" +
                    "Aku asisten berbasis aturan, jadi belum bisa mengobrol bebas."
            )
        }

        if (has("siapa", "kurisu", "amadeus", "namamu")) {
            return Reply(
                "Aku Kurisu, asisten di aplikasi Amadeus. Kepribadianku terinspirasi dari Makise Kurisu, peneliti jenius yang suka sains. " +
                    "Aku bukan AI sungguhan, jawabanku berdasarkan aturan. Ketik \"bantuan\" untuk tahu yang bisa kulakukan."
            )
        }

        if (has("terima kasih", "makasih", "thanks")) {
            return Reply(
                pick(
                    "B-bukan berarti aku senang dibilang terima kasih. Tapi sama-sama.",
                    "Hmph, itu memang tugasku. Sama-sama."
                )
            )
        }

        if (hasWord("halo", "hai", "hi") || has("selamat")) {
            return Reply(
                pick(
                    "Hmph, akhirnya kamu menyapa. Ada yang perlu dicek di HP-mu?",
                    "Halo. Aku siap membantu, jadi cepat bilang apa yang kamu butuhkan."
                )
            )
        }

        return Reply("Hmm, aku belum paham maksudmu. Ketik \"bantuan\" untuk melihat apa yang bisa kulakukan.")
    }
}
