package com.launcher

import org.json.JSONObject

data class GameVersion(
    val id: String,
    val type: String,
    val url: String,
    val releaseTime: String
) {
    companion object {
        fun fromJson(json: JSONObject): GameVersion {
            return GameVersion(
                id = json.getString("id"),
                type = json.getString("type"),
                url = json.getString("url"),
                releaseTime = json.optString("releaseTime", "")
            )
        }
    }
}
