package ir.bumo.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import kotlin.math.max
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ir.bumo.app.data.remote.Pin

fun imageUrl(base:String,path:String)=if(path.startsWith("http"))path else base.trimEnd('/')+"/"+path.trimStart('/')
@Composable fun PinCard(pin:Pin,base:String,onClick:()->Unit){Card(modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable{onClick()},shape=RoundedCornerShape(16.dp),elevation=CardDefaults.cardElevation(defaultElevation=2.dp)){Column{AsyncImage(model=imageUrl(base,pin.image_url),contentDescription=pin.title,modifier=Modifier.fillMaxWidth().aspectRatio(max(0.35f,pin.width.toFloat()/pin.height.coerceAtLeast(1).toFloat())),contentScale=ContentScale.Crop);Column(modifier=Modifier.padding(10.dp)){Text(pin.title,maxLines=2,style=MaterialTheme.typography.titleSmall);Text(pin.author?.display_name.orEmpty(),maxLines=1,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
