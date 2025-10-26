package com.example.iptv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptv.data.PlaylistRepository
import com.example.iptv.model.Channel
import com.example.iptv.model.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repo: PlaylistRepository = PlaylistRepository()
): ViewModel() {
    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    fun setQuery(q: String) { _query.value = q }

    fun load(baseUrl: String, username: String, password: String) {
        viewModelScope.launch {
            runCatching {
                val text = repo.fetchM3U(baseUrl, username, password)
                repo.parseM3U(text)
            }.onSuccess { _playlist.value = it }
             .onFailure { _playlist.value = Playlist(emptyList(), emptyMap()) }
        }
    }

    fun search(list: List<Channel>, q: String): List<Channel> =
        if (q.isBlank()) list else list.filter { it.name.contains(q, ignoreCase = true) }
}
