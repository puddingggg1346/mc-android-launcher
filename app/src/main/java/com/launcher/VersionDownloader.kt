package com.launcher

import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object VersionDownloader {
    fun downloadVersion(
        version: GameVersion,
        gameDir: File,
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ) {
        val versionsDir = File(gameDir, "versions/${version.id}")
        versionsDir.mkdirs()

        val jsonFile = File(versionsDir, "${version.id}.json")
        if (!jsonFile.exists()) {
            onProgress("下载版本描述文件...", 5)
            downloadFile(version.url, jsonFile)
        }

        val versionJson = JSONObject(jsonFile.readText())
        val downloads = versionJson.getJSONObject("downloads")

        // 下载客户端jar
        val clientJar = File(versionsDir, "${version.id}.jar")
        if (!clientJar.exists()) {
            onProgress("下载游戏客户端...", 30)
            val jarUrl = downloads.getJSONObject("client").getString("url")
            downloadFile(jarUrl, clientJar)
        }

        // 下载client-mappings（可选）
        if (downloads.has("client_mappings")) {
            val mappings = File(versionsDir, "${version.id}-client.txt")
            if (!mappings.exists()) {
                val mappingsUrl = downloads.getJSONObject("client_mappings").getString("url")
                downloadFile(mappingsUrl, mappings)
            }
        }

        // 下载libraries
        val libsDir = File(gameDir, "libraries")
        val libraries = versionJson.getJSONArray("libraries")
        var count = 0
        val total = libraries.length()
        for (i in 0 until total) {
            val lib = libraries.getJSONObject(i)
            // 跳过Android不支持的native库
            if (lib.has("natives")) continue
            if (lib.has("rules")) {
                val rules = lib.getJSONArray("rules")
                var allow = false
                for (j in 0 until rules.length()) {
                    val rule = rules.getJSONObject(j)
                    val action = rule.getString("action")
                    val osRule = rule.optJSONObject("os")
                    if (osRule != null) {
                        val osName = osRule.optString("name", "")
                        if (action == "allow" && osName == "windows") allow = osName != "windows"
                        if (action == "allow" && osName.isEmpty()) allow = true
                        if (action == "disallow" && osName == "windows") allow = false
                    } else {
                        if (action == "allow") allow = true
                    }
                }
                if (!allow) continue
            }
            val artifact = lib.optJSONObject("downloads")?.optJSONObject("artifact")
            if (artifact != null) {
                val path = artifact.getString("path")
                val target = File(libsDir, path)
                if (!target.exists()) {
                    target.parentFile.mkdirs()
                    val url = artifact.getString("url")
                    val progress = 40 + ((count.toFloat() / total) * 50).toInt()
                    onProgress("下载依赖库 ${target.name}", progress)
                    downloadFile(url, target)
                }
            }
            count++
        }

        onProgress("下载完成", 100)
    }

    private fun downloadFile(urlStr: String, target: File) {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        conn.instanceFollowRedirects = true

        target.parentFile?.mkdirs()
        conn.inputStream.use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }
    }
}
