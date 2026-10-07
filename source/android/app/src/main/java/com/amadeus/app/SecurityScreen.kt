package com.amadeus.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Green = Color(0xFF2E7D32)
private val Orange = Color(0xFFEF6C00)
private val Red = Color(0xFFC62828)

@Composable
fun SecurityScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<List<StatusItem>>(emptyList()) }
    var apps by remember { mutableStateOf<List<AppRisk>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Amadeus Security", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        scanning = true
                        val result = withContext(Dispatchers.Default) {
                            Pair(
                                SecurityScanner.deviceStatus(context),
                                SecurityScanner.scanApps(context)
                            )
                        }
                        status = result.first
                        apps = result.second
                        scanning = false
                        scanned = true
                    }
                },
                enabled = !scanning
            ) {
                Text(if (scanning) "Memindai..." else "Pindai sekarang")
            }
        }
        if (scanned) {
            item {
                Text("Status keamanan HP", style = MaterialTheme.typography.titleMedium)
            }
            items(status) { StatusCard(it) }
            item {
                val high = apps.count { it.level == "TINGGI" }
                Text(
                    "Aplikasi terpasang: ${apps.size} (risiko tinggi: $high)",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            item {
                Text(
                    "Skor dihitung dari izin dan sumber pemasangan. Skor tinggi bukan berarti pasti malware, " +
                        "tapi aplikasi itu layak diperiksa.",
                    fontSize = 12.sp
                )
            }
            items(apps) { AppCard(it) }
        }
    }
}

@Composable
fun StatusCard(item: StatusItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                item.title,
                fontWeight = FontWeight.Bold,
                color = if (item.ok) Green else Red
            )
            Text(item.detail)
        }
    }
}

@Composable
fun AppCard(app: AppRisk) {
    val color = when (app.level) {
        "TINGGI" -> Red
        "SEDANG" -> Orange
        else -> Green
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(app.name, fontWeight = FontWeight.Bold)
            Text("Risiko ${app.level} (skor ${app.score})", color = color)
            Text(app.packageName, fontSize = 11.sp)
            Text("Pemasang: " + app.installer, fontSize = 11.sp)
            if (app.reasons.isNotEmpty()) {
                Text(app.reasons.joinToString("\n") { "• $it" }, fontSize = 12.sp)
            }
        }
    }
}
