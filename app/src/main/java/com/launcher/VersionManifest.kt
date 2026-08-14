package com.launcher

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object VersionManifest {
    private const val MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

    fun fetchVersions(): List<GameVersion> {
        val conn = URL(MANIFEST_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.requestMethod = "GET"

        val json = JSONObject(BufferedReader(InputStreamReader(conn.inputStream)).readText())
        val versions = JSONArray()
        // 兼容不同字段名
        val arr = if (json.has("versions")) json.getJSONArray("versions") else JSONArray()
        return buildList {
            for (i in 0 until arr.length()) {
                add(GameVersion.fromJson(arr.getJSONObject(i)))
            }
        }.filter { it.type == "release" }
    }
}
