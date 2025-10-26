package com.example.iptv.data

import com.example.iptv.model.Channel
import com.example.iptv.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request

class PlaylistRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun fetchM3U(baseUrl: String, username: String, password: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(baseUrl)
            .header("Authorization", Credentials.basic(username, password))
            .header("User-Agent", "IPTVPlayer/1.0")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${'$'}{resp.code}")
            resp.body?.string() ?: error("Empty body")
        }
    }

    fun parseM3U(text: String): Playlist {
        val lines = text.lines()
        val entries = mutableListOf<Pair<String, String>>() // extinf, url
        var lastExtInf = ""
        for (line in lines) {
            val t = line.trim()
            if (t.startsWith("#EXTINF")) lastExtInf = t
            else if (t.isNotBlank() && !t.startsWith("#")) entries += lastExtInf to t
        }

        // group channels by display name, collect backups
        val primary = mutableMapOf<String, MutableList<Pair<String, String>>>() // name -> list of (group,titleUrl)
        for ((ext, url) in entries) {
            val name = ext.substringAfter(",", missingDelimiterValue = "").trim()
            val group = Regex("group-title=\"([^\"]+)\"").find(ext)?.groupValues?.get(1) ?: "GENEL"
            if (name.isEmpty()) continue
            primary.getOrPut(name) { mutableListOf() }.add(group to url)
        }

        val channels = primary.map { (name, list) ->
            val group = list.firstOrNull()?.first ?: "GENEL"
            val url = list.firstOrNull()?.second ?: ""
            val backups = list.drop(1).map { it.second }
            Channel(name = name, url = url, backups = backups, groupTitle = group)
        }

        val most = channels.filter { it.groupTitle.equals("EN ÇOK İZLENENLER", ignoreCase = true) }
        val others = channels.filter { !it.groupTitle.equals("EN ÇOK İZLENENLER", ignoreCase = true) }
            .groupBy { it.groupTitle }

        return Playlist(mostWatched = most, othersByGroup = others)
    }
}
