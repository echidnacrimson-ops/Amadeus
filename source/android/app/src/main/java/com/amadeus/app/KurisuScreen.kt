package com.amadeus.app

import android.graphics.BitmapFactory
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
private fun loadAvatar(): ImageBitmap? {
    if (!Environment.isExternalStorageManager()) return null
    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val names = listOf("kurisu.png", "kurisu.jpg", "kurisu.jpeg", "kurisu.webp")
    for (n in names) {
        val f = File(dir, n)
        if (!f.exists()) continue
        try {
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            BitmapFactory.decodeFile(f.path, bounds)
            var sample = 1
            while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) {
                sample *= 2
            }
            val opts = BitmapFactory.Options()
            opts.inSampleSize = sample
            val bmp = BitmapFactory.decodeFile(f.path, opts)
            if (bmp != null) return bmp.asImageBitmap()
        } catch (e: Exception) {
        }
    }
    return null
}

@Composable
fun KurisuScreen(messages: SnapshotStateList<ChatMessage>, onNavigate: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        avatar = withContext(Dispatchers.IO) { loadAvatar() }
        if (messages.isEmpty()) {
            messages.add(
                ChatMessage(
                    false,
                    "Hmph, akhirnya kamu datang. Aku Kurisu, asisten di aplikasi Amadeus. Ketik \"bantuan\" kalau bingung mau mulai dari mana."
                )
            )
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        messages.add(ChatMessage(true, text))
        input = ""
        scope.launch {
            delay(300)
            val reply = RuleBrain.reply(context, text)
            messages.add(ChatMessage(false, reply.text))
            if (reply.action == ChatAction.BOOST_RAM) {
                val r = withContext(Dispatchers.Default) { DeviceMonitor.boostRam(context) }
                messages.add(
                    ChatMessage(
                        false,
                        "Selesai. Permintaan penutupan dikirim ke ${r.first} aplikasi, RAM bertambah ${r.second} MB. " +
                            if (r.second > 0) "Lumayan." else "Hmph, tidak banyak yang bisa ditutup."
                    )
                )
            }
            if (reply.goTo >= 0) {
                delay(600)
                onNavigate(reply.goTo)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            val a = avatar
            if (a != null) {
                Image(
                    bitmap = a,
                    contentDescription = "Kurisu",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFF6750A4)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("K", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column {
                Text("Kurisu", style = MaterialTheme.typography.titleLarge)
                Text("Asisten Amadeus (mode aturan)", fontSize = 12.sp)
            }
        }
        if (avatar == null) {
            Text(
                "Tips: taruh gambar bernama kurisu.png (atau kurisu.jpg) di folder Download untuk memasang avatar. " +
                    "Butuh izin akses semua file (atur di tab Pembersih).",
                fontSize = 11.sp
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages) { m ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 300.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (m.fromUser) Color(0xFFD0BCFF) else Color(0xFFE7E0EC)
                        )
                    ) {
                        Text(
                            m.text,
                            modifier = Modifier.padding(10.dp),
                            fontSize = 14.sp,
                            color = Color(0xFF1C1B1F)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quick = listOf("Status HP", "Cek suhu", "Bersihkan RAM", "Pindai virus", "Bantuan")
            quick.forEach { label ->
                OutlinedButton(onClick = { send(label) }) {
                    Text(label, fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ketik pesan atau tempel link...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { send(input) }) { Text("Kirim") }
        }
    }
}
