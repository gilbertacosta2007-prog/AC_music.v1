package com.acmusic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.cos
import kotlin.math.sin

data class Track(val title:String,val artist:String,val url:String)
private val tracks=listOf(
 Track("Dreams","AC Music Demo","https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
 Track("Night Drive","AC Music Demo","https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
 Track("Afterglow","AC Music Demo","https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
 Track("Midnight City","AC Music Demo","https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3")
)
enum class Tab{HOME,SEARCH,LIKES,PLAYLISTS,SETTINGS}

class MusicViewModel:ViewModel(){
 var current by mutableStateOf<Track?>(null);var playing by mutableStateOf(false)
 var tab by mutableStateOf(Tab.HOME);var search by mutableStateOf("")
 var djText by mutableStateOf("");var djMessage by mutableStateOf("Mírame. Dígame qué quiere escuchar.")
 var likes by mutableStateOf(setOf<String>());var accent by mutableStateOf(Color(0xFFE53935))
 var visualizer by mutableStateOf(true);var dynamicBg by mutableStateOf(true);var autoColor by mutableStateOf(true)
 var player:ExoPlayer?=null;var fullPlayer by mutableStateOf(false);var djOpen by mutableStateOf(false);var lyrics by mutableStateOf(false)
 fun attach(p:ExoPlayer){player=p}
 fun play(t:Track){current=t;player?.setMediaItem(MediaItem.fromUri(t.url));player?.prepare();player?.play();playing=true}
 fun toggle(){player?.let{if(it.isPlaying){it.pause();playing=false}else{it.play();playing=true}}}
 fun like(t:Track){likes=if(t.title in likes)likes-t.title else likes+t.title}
 fun askDj(){if(djText.isBlank())return;val q=djText.lowercase();val t=when{q.contains("energ")||q.contains("gym")||q.contains("fiesta")->tracks[2];q.contains("relax")||q.contains("calma")->tracks[0];else->tracks.random()};djMessage=if(q.contains("sugarland"))"Oh. Mírame, esto está mejor que la música aburrida de Sugarland. Vamos con "+t.title+"." else "Ok. Ese mood está claro. Puse "+t.title+". Diablazo.";play(t);djText=""}
 fun list():List<Track>{return if(search.isBlank())tracks else tracks.filter{it.title.contains(search,true)||it.artist.contains(search,true)}}
}

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}

@Composable fun App(vm:MusicViewModel=viewModel()){
 val c=androidx.compose.ui.platform.LocalContext.current
 DisposableEffect(Unit){val p=ExoPlayer.Builder(c).build();vm.attach(p);onDispose{p.release()}}
 MaterialTheme(colorScheme=darkColorScheme(primary=vm.accent,background=Color(0xFF080808),surface=Color(0xFF141414))){
  Box(Modifier.fillMaxSize().background(Color(0xFF080808))){
   Column(Modifier.fillMaxSize()){
    when(vm.tab){Tab.HOME->Home(vm);Tab.SEARCH->Search(vm);Tab.LIKES->Likes(vm);Tab.PLAYLISTS->Playlists(vm);Tab.SETTINGS->Settings(vm)}
    vm.current?.let{Mini(it,vm)}
    NavigationBar(containerColor=Color(0xFF0B0B0B)){
     Nav(Tab.HOME,"Principal",Icons.Default.Home,vm);Nav(Tab.SEARCH,"Buscar",Icons.Default.Search,vm);Nav(Tab.LIKES,"Me gusta",Icons.Default.Favorite,vm);Nav(Tab.PLAYLISTS,"Playlists",Icons.Default.QueueMusic,vm);Nav(Tab.SETTINGS,"Ajustes",Icons.Default.Settings,vm)
    }
   }
   FloatingActionButton({vm.djOpen=true},Modifier.align(Alignment.BottomEnd).padding(end=18.dp,bottom=78.dp),containerColor=vm.accent){Icon(Icons.Default.Mic,"DJ Flow")}
   if(vm.fullPlayer)Player(vm);if(vm.djOpen)DJ(vm);if(vm.lyrics)Lyrics(vm)
  }
 }
}
@Composable fun Nav(t:Tab,s:String,i:androidx.compose.ui.graphics.vector.ImageVector,vm:MusicViewModel){NavigationBarItem(vm.tab==t,{vm.tab=t},{Icon(i,null)},{Text(s)})}

@Composable fun Home(vm:MusicViewModel){LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
 item{Spacer(Modifier.height(12.dp));Text("AC",color=vm.accent);Text("Music",style=MaterialTheme.typography.displaySmall);Text("Su música. Su ritmo. Su DJ.",color=Color.Gray)}
 item{Card(colors=CardDefaults.cardColors(Color(0xFF151515)),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("DJ Flow",style=MaterialTheme.typography.titleLarge);Text(vm.djMessage);Button({vm.djOpen=true},Modifier.fillMaxWidth()){Icon(Icons.Default.Call,null);Spacer(Modifier.width(8.dp));Text("Hablar con DJ Flow")}}}}
 item{Text("Para usted",style=MaterialTheme.typography.titleLarge)}
 items(vm.list()){Row(Modifier.fillMaxWidth().clickable{vm.play(it)}.padding(8.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF242424)),contentAlignment=Alignment.Center){Icon(Icons.Default.MusicNote,null,tint=vm.accent)};Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(it.title);Text(it.artist,color=Color.Gray)};IconButton({vm.like(it)}){Icon(if(it.title in vm.likes)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=if(it.title in vm.likes)vm.accent else Color.Gray)}}}
}}

@Composable fun Search(vm:MusicViewModel){Column(Modifier.fillMaxSize().padding(20.dp)){Spacer(Modifier.height(20.dp));Text("Buscar",style=MaterialTheme.typography.displaySmall);Spacer(Modifier.height(16.dp));OutlinedTextField(vm.search,{vm.search=it},Modifier.fillMaxWidth(),placeholder={Text("Canciones, artistas, playlists")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true);LazyColumn{items(vm.list()){Row(Modifier.fillMaxWidth().clickable{vm.play(it)}.padding(12.dp)){Column{Text(it.title);Text(it.artist,color=Color.Gray)}}}}}}

@Composable fun Likes(vm:MusicViewModel){val l=tracks.filter{it.title in vm.likes};Column(Modifier.fillMaxSize().padding(20.dp)){Spacer(Modifier.height(20.dp));Text("Me gusta",style=MaterialTheme.typography.displaySmall);if(l.isEmpty())Text("Todavía no hay canciones guardadas.",color=Color.Gray) else LazyColumn{items(l){Text(it.title,Modifier.fillMaxWidth().clickable{vm.play(it)}.padding(16.dp))}}}}
@Composable fun Playlists(vm:MusicViewModel){Column(Modifier.fillMaxSize().padding(20.dp)){Spacer(Modifier.height(20.dp));Text("Playlists",style=MaterialTheme.typography.displaySmall);listOf("Favoritas","Flow nocturno","Entrenamiento").forEach{Card(Modifier.fillMaxWidth().padding(vertical=6.dp),colors=CardDefaults.cardColors(Color(0xFF151515))){Text(it,Modifier.padding(20.dp))}}}}
@Composable fun Settings(vm:MusicViewModel){LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Spacer(Modifier.height(18.dp));Text("Ajustes",style=MaterialTheme.typography.displaySmall);Text("Personalice AC Music.",color=Color.Gray)};item{Text("APARIENCIA",color=Color.Gray)};item{SwitchRow("Fondo dinámico","Portada como ambiente",vm.dynamicBg){vm.dynamicBg=it}};item{SwitchRow("Color automático","Adaptar acento a la portada",vm.autoColor){vm.autoColor=it}};item{SwitchRow("Visualizador circular","Animación alrededor del álbum",vm.visualizer){vm.visualizer=it}};item{Card(colors=CardDefaults.cardColors(Color(0xFF151515))){Column(Modifier.padding(18.dp)){Text("Color de acento");Row(Modifier.padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){listOf(Color(0xFFE53935),Color(0xFF7C4DFF),Color(0xFF00BFA5),Color(0xFFFF9800)).forEach{Box(Modifier.size(38.dp).clip(CircleShape).background(it).clickable{vm.accent=it})}}}}};item{Text("AUDIO",color=Color.Gray)};item{Text("Normalización",Modifier.padding(12.dp))};item{Text("Ecualizador",Modifier.padding(12.dp))};item{Text("Caché persistente",Modifier.padding(12.dp))};item{Text("CUENTA",color=Color.Gray)};item{Text("YouTube Music — integración de fuente pendiente",Modifier.padding(12.dp))}}}
@Composable fun SwitchRow(a:String,b:String,v:Boolean,on:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(a);Text(b,color=Color.Gray)};Switch(v,on)}}

@Composable fun Mini(t:Track,vm:MusicViewModel){Surface(color=Color(0xFF161616)){Row(Modifier.fillMaxWidth().clickable{vm.fullPlayer=true}.padding(10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(t.title);Text(t.artist,color=Color.Gray)};IconButton(vm::toggle){Icon(if(vm.playing)Icons.Default.Pause else Icons.Default.PlayArrow,null)}}}}
@Composable fun Player(vm:MusicViewModel){val t=vm.current?:return;val tr=rememberInfiniteTransition(label="v");val p by tr.animateFloat(0f,6.28f,infiniteRepeatable(tween(1300,easing=LinearEasing)),label="p");Box(Modifier.fillMaxSize().background(Color(0xFF090909))){Column(Modifier.fillMaxSize().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){IconButton({vm.fullPlayer=false}){Icon(Icons.Default.KeyboardArrowDown,null)};Text("REPRODUCIENDO");IconButton({vm.lyrics=true}){Icon(Icons.Default.Lyrics,null)}};Spacer(Modifier.height(30.dp));Box(Modifier.size(300.dp),contentAlignment=Alignment.Center){if(vm.visualizer)Canvas(Modifier.fillMaxSize()){val r=size.minDimension/2-12.dp.toPx();for(i in 0 until 48){val a=i/48f*6.28f;val w=(sin(p+i*.4f)+1f)/2f;drawLine(vm.accent,center+androidx.compose.ui.geometry.Offset(cos(a)*r,sin(a)*r),center+androidx.compose.ui.geometry.Offset(cos(a)*(r+w*22),sin(a)*(r+w*22)),2.5f,StrokeCap.Round)}};Box(Modifier.size(244.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF242424)),contentAlignment=Alignment.Center){Icon(Icons.Default.MusicNote,null,Modifier.size(72.dp),tint=vm.accent)}};Spacer(Modifier.height(25.dp));Text(t.title,style=MaterialTheme.typography.headlineSmall);Text(t.artist,color=Color.Gray);Spacer(Modifier.height(35.dp));LinearProgressIndicator({.42f},Modifier.fillMaxWidth(),color=vm.accent);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("1:42");Text("4:03")};Row{IconButton({}){Icon(Icons.Default.SkipPrevious,null,Modifier.size(34.dp))};IconButton(vm::toggle){Icon(if(vm.playing)Icons.Default.Pause else Icons.Default.PlayArrow,null,Modifier.size(40.dp))};IconButton({}){Icon(Icons.Default.SkipNext,null,Modifier.size(34.dp))}};Row{IconButton({vm.like(t)}){Icon(Icons.Default.FavoriteBorder,null)};IconButton({vm.lyrics=true}){Icon(Icons.Default.Lyrics,null)};IconButton({}){Icon(Icons.Default.QueueMusic,null)}}}}}}
@Composable fun Lyrics(vm:MusicViewModel){Box(Modifier.fillMaxSize().background(Color(0xFF050505).copy(.97f))){Column(Modifier.fillMaxSize().padding(22.dp)){IconButton({vm.lyrics=false}){Icon(Icons.Default.Close,null)};Text("LETRAS",color=vm.accent);Spacer(Modifier.height(60.dp));listOf("Las luces se encienden","la noche empieza a respirar","déjame llevarte","un poco más allá","sin mirar atrás").forEachIndexed{i,s->Text(s,style=if(i==2)MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,fontWeight=if(i==2)androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,color=if(i==2)Color.White else Color.White.copy(.35f),Modifier.padding(vertical=8.dp))}}}}
@Composable fun DJ(vm:MusicViewModel){val tr=rememberInfiniteTransition(label="d");val p by tr.animateFloat(0f,6.28f,infiniteRepeatable(tween(1200),RepeatMode.Restart),label="p");Box(Modifier.fillMaxSize().background(Color(0xFF080808))){Column(Modifier.fillMaxSize().padding(22.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){IconButton({vm.djOpen=false}){Icon(Icons.Default.Close,null)};Text("DJ FLOW",color=vm.accent);IconButton({}){Icon(Icons.Default.MoreVert,null)}};Spacer(Modifier.height(25.dp));Canvas(Modifier.size(170.dp).align(Alignment.CenterHorizontally)){val r=size.minDimension*(.36f+sin(p)*.04f);drawCircle(vm.accent.copy(.12f),r);drawCircle(vm.accent,r,style=Stroke(3.dp.toPx()))};Spacer(Modifier.height(20.dp));Text("Estoy escuchando.",Modifier.align(Alignment.CenterHorizontally),style=MaterialTheme.typography.headlineSmall);Text("Hábleme como si fuera una llamada.",Modifier.align(Alignment.CenterHorizontally),color=Color.Gray);Spacer(Modifier.height(25.dp));Card(colors=CardDefaults.cardColors(Color(0xFF151515))){Text(vm.djMessage,Modifier.padding(18.dp))};Spacer(Modifier.weight(1f));OutlinedTextField(vm.djText,{vm.djText=it},Modifier.fillMaxWidth(),placeholder={Text("Escriba o dígame qué quiere")},singleLine=true,trailingIcon={IconButton(vm::askDj){Icon(Icons.Default.Send,null)}});Spacer(Modifier.height(12.dp));Button(vm::askDj,Modifier.fillMaxWidth().height(56.dp)){Icon(Icons.Default.Mic,null);Spacer(Modifier.width(8.dp));Text("Hablar con DJ Flow")}}}}
