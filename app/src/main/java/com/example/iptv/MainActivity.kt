package com.example.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.iptv.model.Channel
import com.example.iptv.ui.MainViewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ui.PlayerView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm by viewModels<MainViewModel>()
        setContent {
            MaterialTheme {
                IPTVApp(vm)
            }
        }
    }
}

@Composable
fun IPTVApp(vm: MainViewModel) {
    var baseUrl by remember { mutableStateOf("https://example.com/playlist.php") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val playlist by vm.playlist.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }

    Scaffold(topBar = {
        Column(Modifier.padding(8.dp)) {
            Text("IPTV Player", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("User") }, modifier = Modifier.width(160.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Pass") }, modifier = Modifier.width(160.dp))
                Button(onClick = { vm.load(baseUrl, username, password) }) { Text("Yükle") }
            }
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Ara") }, modifier = Modifier.fillMaxWidth())
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab==0, onClick = { tab = 0; }) { Text("EN ÇOK İZLENENLER") }
                Tab(selected = tab==1, onClick = { tab = 1; }) { Text("DİĞER KANALLAR") }
            }
        }
    }) { padding ->
        val contentModifier = Modifier.padding(padding)
        if (playlist == null) {
            Box(contentModifier.fillMaxSize()) { Text("URL ve bilgileri girip Yükle'ye basın") }
        } else {
            when (tab) {
                0 -> ChannelList(vm, playlist!!.mostWatched, query)
                else -> OtherGroups(vm, playlist!!.othersByGroup, query)
            }
        }
    }
}

@Composable
fun ChannelList(vm: MainViewModel, channels: List<Channel>, query: String) {
    val filtered = vm.search(channels, query)
    LazyColumn { items(filtered) { ch -> ChannelRow(ch) } }
}

@Composable
fun OtherGroups(vm: MainViewModel, groups: Map<String, List<Channel>>, query: String) {
    val keys = groups.keys.sorted()
    LazyColumn {
        keys.forEach { key ->
            item { Text(key, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp)) }
            items(vm.search(groups[key] ?: emptyList(), query)) { ch -> ChannelRow(ch) }
        }
    }
}

@Composable
fun ChannelRow(ch: Channel) {
    var showPlayer by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { showPlayer = true }.padding(12.dp)) {
        Text(ch.name, style = MaterialTheme.typography.bodyLarge)
        Text(ch.groupTitle, style = MaterialTheme.typography.bodySmall)
    }
    if (showPlayer) PlayerDialog(ch) { showPlayer = false }
}

@Composable
fun PlayerDialog(ch: Channel, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } }, text = {
        ExoPlayerView(urls = listOf(ch.url) + ch.backups)
    })
}

@Composable
fun ExoPlayerView(urls: List<String>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var index by remember { mutableStateOf(0) }
    var player by remember {
        mutableStateOf(ExoPlayer.Builder(context).build())
    }

    DisposableEffect(urls) {
        fun prepare(url: String) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = true
        }
        prepare(urls.getOrElse(index) { urls.first() })
        val listener = object : com.google.android.exoplayer2.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == com.google.android.exoplayer2.Player.STATE_IDLE) {
                    val next = index + 1
                    if (next < urls.size) {
                        index = next
                        prepare(urls[next])
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    AndroidView(factory = { ctx ->
        PlayerView(ctx).apply { this.player = player }
    }, modifier = Modifier.fillMaxWidth().height(220.dp))
}
