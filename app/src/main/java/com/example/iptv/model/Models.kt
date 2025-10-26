package com.example.iptv.model

data class Channel(
    val name: String,
    val url: String,
    val backups: List<String> = emptyList(),
    val groupTitle: String = "GENEL"
)

data class Playlist(
    val mostWatched: List<Channel>,
    val othersByGroup: Map<String, List<Channel>>
)
