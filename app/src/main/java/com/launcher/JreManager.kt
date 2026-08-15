package com.launcher

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object JreManager {
    private const val TAG = "JreManager"
    private const val JRE_ASSET_DIR = "jre_unpacked"

    fun ensureJre(context: Context): File {
        val jreDir = File(context.filesDir, "jre21")
        val javaBin = File(jreDir, "bin/java")

        if (javaBin.exists() && javaBin.isFile) {
            Log.d(TAG, "JRE already exists")
            return jreDir
        }

        Log.d(TAG, "Copying JRE from assets...")
        jreDir.mkdirs()

        try {
            context.assets.list(JRE_ASSET_DIR)?.forEach { name ->
                copyAsset(context, "$JRE_ASSET_DIR/$name", File(jreDir, name))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Copy error", e)
            throw IllegalStateException("JRE复制失败: ${e.message}")
        }

        if (!javaBin.exists()) {
            Log.e(TAG, "bin/java missing")
            throw IllegalStateException("JRE复制失败：找不到bin/java")
        }
        Log.d(TAG, "JRE ready at ${jreDir.absolutePath}")
        return jreDir
    }

    private fun copyAsset(context: Context, assetPath: String, target: File) {
        val children = context.assets.list(assetPath)
        if (children != null && children.isNotEmpty()) {
            // 目录
            target.mkdirs()
            children.forEach { child ->
                copyAsset(context, "$assetPath/$child", File(target, child))
            }
        } else {
            // 文件
            target.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            target.setExecutable(true, false)
        }
    }
}
