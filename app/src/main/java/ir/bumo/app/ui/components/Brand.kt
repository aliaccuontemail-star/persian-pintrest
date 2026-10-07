package ir.bumo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.bumo.app.ui.theme.BumoRed

@Composable fun BumoMark(modifier:Modifier=Modifier){Box(modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(BumoRed),contentAlignment=Alignment.Center){Text("B",color=androidx.compose.ui.graphics.Color.White,fontSize=24.sp,fontWeight=FontWeight.Black)}}
@Composable fun BumoBrand(){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){BumoMark();Text("بومو",fontSize=25.sp,fontWeight=FontWeight.Bold)}}
