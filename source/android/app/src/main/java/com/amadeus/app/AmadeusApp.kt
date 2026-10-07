package com.amadeus.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AmadeusApp() {
    var tab by remember { mutableStateOf(0) }
    val titles = listOf("Beranda", "Keamanan", "Perangkat", "Pembersih")
    val tight = PaddingValues(horizontal = 4.dp, vertical = 8.dp)

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            titles.forEachIndexed { index, title ->
                if (index == tab) {
                    Button(
                        onClick = { tab = index },
                        modifier = Modifier.weight(1f),
                        contentPadding = tight
                    ) { Text(title, fontSize = 11.sp, maxLines = 1) }
                } else {
                    OutlinedButton(
                        onClick = { tab = index },
                        modifier = Modifier.weight(1f),
                        contentPadding = tight
                    ) { Text(title, fontSize = 11.sp, maxLines = 1) }
                }
            }
        }
        when (tab) {
            0 -> HomeScreen(onNavigate = { tab = it })
            1 -> SecurityScreen()
            2 -> DeviceScreen()
            else -> CleanerScreen()
        }
    }
}
