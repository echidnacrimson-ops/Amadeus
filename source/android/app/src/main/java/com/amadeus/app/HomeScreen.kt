package com.amadeus.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val HomeGreen = Color(0xFF2E7D32)
private val HomeOrange = Color(0xFFEF6C00)
private val HomeRed = Color(0xFFC62828)

private fun usageColor(pct: Int): Color {
    return when {
        pct >= 85 -> HomeRed
        pct >= 70 -> HomeOrange
        else -> HomeGreen
    }
}

@Composable
fun Ring(fraction: Float, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 11.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                drawArc(
                    color = color.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun HomeScreen(onNavigate: (Int) -> Unit) {
    val context = LocalContext.current
    var q by remember { mutableStateOf(HomeData.quick(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            q = HomeData.quick(context)
        }
    }

    val batteryColor = if (q.batteryPct < 20) HomeRed else HomeGreen
    val tempColor = when {
        q.tempC >= 45f -> HomeRed
        q.tempC >= 40f -> HomeOrange
        else -> HomeGreen
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Amadeus", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Ring(q.ramPct / 100f, "RAM terpakai", q.ramPct.toString() + "%", usageColor(q.ramPct))
                Ring(q.storagePct / 100f, "Penyimpanan", q.storagePct.toString() + "%", usageColor(q.storagePct))
                Ring(q.batteryPct / 100f, "Baterai", q.batteryPct.toString() + "%", batteryColor)
            }
        }
        item {
            Text(
                "Suhu baterai: " + String.format("%.1f", q.tempC) + " °C",
                color = tempColor,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Button(onClick = { onNavigate(1) }, modifier = Modifier.fillMaxWidth()) {
                Text("Pindai keamanan")
            }
        }
        item {
            OutlinedButton(onClick = { onNavigate(2) }, modifier = Modifier.fillMaxWidth()) {
                Text("Pantau perangkat dan bersihkan RAM")
            }
        }
        item {
            OutlinedButton(onClick = { onNavigate(3) }, modifier = Modifier.fillMaxWidth()) {
                Text("Cari file sampah")
            }
        }
    }
}
