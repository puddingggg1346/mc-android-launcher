package com.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LauncherScreen()
            }
        }
    }
}

@Composable
fun LauncherScreen() {
    var version by remember { mutableStateOf("1.20.1") }
    var ram by remember { mutableStateOf("2048") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Minecraft Java Launcher", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = version,
            onValueChange = { version = it },
            label = { Text("游戏版本") }
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = ram,
            onValueChange = { ram = it },
            label = { Text("内存 (MB)") }
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val ramInt = ram.toIntOrNull() ?: 2048
                Toast.makeText(context, "启动 $version (${ramInt}MB)", Toast.LENGTH_SHORT).show()
            }
        ) {
            Text("启动游戏")
        }
    }
}
