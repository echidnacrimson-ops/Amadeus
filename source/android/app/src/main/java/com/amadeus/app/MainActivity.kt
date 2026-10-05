package com.amadeus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AmadeusScreen()
        }
    }
}

@Composable
fun AmadeusScreen() {
    var message by remember { mutableStateOf("") }
    var reply by remember {
        mutableStateOf(
            "Hm? Kau ingin membicarakan sesuatu? Aku mendengarkan."
        )
    }

    val backgroundColor = Color(0xFF101522)
    val accentColor = Color(0xFF70D6E8)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AMADEUS",
            color = accentColor,
            fontSize = 28.sp
        )

        Text(
            text = "AI PERSONAL ASSISTANT",
            color = Color.LightGray,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF202A3B)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = reply,
                color = Color.White,
                modifier = Modifier.padding(20.dp),
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Katakan sesuatu...") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (message.isNotBlank()) {
                    reply = "Hm, \"$message\"? Menarik. Aku akan memikirkannya."
                    message = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kirim")
        }
    }
}
