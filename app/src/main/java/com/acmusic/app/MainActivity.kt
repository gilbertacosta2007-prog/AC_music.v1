package com.acmusic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

data class Track(val title: String, val artist: String, val url: String)

private val demoTracks = listOf(
    Track("Dreams", "AC Music Demo", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
    Track("Night Drive", "AC Music Demo", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
    Track("Afterglow", "AC Music Demo", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3")
)

class MusicViewModel : ViewModel() {
    var current by mutableStateOf<Track?>(null)
    var prompt by mutableStateOf("")
    var djMessage by mutableStateOf("Dígale al DJ qué quiere escuchar.")
    private var player: ExoPlayer? = null

    fun attach(p: ExoPlayer) { player = p }

    fun play(track: Track) {
        current = track
        player?.setMediaItem(MediaItem.fromUri(track.url))
        player?.prepare()
        player?.play()
    }

    fun askDj() {
        if (prompt.isBlank()) return
        val text = prompt.lowercase()
        val track = when {
            "entren" in text || "energ" in text || "movid" in text -> demoTracks[2]
            "relax" in text || "tranquil" in text -> demoTracks[0]
            else -> demoTracks.random()
        }
        djMessage = "Listo. El DJ eligió " + track.title + "."
        play(track)
        prompt = ""
    }

    fun toggle() {
        player?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    override fun onCleared() {
        player?.release()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ACMusicApp() }
    }
}

@Composable
fun ACMusicApp(vm: MusicViewModel = viewModel()) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val player = ExoPlayer.Builder(context).build()
        vm.attach(player)
        onDispose { player.release() }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("AC Music") },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                )
            },
            bottomBar = {
                vm.current?.let { MiniPlayer(it, vm::toggle) }
            }
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("Su música", style = MaterialTheme.typography.headlineMedium)
                    Text("Reproduzca música y deje que el AI DJ elija por usted.")
                }
                item {
                    Card {
                        Column(Modifier.padding(18.dp)) {
                            Text("🤖 AI DJ", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text(vm.djMessage)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = vm.prompt,
                                onValueChange = { vm.prompt = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Ej. música para entrenar") },
                                singleLine = true
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = vm::askDj,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Pedirle al DJ") }
                        }
                    }
                }
                item { Text("Para usted", style = MaterialTheme.typography.titleLarge) }
                items(demoTracks) { track ->
                    Card(onClick = { vm.play(track) }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(Modifier.size(54.dp), shape = MaterialTheme.shapes.medium) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null)
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.titleMedium)
                                Text(track.artist)
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(track: Track, onPlayPause: () -> Unit) {
    Surface(tonalElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(track.title)
                Text(track.artist, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onPlayPause) {
                Icon(Icons.Default.PlayPause, contentDescription = "Reproducir o pausar")
            }
        }
    }
}
