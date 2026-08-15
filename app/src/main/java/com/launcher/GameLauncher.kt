package com.launcher

import android.content.Context
import org.json.JSONObject
import java.io.File

object GameLauncher {
    fun launch(context: Context, versionId: String, ramMB: Int) {
        val gameDir = context.getExternalFilesDir(null)?.let { File(it, "minecraft") }
            ?: context.filesDir.resolve("minecraft")
        val jreDir = JreManager.ensureJre(context)  // 已是外部存储可执行路径
        val javaBin = File(jreDir, "bin/java")

        if (!javaBin.exists()) {
            throw IllegalStateException("JRE不可用")
        }

        val versionJsonFile = File(gameDir, "versions/$versionId/$versionId.json")
        if (!versionJsonFile.exists()) {
            throw IllegalStateException("版本文件缺失")
        }
        val versionJson = JSONObject(versionJsonFile.readText())
        val libsDir = File(gameDir, "libraries")
        val versionsDir = File(gameDir, "versions/$versionId")

        // 构建classpath
        val classpath = buildList {
            add(File(versionsDir, "$versionId.jar").absolutePath)
            val libraries = versionJson.getJSONArray("libraries")
            for (i in 0 until libraries.length()) {
                val lib = libraries.getJSONObject(i)
                if (lib.has("natives")) continue
                val artifact = lib.optJSONObject("downloads")?.optJSONObject("artifact") ?: continue
                val path = artifact.getString("path")
                val file = File(libsDir, path)
                if (file.exists()) add(file.absolutePath)
            }
        }

        val mainClass = versionJson.getString("mainClass")

        // 构建命令
        val cmd = buildList {
            add(javaBin.absolutePath)
            add("-Xms${ramMB / 1024}g")
            add("-Xmx${ramMB / 1024}g")
            add("-Djava.library.path=${File(versionsDir, "natives").absolutePath}")
            add("-cp")
            add(classpath.joinToString(":"))
            add(mainClass)
        }

        val pb = ProcessBuilder(cmd)
        pb.directory(gameDir)
        pb.redirectErrorStream(true)
        pb.start()
    }
}
