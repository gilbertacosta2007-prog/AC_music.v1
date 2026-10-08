package com.acmusic.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale
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
import androidx.compose.ui.graphics.asImageBitmap
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

data class Track(val title:String,val artist:String,val url:String,val isLocal:Boolean=false,val artworkUri:String?=null)
private val demo=listOf(
 Track("Dreams","AC Music Demo","https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
 Track("Night Drive","AC Music Demo","https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
 Track("Afterglow","AC Music Demo","https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
 Track("Midnight City","AC Music Demo","https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"))
enum class Tab{HOME,SEARCH,LIKES,PLAYLISTS,SETTINGS}
enum class SearchSource{ALL,PHONE,YOUTUBE}
enum class VisualizerStyle{CIRCLE,BARS,WAVES,SPECTRUM}

class MusicViewModel:ViewModel(){
 var current by mutableStateOf<Track?>(null);var playing by mutableStateOf(false);var tab by mutableStateOf(Tab.HOME);var searchSource by mutableStateOf(SearchSource.ALL)
 var search by mutableStateOf("");var djInput by mutableStateOf("");var djMessage by mutableStateOf("Mírame. Dígame qué quiere escuchar.")
 var likes by mutableStateOf(setOf<String>());var localTracks by mutableStateOf<List<Track>>(emptyList());var positionMs by mutableStateOf(0L);var durationMs by mutableStateOf(0L);var shuffle by mutableStateOf(false);var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF);var accent by mutableStateOf(Color(0xFFE53935));var visualizer by mutableStateOf(true)
 var player:ExoPlayer?=null;var playerOpen by mutableStateOf(false);var djOpen by mutableStateOf(false);var lyricsOpen by mutableStateOf(false);var youtubeOpen by mutableStateOf(false);var youtubeUrl by mutableStateOf("https://music.youtube.com/");var background by mutableStateOf(Color(0xFF080808));var cardColor by mutableStateOf(Color(0xFF151515));var opacity by mutableFloatStateOf(1f);var visualizerStyle by mutableStateOf(VisualizerStyle.CIRCLE);var visualizerIntensity by mutableFloatStateOf(.65f);var visualizerSpeed by mutableFloatStateOf(1f)
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
 fun askDj(input:String=djInput):String{if(input.isBlank())return djMessage;val q=input.lowercase();val t=when{q.contains("energ")||q.contains("gym")||q.contains("fiesta")->demo[2];q.contains("relax")||q.contains("calma")->demo[0];else->demo.random()};djMessage=if(q.contains("sugarland"))"Oh. Mírame, esto sí está mejor que la música aburrida de Sugarland. Vamos con ${t.title}." else if(t.isLocal) "Oh. Encontré esa canción en su teléfono. Puse ${t.title}. Diablazo." else "Ok. Ese mood está claro. Puse ${t.title}. Diablazo.";play(t);djInput="";return djMessage}
 fun filtered():List<Track>{val base=when(searchSource){SearchSource.ALL->allTracks();SearchSource.PHONE->localTracks;SearchSource.YOUTUBE->emptyList()};return if(search.isBlank())base else base.filter{it.title.contains(search,true)||it.artist.contains(search,true)}}
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
 val micPermission=Manifest.permission.RECORD_AUDIO
 var micGranted by remember{mutableStateOf(ContextCompat.checkSelfPermission(ctx,micPermission)==PackageManager.PERMISSION_GRANTED)}
 val micLauncher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->micGranted=granted}
 LaunchedEffect(Unit){
  repeat(40){
   PlaybackService.player?.let{vm.attach(it);return@LaunchedEffect}
   delay(100)
  }
 }
 LaunchedEffect(permissionGranted){if(permissionGranted)vm.refreshLocal(ctx)}
 LaunchedEffect(vm.playing){
  while(true){
   vm.updateProgress()
   delay(500)
  }
 }
 MaterialTheme(colorScheme=darkColorScheme(primary=vm.accent,background=vm.background,surface=vm.cardColor)){
  Box(Modifier.fillMaxSize().background(vm.background)){
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
   if(vm.djOpen)DJ(vm,micGranted){micLauncher.launch(micPermission)}
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

@Composable fun Home(vm:MusicViewModel,permissionGranted:Boolean,requestPermission:()->Unit){LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){item{Spacer(Modifier.height(16.dp));Text("AC",color=vm.accent);Text("Music",style=MaterialTheme.typography.displaySmall);Text("Su música. Su ritmo. Su DJ.",color=Color.Gray)};item{Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF151515)),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(20.dp)){Text("DJ Flow",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(8.dp));Text(vm.djMessage);Spacer(Modifier.height(12.dp));Button({vm.djOpen=true},Modifier.fillMaxWidth()){Icon(Icons.Default.Call,null);Spacer(Modifier.width(8.dp));Text("Hablar con DJ Flow")}}}};item{LocalLibraryCard(vm,permissionGranted,requestPermission)};item{Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF151515)),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(20.dp)){Text("YouTube Music",style=MaterialTheme.typography.titleLarge);Text("Busque y reproduzca música desde YouTube Music dentro de AC Music.",color=Color.Gray);Spacer(Modifier.height(12.dp));Button({vm.openYouTubeSearch("")},Modifier.fillMaxWidth()){Icon(Icons.Default.Language,null);Spacer(Modifier.width(8.dp));Text("Abrir YouTube Music")}}}};item{Text("Para usted",style=MaterialTheme.typography.titleLarge)};items(vm.filtered()){TrackRow(it,vm)}}}
@Composable fun TrackRow(t:Track,vm:MusicViewModel){Row(Modifier.fillMaxWidth().clickable{vm.play(t)}.padding(10.dp),verticalAlignment=Alignment.CenterVertically){Artwork(t,56.dp,vm);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(t.title);Text(t.artist,color=Color.Gray)};IconButton({vm.like(t)}){Icon(if(t.title in vm.likes)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=if(t.title in vm.likes)vm.accent else Color.Gray)}}}
@Composable fun Search(vm:MusicViewModel){
 Column(Modifier.fillMaxSize().padding(20.dp)){
  Spacer(Modifier.height(20.dp));Text("Buscar",style=MaterialTheme.typography.displaySmall);Text("Teléfono y YouTube Music",color=Color.Gray);Spacer(Modifier.height(12.dp))
  OutlinedTextField(vm.search,{vm.search=it},Modifier.fillMaxWidth(),placeholder={Text("Canciones, artistas, playlists")},leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={IconButton({if(vm.search.isNotBlank())vm.openYouTubeSearch(vm.search)}){Icon(Icons.Default.Language,"YouTube Music")}},singleLine=true)
  Spacer(Modifier.height(10.dp))
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   FilterChip(vm.searchSource==SearchSource.ALL,{vm.searchSource=SearchSource.ALL},label={Text("Todo")})
   FilterChip(vm.searchSource==SearchSource.PHONE,{vm.searchSource=SearchSource.PHONE},label={Text("Teléfono")})
   FilterChip(vm.searchSource==SearchSource.YOUTUBE,{vm.searchSource=SearchSource.YOUTUBE},label={Text("YouTube Music")})
  }
  Spacer(Modifier.height(8.dp))
  if(vm.searchSource==SearchSource.YOUTUBE){
   Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF151515)),shape=RoundedCornerShape(22.dp)){
    Column(Modifier.padding(18.dp)){Text("YouTube Music",style=MaterialTheme.typography.titleMedium);Text("La búsqueda se abre en la experiencia oficial de YouTube Music dentro de AC Music.",color=Color.Gray);Spacer(Modifier.height(12.dp));Button({vm.openYouTubeSearch(vm.search)},Modifier.fillMaxWidth()){Text("Buscar en YouTube Music")}}
   }
  } else LazyColumn{items(vm.filtered()){TrackRow(it,vm)}}
 }
}
@Composable fun Likes(vm:MusicViewModel){val l=vm.allTracks().filter{it.url in vm.likes};Column(Modifier.fillMaxSize().padding(20.dp)){Spacer(Modifier.height(20.dp));Text("Me gusta",style=MaterialTheme.typography.displaySmall);if(l.isEmpty())Text("Todavía no hay canciones guardadas.",color=Color.Gray)else LazyColumn{items(l){TrackRow(it,vm)}}}}
@Composable fun Playlists(vm:MusicViewModel){Column(Modifier.fillMaxSize().padding(20.dp)){Spacer(Modifier.height(20.dp));Text("Playlists",style=MaterialTheme.typography.displaySmall);listOf("Favoritas","Flow nocturno","Entrenamiento").forEach{Card(Modifier.fillMaxWidth().padding(vertical=6.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF151515))){Text(it,Modifier.padding(20.dp))}}}}
@Composable
fun Settings(vm:MusicViewModel,permissionGranted:Boolean,requestPermission:()->Unit){
 val context=LocalContext.current
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Spacer(Modifier.height(20.dp));Text("Ajustes",style=MaterialTheme.typography.displaySmall);Text("Personalice el reproductor a su gusto.",color=Color.Gray)}
  item{Text("BIBLIOTECA LOCAL",color=Color.Gray)}
  item{Text(if(permissionGranted) vm.localTracks.size.toString()+" canciones locales disponibles" else "Active el acceso para reproducir las canciones descargadas en el teléfono",color=Color.Gray)}
  item{Button(if(permissionGranted){{vm.refreshLocal(context)}}else requestPermission,Modifier.fillMaxWidth()){Text(if(permissionGranted)"Actualizar biblioteca" else "Dar acceso a la música")}}
  item{Text("APARIENCIA",color=Color.Gray)}
  item{SwitchRow("Visualizador","Animación alrededor de la portada",vm.visualizer){vm.visualizer=it}}
  item{Text("Intensidad del visualizador")}
  item{Slider(vm.visualizerIntensity,{vm.visualizerIntensity=it},valueRange=0f..1f)}
  item{Text("Velocidad")}
  item{Slider(vm.visualizerSpeed,{vm.visualizerSpeed=it},valueRange=.2f..2f)}
  item{Text("Estilo del visualizador")}
  item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){VisualizerStyle.values().forEach{style->FilterChip(vm.visualizerStyle==style,{vm.visualizerStyle=style},label={Text(style.name.lowercase().replaceFirstChar{it.uppercase()})})}}}
  item{SwitchRow("Reproducción aleatoria","Mezclar la cola",vm.shuffle){vm.toggleShuffle()}}
  item{Text("Color de acento")}
  item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){listOf(Color(0xFFE53935),Color(0xFF7C4DFF),Color(0xFF00BFA5),Color(0xFFFF9800),Color(0xFF42A5F5)).forEach{c->Box(Modifier.size(38.dp).clip(CircleShape).background(c).clickable{vm.accent=c})}}}
  item{Text("Fondo")}
  item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){listOf(Color(0xFF080808),Color(0xFF111827),Color(0xFF160B16),Color(0xFF050505)).forEach{c->Box(Modifier.size(38.dp).clip(CircleShape).background(c).clickable{vm.background=c})}}}
  item{Text("Transparencia de tarjetas")}
  item{Slider(vm.opacity,{vm.opacity=it},valueRange=.65f..1f)}
  item{Text("AUDIO",color=Color.Gray)}
  item{Card(colors=CardDefaults.cardColors(containerColor=vm.cardColor),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(18.dp)){Text("Normalización");Text("Mantenga el volumen más uniforme entre canciones.",color=Color.Gray);Spacer(Modifier.height(8.dp));Text("Ecualizador");Text("La integración avanzada del ecualizador se deja preparada para una futura versión.",color=Color.Gray)}}}
  item{Text("YOUTUBE MUSIC",color=Color.Gray)}
  item{Card(colors=CardDefaults.cardColors(containerColor=vm.cardColor),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(18.dp)){Text("Fuente online oficial",style=MaterialTheme.typography.titleMedium);Text("Busque y reproduzca música desde YouTube Music dentro de AC Music, sin extraer ni convertir su audio.",color=Color.Gray);Spacer(Modifier.height(10.dp));Button({vm.openYouTubeSearch("")},Modifier.fillMaxWidth()){Text("Abrir YouTube Music")}}}}
 }
}
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
                progress = { if(vm.durationMs>0) (vm.positionMs.toFloat()/vm.durationMs.toFloat()).coerceIn(0f,1f) else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = vm.accent
            )

            Row {
                IconButton(onClick = vm::previous) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior")
                }
                IconButton(onClick = vm::toggle) {
                    Icon(
                        if (vm.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        modifier = Modifier.size(38.dp)
                    )
                }
                IconButton(onClick = vm::next) {
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
fun DJ(vm:MusicViewModel,micGranted:Boolean,requestMic:()->Unit){
 val context=LocalContext.current
 var listening by remember{mutableStateOf(false)}
 val tts=remember{TextToSpeech(context){}}
 DisposableEffect(Unit){onDispose{tts.stop();tts.shutdown()}}
 fun speak(text:String){tts.language=Locale("es","ES");tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"dj-flow")}
 fun listen(){
  if(!micGranted){requestMic();return}
  if(!SpeechRecognizer.isRecognitionAvailable(context))return
  val recognizer=SpeechRecognizer.createSpeechRecognizer(context)
  recognizer.setRecognitionListener(object:RecognitionListener{
   override fun onReadyForSpeech(params:Bundle?){listening=true}
   override fun onBeginningOfSpeech(){}
   override fun onRmsChanged(rmsdB:Float){}
   override fun onBufferReceived(buffer:ByteArray?){}
   override fun onEndOfSpeech(){listening=false}
   override fun onError(error:Int){listening=false;recognizer.destroy()}
   override fun onResults(results:Bundle?){
    listening=false
    val words=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
    if(words.isNotBlank())speak(vm.askDj(words))
    recognizer.destroy()
   }
   override fun onPartialResults(partialResults:Bundle?){}
   override fun onEvent(eventType:Int,params:Bundle?){}
  })
  recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
   putExtra(RecognizerIntent.EXTRA_LANGUAGE,"es-ES")
  })
 }
 Box(Modifier.fillMaxSize().background(Color(0xFF080808))){
  Column(Modifier.fillMaxSize().padding(22.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    IconButton({vm.djOpen=false}){Icon(Icons.Default.Close,"Cerrar")}
    Text("DJ FLOW",color=vm.accent)
    IconButton({}){Icon(Icons.Default.MoreVert,"Más")}
   }
   Spacer(Modifier.height(20.dp))
   Text("Mírame.",style=MaterialTheme.typography.displaySmall,modifier=Modifier.align(Alignment.CenterHorizontally))
   Text(if(listening)"Estoy escuchando…" else "Hábleme como si fuera una llamada.",modifier=Modifier.align(Alignment.CenterHorizontally),color=Color.Gray)
   Spacer(Modifier.height(20.dp))
   Box(Modifier.size(190.dp).align(Alignment.CenterHorizontally),contentAlignment=Alignment.Center){
    val tr=rememberInfiniteTransition(label="djvoice")
    val p by tr.animateFloat(.82f,1.12f,infiniteRepeatable(tween(800),RepeatMode.Reverse),label="pulse")
    Canvas(Modifier.fillMaxSize()){drawCircle(vm.accent.copy(alpha=.12f),size.minDimension*.42f*p);drawCircle(vm.accent,size.minDimension*.32f*p,style=Stroke(3.dp.toPx()))}
    Icon(if(listening)Icons.Default.Stop else Icons.Default.Mic,null,tint=vm.accent,modifier=Modifier.size(56.dp))
   }
   Spacer(Modifier.height(20.dp))
   Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF151515)),shape=RoundedCornerShape(24.dp)){Text(vm.djMessage,Modifier.padding(20.dp))}
   Spacer(Modifier.weight(1f))
   OutlinedTextField(vm.djInput,{vm.djInput=it},Modifier.fillMaxWidth(),placeholder={Text("También puede escribirle")},trailingIcon={IconButton({speak(vm.askDj())}){Icon(Icons.Default.Send,"Enviar")}},singleLine=true)
   Spacer(Modifier.height(14.dp))
   FilledIconButton({listen()},Modifier.size(78.dp).align(Alignment.CenterHorizontally),colors=IconButtonDefaults.filledIconButtonColors(containerColor=vm.accent)){Icon(if(listening)Icons.Default.Stop else Icons.Default.Mic,null,modifier=Modifier.size(34.dp))}
  }
 }
}

@Composable
fun YouTubeMusic(vm:MusicViewModel){
 Box(Modifier.fillMaxSize().background(Color.Black)){
  AndroidView(
   modifier=Modifier.fillMaxSize(),
   factory={context->
    WebView(context).apply{
     layoutParams=ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
     webViewClient=WebViewClient()
     settings.javaScriptEnabled=true
     settings.domStorageEnabled=true
     settings.mediaPlaybackRequiresUserGesture=false
     loadUrl(vm.youtubeUrl)
    }
   },
   update={web->if(web.url!=vm.youtubeUrl)web.loadUrl(vm.youtubeUrl)}
  )
  Surface(Modifier.align(Alignment.TopCenter).padding(10.dp),color=Color.Black.copy(alpha=.72f),shape=RoundedCornerShape(18.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){
    IconButton({vm.youtubeOpen=false}){Icon(Icons.Default.Close,"Cerrar",tint=Color.White)}
    Text("YouTube Music",color=Color.White)
   }
  }
 }
}


@Composable
fun Artwork(t:Track,size:androidx.compose.ui.unit.Dp,vm:MusicViewModel){
 val context=LocalContext.current
 var bitmap by remember(t.artworkUri){mutableStateOf<android.graphics.Bitmap?>(null)}
 LaunchedEffect(t.artworkUri){
  bitmap=t.artworkUri?.let{uri->runCatching{context.contentResolver.openInputStream(Uri.parse(uri)).use{stream->android.graphics.BitmapFactory.decodeStream(stream)}}.getOrNull()}
 }
 Box(Modifier.size(size).clip(RoundedCornerShape(24.dp)).background(Color(0xFF242424)),contentAlignment=Alignment.Center){
  bitmap?.let{androidx.compose.foundation.Image(it.asImageBitmap(),null,Modifier.fillMaxSize())}
   ?: Icon(Icons.Default.MusicNote,null,modifier=Modifier.size(size*.28f),tint=vm.accent)
 }
}
