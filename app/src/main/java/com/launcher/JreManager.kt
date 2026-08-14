package com.launcher

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream

object JreManager {
    private const val TAG = "JreManager"
    private const val JRE_ASSET = "jre/jre21.tar.xz"

    fun ensureJre(context: Context): File {
        val jreDir = File(context.filesDir, "jre21")
        val javaBin = File(jreDir, "bin/java")

        // 已存在则直接复用
        if (javaBin.exists() && javaBin.isFile) {
            Log.d(TAG, "JRE already exists")
            return jreDir
        }

        Log.d(TAG, "JRE not found, extracting...")
        val tmpDir = File(context.cacheDir, "jre21_extract")

        try {
            tmpDir.deleteRecursively()
            tmpDir.mkdirs()

            val asset = context.assets.open(JRE_ASSET)
            Log.d(TAG, "Asset opened: ${asset.available()} bytes")

            asset.use { a ->
                XZInputStream(a).use { xz ->
                    TarArchiveInputStream(xz).use { tar ->
                        var entry: TarArchiveEntry? = tar.currentEntry
                        while (true) {
                            entry = tar.nextEntry ?: break
                            val name = entry.name.removePrefix("./").removeSuffix("/")
                            if (name.isEmpty()) continue

                            val target = File(tmpDir, name)
                            if (entry.isDirectory) {
                                target.mkdirs()
                            } else if (entry.isSymbolicLink) {
                                // 复制链接目标
                                try {
                                    val linkPath = entry.linkName.removePrefix("./")
                                    val src = File(tmpDir, linkPath)
                                    if (src.exists()) {
                                        target.parentFile?.mkdirs()
                                        src.copyTo(target)
                                    } else {
                                        Log.w(TAG, "Symlink target missing: $linkPath → $name")
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Symlink failed for $name: ${e.message}")
                                }
                            } else {
                                target.parentFile?.mkdirs()
                                FileOutputStream(target).use { out ->
                                    tar.copyTo(out)
                                }
                                if (entry.mode and 0x40 != 0) {
                                    target.setExecutable(true, false)
                                }
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "Extraction complete")

            // 移动tmp到正式位置
            jreDir.deleteRecursively()
            jreDir.mkdirs()
            tmpDir.listFiles()?.forEach { it.copyRecursively(jreDir, overwrite = true) }
            tmpDir.deleteRecursively()

            // 检查java
            val extractedJava = File(jreDir, "bin/java")
            if (!extractedJava.exists()) {
                Log.e(TAG, "bin/java NOT FOUND after extraction!")
                Log.e(TAG, "jreDir contents: ${jreDir.list()?.joinToString()}")
                throw IllegalStateException("JRE解压失败：找不到bin/java")
            }
            Log.d(TAG, "bin/java exists at ${extractedJava.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Extract error", e)
            throw IllegalStateException("JRE解压失败: ${e.message}")
        }

        return jreDir
    }
}
