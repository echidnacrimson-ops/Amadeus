package com.amadeus.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Page(val title: String, val id: Int)

@Composable
fun AmadeusApp() {
    var current by remember { mutableStateOf(0) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val pages = listOf(
        Page("Beranda", 0),
        Page("Kurisu", 5),
        Page("Keamanan", 1),
        Page("Perangkat", 2),
        Page("Pembersih", 3),
        Page("Antivirus", 4)
    )
    val tight = PaddingValues(horizontal = 14.dp, vertical = 8.dp)

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            pages.forEach { page ->
                if (page.id == current) {
                    Button(
                        onClick = { current = page.id },
                        contentPadding = tight
                    ) { Text(page.title, fontSize = 12.sp, maxLines = 1) }
                } else {
                    OutlinedButton(
                        onClick = { current = page.id },
                        contentPadding = tight
                    ) { Text(page.title, fontSize = 12.sp, maxLines = 1) }
                }
            }
        }
        when (current) {
            0 -> HomeScreen(onNavigate = { current = it })
            1 -> SecurityScreen()
            2 -> DeviceScreen()
            3 -> CleanerScreen()
            4 -> AntivirusScreen()
            else -> KurisuScreen(messages, onNavigate = { current = it })
        }
    }
}
