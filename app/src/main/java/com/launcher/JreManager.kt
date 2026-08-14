package com.launcher

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream

object JreManager {
    private const val JRE_ASSET = "jre/jre21.tar.xz"

    fun ensureJre(context: Context): File {
        val jreDir = File(context.filesDir, "jre21")
        val javaBin = File(jreDir, "bin/java")
        if (javaBin.exists() && javaBin.isFile) {
            return jreDir
        }

        if (!jreDir.exists()) jreDir.mkdirs()

        // 使用临时目录解压再切换
        val tmpDir = File(context.cacheDir, "jre21_tmp")
        tmpDir.deleteRecursively()
        tmpDir.mkdirs()

        context.assets.open(JRE_ASSET).use { asset ->
            XZInputStream(asset).use { xz ->
                TarArchiveInputStream(xz).use { tar ->
                    var entry: TarArchiveEntry? = tar.nextEntry
                    while (entry != null) {
                        if (entry.isDirectory) {
                            File(tmpDir, entry.name.removePrefix("./")).mkdirs()
                        } else if (entry.isSymbolicLink) {
                            // 处理符号链接
                            val linkTarget = entry.linkName
                            val target = File(tmpDir, entry.name.removePrefix("./"))
                            target.parentFile?.mkdirs()
                            // Android上符号链接可能不可用，复制链接目标
                            createSymlinkShim(linkTarget, target, entry.name.removePrefix("./"))
                        } else {
                            val target = File(tmpDir, entry.name.removePrefix("./"))
                            target.parentFile?.mkdirs()
                            FileOutputStream(target).use { out ->
                                tar.copyTo(out)
                            }
                            if (entry.mode and 0x40 != 0) {
                                target.setExecutable(true, false)
                            }
                        }
                        entry = tar.nextEntry
                    }
                }
            }
        }

        // 解压完成，移动到正式目录
        jreDir.deleteRecursively()
        File(context.filesDir, "jre21").mkdirs()

        // 简化：假设没有符号链接问题，直接把tmpDir移动
        tmpDir.listFiles()?.forEach { file ->
            file.copyRecursively(jreDir, overwrite = true)
        }
        tmpDir.deleteRecursively()

        if (!File(jreDir, "bin/java").exists()) {
            throw IllegalStateException("JRE解压失败")
        }
        return jreDir
    }

    private fun createSymlinkShim(linkTarget: String, target: File, entryName: String) {
        // 多数情况下JRE的symlink指向同一目录下的其他文件
        // 这里尝试普通复制，如果失败则跳过
        try {
            val resolved = if (linkTarget.startsWith("/")) {
                File(linkTarget)
            } else {
                File(target.parentFile, linkTarget)
            }
            if (resolved.exists()) {
                resolved.copyTo(target)
                target.setExecutable(resolved.canExecute())
            }
        } catch (e: Exception) {
            // 符号链接失败不阻塞
        }
    }
}
