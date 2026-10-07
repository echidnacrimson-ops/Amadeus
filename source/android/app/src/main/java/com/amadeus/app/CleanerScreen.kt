package com.amadeus.app

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CleanerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasAccess by remember { mutableStateOf(Environment.isExternalStorageManager()) }
    var found by remember { mutableStateOf<List<CleanItem>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var scanning by remember { mutableStateOf(false) }
    var scannedOnce by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }

    val selectedBytes = found.filter { it.path in selected }.sumOf { it.sizeBytes }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Hapus file terpilih?") },
            text = {
                Text(
                    "${selected.size} file (${Cleaner.formatSize(selectedBytes)}) akan dihapus " +
                        "permanen dan tidak bisa dikembalikan."
                )
            },
            confirmButton = {
                Button(onClick = {
                    confirm = false
                    val paths = selected.toList()
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { Cleaner.delete(paths) }
                        found = found.filter { it.path !in paths }
                        selected = emptySet()
                        message = "Terhapus ${r.first} file, ruang kosong ${Cleaner.formatSize(r.second)}"
                    }
                }) { Text("Hapus") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirm = false }) { Text("Batal") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Pembersih", style = MaterialTheme.typography.headlineMedium)
        }
        if (!hasAccess) {
            item {
                Text(
                    "Fitur ini butuh izin akses semua file untuk mencari file sampah di penyimpanan. " +
                        "Aplikasi hanya menghapus file yang Anda centang sendiri."
                )
            }
            item {
                Button(onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + context.packageName)
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }) { Text("Berikan izin akses file") }
            }
            item {
                OutlinedButton(onClick = {
                    hasAccess = Environment.isExternalStorageManager()
                }) { Text("Saya sudah mengizinkan") }
            }
        } else {
            item {
                Button(
                    onClick = {
                        scope.launch {
                            scanning = true
                            message = ""
                            val result = withContext(Dispatchers.IO) { Cleaner.scan() }
                            found = result
                            selected = result.filter { it.selectDefault }.map { it.path }.toSet()
                            scanning = false
                            scannedOnce = true
                        }
                    },
                    enabled = !scanning
                ) {
                    Text(if (scanning) "Memindai..." else "Cari file sampah")
                }
            }
            if (message.isNotEmpty()) {
                item { Text(message) }
            }
            if (scannedOnce) {
                item {
                    Text(
                        "Ditemukan ${found.size} file. Terpilih: ${selected.size} (${Cleaner.formatSize(selectedBytes)})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (selected.isNotEmpty()) {
                    item {
                        Button(onClick = { confirm = true }) { Text("Hapus terpilih") }
                    }
                }
                items(found) { entry ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Checkbox(
                                checked = entry.path in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + entry.path else selected - entry.path
                                }
                            )
                            Column {
                                Text(File(entry.path).name, fontWeight = FontWeight.Bold)
                                Text(
                                    Cleaner.formatSize(entry.sizeBytes) + " • " + entry.reason,
                                    fontSize = 12.sp
                                )
                                Text(entry.path, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
