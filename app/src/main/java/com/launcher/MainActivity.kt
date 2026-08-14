package com.launcher

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.io.File
import kotlinx.coroutines.*

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var versions by remember { mutableStateOf<List<GameVersion>>(emptyList()) }
    var selectedVersion by remember { mutableStateOf<GameVersion?>(null) }
    var ram by remember { mutableStateOf("2048") }
    var isLoading by remember { mutableStateOf(false) }
    var progressMsg by remember { mutableStateOf("") }
    var progressValue by remember { mutableStateOf(0f) }
    var showVersionPicker by remember { mutableStateOf(false) }

    // 加载版本列表
    LaunchedEffect(Unit) {
        isLoading = true
        progressMsg = "获取版本列表..."
        try {
            versions = withContext(Dispatchers.IO) {
                VersionManifest.fetchVersions()
            }
            selectedVersion = versions.firstOrNull { it.id == "1.21.1" } ?: versions.firstOrNull()
        } catch (e: Exception) {
            Toast.makeText(context, "获取版本失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Minecraft Java Launcher", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // 版本选择
        OutlinedButton(onClick = { showVersionPicker = true }) {
            Text(selectedVersion?.id ?: "选择版本")
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = ram,
            onValueChange = { ram = it },
            label = { Text("内存 (MB)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.height(24.dp))

        // 进度显示
        if (isLoading || progressMsg.isNotEmpty()) {
            if (progressValue > 0f && progressValue < 100f) {
                LinearProgressIndicator(
                    progress = { progressValue / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(progressMsg, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = {
                val version = selectedVersion ?: run {
                    Toast.makeText(context, "请先选择版本", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                scope.launch {
                    try {
                        isLoading = true
                        val gameDir = context.getExternalFilesDir(null)?.let { java.io.File(it, "minecraft") }
                            ?: context.filesDir.resolve("minecraft")
                        withContext(Dispatchers.IO) {
                            VersionDownloader.downloadVersion(version, gameDir) { msg, pct ->
                                progressMsg = msg
                                progressValue = pct.toFloat()
                            }
                        }
                        progressMsg = "准备启动..."
                        withContext(Dispatchers.IO) {
                            GameLauncher.launch(context, version.id, ram.toIntOrNull() ?: 2048)
                        }
                        progressMsg = "游戏已启动"
                        Toast.makeText(context, "游戏进程已启动", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "失败: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("Launcher", "Download failed", e)
                    } finally {
                        isLoading = false
                        progressValue = 0f
                    }
                }
            },
            enabled = !isLoading && selectedVersion != null
        ) {
            Text(if (isLoading) "处理中..." else "下载并启动")
        }
    }

    // 版本选择弹窗
    if (showVersionPicker) {
        AlertDialog(
            onDismissRequest = { showVersionPicker = false },
            title = { Text("选择版本") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(400.dp)
                ) {
                    items(versions) { ver ->
                        Text(
                            text = "${ver.id} (${ver.type})",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVersion = ver
                                    showVersionPicker = false
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersionPicker = false }) { Text("取消") }
            }
        )
    }
}
