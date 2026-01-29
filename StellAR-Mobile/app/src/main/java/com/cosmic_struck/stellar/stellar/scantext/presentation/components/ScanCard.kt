package com.cosmic_struck.stellar.stellar.scantext.presentation.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

@Composable
fun ScanCard(
    info1: String,
    info2: String,
    info3: String,
    title: String,
    badge:String,
    description: String,
    image: String,
    modifier: Modifier = Modifier
) {
    // Image Container with Loading and Error Handling
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
            .background(Color(0xFF1A1A1A))
    ) {
        if (image.isNotEmpty()) {
            val asyncImageState = rememberAsyncImagePainter(
                model = image
            )
            AsyncImage(
                model = image,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = {it->
                    when(it){
                        is AsyncImagePainter.State.Loading -> {
                            Log.d("LOADING IMAGE",title)
                        }
                        is AsyncImagePainter.State.Success -> {
                            Log.d("SUCCESS IMAGE",title)
                        }
                        is AsyncImagePainter.State.Error -> {
                            Log.d("ERROR IMAGE",title + it.result.throwable.toString()+it.result.request.toString())

                        }
                        is AsyncImagePainter.State.Empty -> {
                            Log.d("EMPTY IMAGE",title)
                        }
                    }
                }
            )
        } else {
            // Placeholder when no image URL
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "📷",
                    fontSize = 60.sp
                )
            }
        }

        // Badge - Only show if not "Data unavailable"
        if (!info1.contains("unavailable", ignoreCase = true)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(
                        color = Color(0xFFAB47BC),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    badge,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }


    // Planet Info Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Planet Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "✨",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Facts - Only show if not "Data unavailable"
            if (!info1.contains("unavailable", ignoreCase = true)) {
                PlanetInfoRow(icon = "💠", label = info1)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!info2.contains("unavailable", ignoreCase = true)) {
                PlanetInfoRow(icon = "💠", label = info2)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!info3.contains("unavailable", ignoreCase = true)) {
                PlanetInfoRow(icon = "💠", label = info3)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun PlanetInfoRow(
    icon: String,
    label: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = Color(0xFFF3E5F5),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            fontSize = 13.sp,
            color = Color(0xFF424242),
            fontWeight = FontWeight.Medium,
            lineHeight = 16.sp
        )
    }
}