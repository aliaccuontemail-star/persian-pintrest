package ir.bumo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BumoRed=Color(0xFFE95B64)
val BumoRedDark=Color(0xFFFF7A82)
val BumoInk=Color(0xFF101116)
val BumoSurface=Color(0xFF171923)
val BumoSurface2=Color(0xFF1D202A)
@Composable fun BumoTheme(darkTheme:Boolean=isSystemInDarkTheme(),content: @Composable () -> Unit){val dark=darkTheme;val scheme=if(dark)darkColorScheme(primary=BumoRedDark,onPrimary=Color.White,background=BumoInk,surface=BumoSurface,surfaceVariant=BumoSurface2) else lightColorScheme(primary=BumoRed,background=Color(0xFFF7F7F9),surface=Color.White);MaterialTheme(colorScheme=scheme,typography=Typography(),content=content)}
