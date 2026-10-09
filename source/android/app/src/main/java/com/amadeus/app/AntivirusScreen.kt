package com.amadeus.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.launch

private val AvRed = Color(0xFFC62828)
private val AvOrange = Color(0xFFEF6C00)
private val AvGray = Color(0xFF616161)
private val AvGreen = Color(0xFF2E7D32)

private fun findingColor(level: String): Color {
    return when (level) {
        "MALWARE", "CURIGA" -> AvRed
        "PERIKSA" -> AvOrange
        else -> AvGray
    }
}

private fun linkColor(level: String): Color {
    return when (level) {
        "SANGAT CURIGA" -> AvRed
        "CURIGA" -> AvOrange
        "TIDAK ADA POLA MENCURIGAKAN" -> AvGreen
        else -> AvGray
    }
}

@Composable
fun AntivirusScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var report by remember { mutableStateOf<ScanReport?>(null) }
    var message by remember { mutableStateOf("") }
    var confirmFile by remember { mutableStateOf<Finding?>(null) }
    var link by remember { mutableStateOf("") }
    var verdict by remember { mutableStateOf<LinkVerdict?>(null) }

    val toDelete = confirmFile
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmFile = null },
            title = { Text("Hapus file ini?") },
            text = { Text(toDelete.name + "\n" + toDelete.path + "\n\nFile dihapus permanen.") },
            confirmButton = {
                Button(onClick = {
                    val ok = File(toDelete.path).delete()
                    message = if (ok) "File dihapus." else "Gagal menghapus file."
                    if (ok) {
                        val rep = report
                        if (rep != null) {
                            report = rep.copy(findings = rep.findings.filter { it.path != toDelete.path })
                        }
                    }
                    confirmFile = null
                }) { Text("Hapus") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmFile = null }) { Text("Batal") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Antivirus", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Text(
                "Pemindai offline: mencari file yang menyamar, file APK berbahaya, dan aplikasi mencurigakan. " +
                    "Tanpa database virus, jadi hasilnya berupa dugaan dari pola dan bukan jaminan 100% bebas virus. " +
                    "Gunakan bersama antivirus bawaan HP.",
                fontSize = 12.sp
            )
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        scanning = true
                        report = null
                        message = ""
                        progress = "Memulai..."
                        val result = MalwareScanner.scanAll(context, onProgress = { progress = it })
                        report = result
                        scanning = false
                        progress = ""
                    }
                },
                enabled = !scanning
            ) {
                Text(if (scanning) "Memindai..." else "Pindai virus dan malware")
            }
        }
        if (scanning && progress.isNotEmpty()) {
            item { Text(progress) }
        }
        if (message.isNotEmpty()) {
            item { Text(message) }
        }
        val r = report
        if (r != null) {
            item {
                Text(
                    "File diperiksa: ${r.filesChecked}. Aplikasi diperiksa: ${r.appsChecked}. Temuan: ${r.findings.size}.",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (r.note.isNotEmpty()) {
                item { Text(r.note, fontSize = 12.sp) }
            }
            if (r.findings.isEmpty()) {
                item { Text("Tidak ada ancaman terdeteksi oleh pemeriksaan ini.") }
            }
            items(r.findings) { f ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(f.name, fontWeight = FontWeight.Bold)
                        Text(f.level, color = findingColor(f.level), fontWeight = FontWeight.Bold)
                        Text(f.reasons.joinToString("\n") { "• $it" }, fontSize = 12.sp)
                        Text(if (f.packageName.isNotEmpty()) f.packageName else f.path, fontSize = 10.sp)
                        if (f.packageName.isNotEmpty()) {
                            OutlinedButton(onClick = {
                                val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:" + f.packageName))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }) { Text("Copot aplikasi") }
                        } else {
                            OutlinedButton(onClick = { confirmFile = f }) { Text("Hapus file") }
                        }
                    }
                }
            }
        }
        item {
            Text("Periksa tautan", style = MaterialTheme.typography.titleLarge)
        }
        item {
            Text(
                "Tempel alamat link dari chat, SMS, atau situs unduhan. Pemeriksaan hanya membaca pola alamat " +
                    "(offline), bukan mengecek daftar situs berbahaya.",
                fontSize = 12.sp
            )
        }
        item {
            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("Alamat tautan") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(onClick = { verdict = LinkChecker.check(link) }) {
                Text("Periksa tautan")
            }
        }
        val v = verdict
        if (v != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(v.level, color = linkColor(v.level), fontWeight = FontWeight.Bold)
                        Text(v.reasons.joinToString("\n") { "• $it" }, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
