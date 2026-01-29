package com.cosmic_struck.stellar.stellar.models.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.common.util.Rajdhani
import com.cosmic_struck.stellar.ui.theme.Blue5

@Composable
fun ScoreCard(
    icon: @Composable ()-> Unit,
    title: String,
    score: String,
    modifier: Modifier = Modifier) {

    Card(
        modifier = modifier
            .height(160.dp), // Slightly reduced height
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Blue5.copy(alpha=0.5f),
                    Color.Transparent
                )
            )
        ),
        colors = cardColors(
            containerColor = Color.Transparent,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                     Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E2130).copy(alpha = 0.6f),
                            Color(0xFF0B0D17).copy(alpha = 0.4f)
                        )
                    )
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            icon()
            
            Text(
                text = score,
                color = Color.White,
                fontFamily = Rajdhani,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = title.uppercase(),
                color = Color.White.copy(alpha = 0.7f),
                fontFamily = Rajdhani,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
    }
}