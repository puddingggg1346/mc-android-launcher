package com.launcher

import org.json.JSONObject
import java.io.File

object LibraryResolver {
    fun resolveLibraries(versionJson: JSONObject): List<String> {
        val libraries = versionJson.getJSONArray("libraries")
        val result = mutableListOf<String>()
        val osName = System.getProperty("os.name").lowercase()
        val isAndroid = osName.contains("linux") || osName.contains("android")

        for (i in 0 until libraries.length()) {
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
                        val osName2 = osRule.optString("name", "")
                        val matches = !isAndroid || osName2 != "windows"
                        if (matches && action == "allow") allow = true
                        if (matches && action == "disallow") allow = false
                    } else {
                        if (action == "allow") allow = true
                    }
                }
                if (!allow) continue
            }
            val artifact = lib.optJSONObject("downloads")?.optJSONObject("artifact")
            if (artifact != null) {
                result.add(artifact.getString("path"))
            }
        }
        return result
    }
}
