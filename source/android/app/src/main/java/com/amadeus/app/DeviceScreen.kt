package com.amadeus.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DeviceScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf(DeviceMonitor.snapshot(context)) }
    var boosting by remember { mutableStateOf(false) }
    var boostMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            info = DeviceMonitor.snapshot(context)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Kondisi Perangkat", style = MaterialTheme.typography.headlineMedium)
        }
        items(info) { StatusCard(it) }
        item {
            Button(
                onClick = {
                    scope.launch {
                        boosting = true
                        val r = withContext(Dispatchers.Default) { DeviceMonitor.boostRam(context) }
                        info = DeviceMonitor.snapshot(context)
                        boostMessage = "Permintaan dikirim ke ${r.first} aplikasi. RAM bertambah ${r.second} MB."
                        boosting = false
                    }
                },
                enabled = !boosting
            ) {
                Text(if (boosting) "Membersihkan..." else "Bersihkan RAM")
            }
        }
        if (boostMessage.isNotEmpty()) {
            item { Text(boostMessage) }
        }
    }
}
