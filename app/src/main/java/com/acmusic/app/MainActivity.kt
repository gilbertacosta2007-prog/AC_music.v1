package com.acmusic.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Track(val title:String,val artist:String,val url:String,val isLocal:Boolean=false)
private val demo=listOf(
 Track("Dreams","AC Music Demo","https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
 Track("Night Drive","AC Music Demo","https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
 Track("Afterglow","AC Music Demo","https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
 Track("Midnight City","AC Music Demo","https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"))
enum class Tab{HOME,SEARCH,LIKES,PLAYLISTS,SETTINGS}

class MusicViewModel:ViewModel(){
 var current by mutableStateOf<Track?>(null);var playing by mutableStateOf(false);var tab by mutableStateOf(Tab.HOME)
 var search by mutableStateOf("");var djInput by mutableStateOf("");var djMessage by mutableStateOf("Mírame. Dígame qué quiere escuchar.")
 var likes by mutableStateOf(setOf<String>());var localTracks by mutableStateOf<List<Track>>(emptyList());var positionMs by mutableStateOf(0L);var durationMs by mutableStateOf(0L);var shuffle by mutableStateOf(false);var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF);var accent by mutableStateOf(Color(0xFFE53935));var visualizer by mutableStateOf(true)
 var player:ExoPlayer?=null;var playerOpen by mutableStateOf(false);var djOpen by mutableStateOf(false);var lyricsOpen by mutableStateOf(false);var youtubeOpen by mutableStateOf(false);var youtubeUrl by mutableStateOf("https://music.youtube.com/")
 fun attach(p:ExoPlayer){player=p;p.repeatMode=repeatMode;p.shuffleModeEnabled=shuffle}
 fun refreshLocal(context:Context){viewModelScope.launch(Dispatchers.IO){localTracks=runCatching{LocalAudioRepository.load(context)}.getOrDefault(emptyList())}}
 fun updateProgress(){player?.let{positionMs=it.currentPosition.coerceAtLeast(0L);durationMs=it.duration.takeIf{d->d>0}?:0L;playing=it.isPlaying}}
 fun next(){val list=allTracks();if(list.isEmpty())return;val currentIndex=list.indexOfFirst{it.url==current?.url};val target=if(shuffle)list.random() else list[(currentIndex+1).mod(list.size)];if(repeatMode==Player.REPEAT_MODE_ONE){player?.seekTo(0);player?.play();return};play(target)}
 fun previous(){val list=allTracks();if(list.isEmpty())return;val p=player;if(p!=null&&p.currentPosition>3000){p.seekTo(0);return};val currentIndex=list.indexOfFirst{it.url==current?.url}.let{if(it<0)0 else it};play(list[(currentIndex-1+list.size).mod(list.size)])}
 fun toggleShuffle(){shuffle=!shuffle;player?.shuffleModeEnabled=shuffle}
 fun cycleRepeat(){repeatMode=when(repeatMode){Player.REPEAT_MODE_OFF->Player.REPEAT_MODE_ALL;Player.REPEAT_MODE_ALL->Player.REPEAT_MODE_ONE;else->Player.REPEAT_MODE_OFF};player?.repeatMode=repeatMode}
 fun seekTo(ms:Long){player?.seekTo(ms);positionMs=ms}
 fun allTracks()=demo+localTracks
 fun play(t:Track){current=t;player?.setMediaItem(MediaItem.fromUri(t.url));player?.prepare();player?.play();playing=true}
 fun toggle(){player?.let{if(it.isPlaying){it.pause();playing=false}else{it.play();playing=true}}}
 fun like(t:Track){likes=if(t.url in likes)likes-t.url else likes+t.url}
 fun askDj(){if(djInput.isBlank())return;val q=djInput.lowercase();val t=when{q.contains("energ")||q.contains("gym")||q.contains("fiesta")->demo[2];q.contains("relax")||q.contains("calma")->demo[0];else->demo.random()};djMessage=if(q.contains("sugarland"))"Oh. Mírame, esto sí está mejor que la música aburrida de Sugarland. Vamos con ${t.title}." else if(t.isLocal) "Oh. Encontré esa canción en su teléfono. Puse ${t.title}. Diablazo." else "Ok. Ese mood está claro. Puse ${t.title}. Diablazo.";play(t);djInput=""}
 fun filtered()=if(search.isBlank())allTracks() else allTracks().filter{it.title.contains(search,true)||it.artist.contains(search,true)}
 fun openYouTubeSearch(query:String){val q=query.ifBlank{"música"};youtubeUrl="https://music.youtube.com/search?q="+Uri.encode(q);youtubeOpen=true}
}

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{ACMusic()}}}

@Composable
fun ACMusic(vm:MusicViewModel=viewModel()){
 val ctx=LocalContext.current
 val permissionName=if(Build.VERSION.SDK_INT>=33)Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
 var permissionGranted by remember{mutableStateOf(ContextCompat.checkSelfPermission(ctx,permissionName)==PackageManager.PERMISSION_GRANTED)}
 val permissionLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
  permissionGranted=granted
  if(granted)vm.refreshLocal(ctx)
 }
 DisposableEffect(Unit){
  val p=ExoPlayer.Builder(ctx).build()
  vm.attach(p)
  onDispose{p.release()}
 }
 LaunchedEffect(permissionGranted){if(permissionGranted)vm.refreshLocal(ctx)}
 LaunchedEffect(vm.playing){
  while(true){
   vm.updateProgress()
   delay(500)
  }
 }
 MaterialTheme(colorScheme=darkColorScheme(primary=vm.accent,background=Color(0xFF080808),surface=Color(0xFF151515))){
  Box(Modifier.fillMaxSize().background(Color(0xFF080808))){
   Column(Modifier.fillMaxSize()){
    when(vm.tab){
     Tab.HOME->Home(vm,permissionGranted){permissionLauncher.launch(permissionName)}
     Tab.SEARCH->Search(vm)
     Tab.LIKES->Likes(vm)
     Tab.PLAYLISTS->Playlists(vm)
     Tab.SETTINGS->Settings(vm,permissionGranted){permissionLauncher.launch(permissionName)}
    }
    vm.current?.let{Mini(it,vm)}
    NavigationBar(containerColor=Color(0xFF0B0B0B)){
     Nav(Tab.HOME,"Principal",Icons.Default.Home,vm)
     Nav(Tab.SEARCH,"Buscar",Icons.Default.Search,vm)
     Nav(Tab.LIKES,"Me gusta",Icons.Default.Favorite,vm)
     Nav(Tab.PLAYLISTS,"Playlists",Icons.Default.QueueMusic,vm)
     Nav(Tab.SETTINGS,"Ajustes",Icons.Default.Settings,vm)
    }
   }
   FloatingActionButton({vm.djOpen=true},Modifier.align(Alignment.BottomEnd).padding(18.dp).padding(bottom=70.dp),containerColor=vm.accent){Icon(Icons.Default.Mic,"DJ Flow")}
   if(vm.playerOpen)Player(vm)
   if(vm.djOpen)DJ(vm)
   if(vm.lyricsOpen)Lyrics(vm)
   if(vm.youtubeOpen)YouTubeMusic(vm)
  }
 }
}

@Composable
fun Nav(
    t: Tab,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    vm: MusicViewModel
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable { vm.tab = t }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (vm.tab == t) vm.accent else Color.Gray
        )
        Text(
            text = label,
            color = if (vm.tab == t) vm.accent else Color.Gray,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable fun Home(vm:MusicViewModel,permissionGranted:Boolean,requestPermission:()->Unit){LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){item{Spacer(Modifier.height(16.dp));Text("AC",color=vm.accent);Text("Music",style=MaterialTheme.typography.displaySmall);Text("Su música. Su ritmo. Su DJ.",color=Color.Gray)};item{Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF151515)),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(20.dp)){Text("DJ Flow",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(8.dp));Text(vm.djMessage);Spacer(Modifier.height(12.dp));Button({vm.djOpen=true},Modifier.fillMaxWidth()){Icon(Icons.Default.Call,null);Spacer(Modifier.width(8.dp));Text("Hablar con DJ Flow")}}}};item{LocalLibraryCard(vm,permissionGranted,requestPermission)};item{Text("Para usted",style=MaterialTheme.typography.titleLarge)};items(vm.filtered()){TrackRow(it,vm)}}}
@Composable fun TrackRow(t:Track,vm:MusicViewModel){Row(Modifier.fillMaxWidth().clickable{vm.play(t)}.padding(10.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF242424)),contentAlignment=Alignment.Center){Icon(Icons.Default.MusicNote,null,tint=vm.accent)};Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(t.title);Text(t.artist,color=Color.Gray)};IconButton({vm.like(t)}){Icon(if(t.title in vm.likes)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=if(t.title in vm.likes)vm.accent else Color.Gray)}}}
@Composable fun Search(vm:MusicViewModel){Column(Modifier.fillMaxSize().padding(20.dp)){Spacer(Modifier.height(20.dp));Text("Buscar",style=MaterialTheme.typography.displaySmall);Spacer(Modifier.height(12.dp));OutlinedTextField(vm.search,{vm.search=it},Modifier.fillMaxWidth(),placeholder={Text("Canciones, artistas, playlists")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true);LazyColumn{items(vm.filtered()){TrackRow(it,vm)}}}}
@Composable fun Likes(vm:MusicViewModel){val l=vm.allTracks().filter{it.url in vm.likes};Column(Modifier.fillMaxSize().padding(20.dp)){Spacer(Modifier.height(20.dp));Text("Me gusta",style=MaterialTheme.typography.displaySmall);if(l.isEmpty())Text("Todavía no hay canciones guardadas.",color=Color.Gray)else LazyColumn{items(l){TrackRow(it,vm)}}}}
@Composable fun Playlists(vm:MusicViewModel){Column(Modifier.fillMaxSize().padding(20.dp)){Spacer(Modifier.height(20.dp));Text("Playlists",style=MaterialTheme.typography.displaySmall);listOf("Favoritas","Flow nocturno","Entrenamiento").forEach{Card(Modifier.fillMaxWidth().padding(vertical=6.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF151515))){Text(it,Modifier.padding(20.dp))}}}}
@Composable fun Settings(vm:MusicViewModel,permissionGranted:Boolean,requestPermission:()->Unit){val context=LocalContext.current;LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Spacer(Modifier.height(20.dp));Text("Ajustes",style=MaterialTheme.typography.displaySmall);Text("Personalice AC Music.",color=Color.Gray)};item{Text("BIBLIOTECA LOCAL",color=Color.Gray)};item{Text(if(permissionGranted)"${vm.localTracks.size} canciones locales disponibles" else "Active el acceso para reproducir las canciones descargadas en el teléfono",color=Color.Gray)};item{Button(if(permissionGranted){ {vm.refreshLocal(context)} } else requestPermission,Modifier.fillMaxWidth()){Text(if(permissionGranted)"Actualizar biblioteca" else "Dar acceso a la música")}};item{Text("APARIENCIA",color=Color.Gray)};item{SwitchRow("Visualizador circular","Animación alrededor de la portada",vm.visualizer){vm.visualizer=it}};item{SwitchRow("Reproducción aleatoria","Mezclar la cola",vm.shuffle){vm.toggleShuffle()}};item{Text("Color de acento")};item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){listOf(Color(0xFFE53935),Color(0xFF7C4DFF),Color(0xFF00BFA5),Color(0xFFFF9800)).forEach{Box(Modifier.size(38.dp).clip(CircleShape).background(it).clickable{vm.accent=it})}}};item{Text("Normalización",Modifier.padding(12.dp))};item{Text("Ecualizador",Modifier.padding(12.dp))};item{Text("Caché persistente",Modifier.padding(12.dp))};item{Text("YouTube Music — fuente pendiente de integración",Modifier.padding(12.dp),color=Color.Gray)}}}
@Composable
fun LocalLibraryCard(vm:MusicViewModel,granted:Boolean,request:()->Unit){val context=LocalContext.current
 Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF151515)),shape=RoundedCornerShape(24.dp)){
  Column(Modifier.padding(20.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){
    Icon(Icons.Default.LibraryMusic,null,tint=vm.accent,modifier=Modifier.size(32.dp))
    Spacer(Modifier.width(12.dp))
    Column{Text("Música de su teléfono",style=MaterialTheme.typography.titleMedium);Text(if(granted)"${vm.localTracks.size} canciones encontradas" else "Canciones descargadas y guardadas en el celular",color=Color.Gray)}
   }
   Spacer(Modifier.height(12.dp))
   if(!granted)Button(request,Modifier.fillMaxWidth()){Icon(Icons.Default.FolderOpen,null);Spacer(Modifier.width(8.dp));Text("Dar acceso a mi música")}
   else OutlinedButton({vm.refreshLocal(context)},Modifier.fillMaxWidth()){Icon(Icons.Default.Refresh,null);Spacer(Modifier.width(8.dp));Text("Actualizar biblioteca")}
  }
 }
}
@Composable fun SwitchRow(a:String,b:String,v:Boolean,on:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(a);Text(b,color=Color.Gray)};Switch(checked=v,onCheckedChange=on)}}
@Composable fun Mini(t:Track,vm:MusicViewModel){Surface(color=Color(0xFF161616)){Row(Modifier.fillMaxWidth().clickable{vm.playerOpen=true}.padding(10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(t.title);Text(t.artist,color=Color.Gray)};IconButton(vm::toggle){Icon(if(vm.playing)Icons.Default.Pause else Icons.Default.PlayArrow,null)}}}}
@Composable
fun Player(vm: MusicViewModel) {
    val t = vm.current ?: return
    val transition = rememberInfiniteTransition(label = "visualizer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(Modifier.fillMaxSize().background(Color(0xFF090909))) {
        Column(
            Modifier.fillMaxSize().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { vm.playerOpen = false }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Cerrar")
                }
                Text("REPRODUCIENDO")
                IconButton(onClick = { vm.lyricsOpen = true }) {
                    Icon(Icons.Default.Lyrics, contentDescription = "Letras")
                }
            }

            Spacer(Modifier.height(28.dp))

            Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                if (vm.visualizer) {
                    Canvas(Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2f - 12.dp.toPx()
                        for (i in 0 until 48) {
                            val angle = i.toFloat() / 48f * 6.28f
                            val wave = (sin(phase + i * 0.4f) + 1f) / 2f
                            val cs = cos(angle.toDouble()).toFloat()
                            val sn = sin(angle.toDouble()).toFloat()
                            val endRadius = radius + wave * 22f
                            drawLine(
                                color = vm.accent,
                                start = androidx.compose.ui.geometry.Offset(
                                    center.x + cs * radius,
                                    center.y + sn * radius
                                ),
                                end = androidx.compose.ui.geometry.Offset(
                                    center.x + cs * endRadius,
                                    center.y + sn * endRadius
                                ),
                                strokeWidth = 2.5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                Box(
                    Modifier.size(244.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF242424)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = vm.accent
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(t.title, style = MaterialTheme.typography.headlineSmall)
            Text(t.artist, color = Color.Gray)
            Spacer(Modifier.height(30.dp))

            LinearProgressIndicator(
                progress = { 0.42f },
                modifier = Modifier.fillMaxWidth(),
                color = vm.accent
            )

            Row {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior")
                }
                IconButton(onClick = vm::toggle) {
                    Icon(
                        if (vm.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        modifier = Modifier.size(38.dp)
                    )
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Siguiente")
                }
            }

            Row {
                IconButton(onClick = { vm.like(t) }) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Me gusta")
                }
                IconButton(onClick = { vm.lyricsOpen = true }) {
                    Icon(Icons.Default.Lyrics, contentDescription = "Letras")
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "Cola")
                }
            }
        }
    }
}

@Composable fun Lyrics(vm:MusicViewModel){Box(Modifier.fillMaxSize().background(Color(0xFF050505))){Column(Modifier.fillMaxSize().padding(22.dp)){IconButton({vm.lyricsOpen=false}){Icon(Icons.Default.Close,null)};Text("LETRAS",color=vm.accent);Spacer(Modifier.height(60.dp));listOf("Las luces se encienden","la noche empieza a respirar","déjame llevarte","un poco más allá","sin mirar atrás").forEachIndexed{i,s->Text(s,style=if(i==2)MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,color=if(i==2)Color.White else Color.White.copy(.35f),modifier=Modifier.padding(vertical=8.dp))}}}}
@Composable
fun DJ(vm: MusicViewModel) {
    val transition = rememberInfiniteTransition(label = "dj")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Box(Modifier.fillMaxSize().background(Color(0xFF080808))) {
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { vm.djOpen = false }) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
                Text("DJ FLOW", color = vm.accent)
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más")
                }
            }

            Spacer(Modifier.height(20.dp))

            Canvas(
                Modifier.size(170.dp).align(Alignment.CenterHorizontally)
            ) {
                val radius = size.minDimension * (0.36f + sin(pulse) * 0.04f)
                drawCircle(vm.accent.copy(alpha = 0.12f), radius)
                drawCircle(vm.accent, radius, style = Stroke(3.dp.toPx()))
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Estoy escuchando.",
                Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Hábleme como si fuera una llamada.",
                Modifier.align(Alignment.CenterHorizontally),
                color = Color.Gray
            )

            Spacer(Modifier.height(20.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151515))) {
                Text(vm.djMessage, Modifier.padding(18.dp))
            }

            Spacer(Modifier.weight(1f))

            OutlinedTextField(
                value = vm.djInput,
                onValueChange = { vm.djInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Escriba qué quiere escuchar") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = vm::askDj) {
                        Icon(Icons.Default.Send, contentDescription = "Enviar")
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = vm::askDj,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Hablar con DJ Flow")
            }
        }
    }
}
