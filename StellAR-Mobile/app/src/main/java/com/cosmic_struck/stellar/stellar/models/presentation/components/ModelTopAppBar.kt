package com.cosmic_struck.stellar.stellar.models.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.common.util.Rajdhani

// Define colors to match the image
val GoldAccent = Color(0xFFFFD700)
val XPAccent = Color(0xFF00E5FF) // Cyan instead of faint purple for better contrast

@Composable
fun ModelTopAppBar(
    modifier: Modifier = Modifier,
    currentLevel: Int = 5,
    currentXP: Int = 1250,
    maxXP: Int = 1500,
    fontFamily: FontFamily = Rajdhani
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B0D17).copy(alpha = 0.95f),
                            Color(0xFF1E2130).copy(alpha = 0.85f)
                        )
                    )
                )
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Column{
                // Top Row: Title/Subtitle vs Level/XP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Side: Titles
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SPACE MODELS",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Collect and explore universe",
                            color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = fontFamily,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Right Side: Stats
                    Column(horizontalAlignment = Alignment.End) {
                        // Level Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.trophy), // Trophy Icon
                                contentDescription = "Level",
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LEVEL $currentLevel",
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = fontFamily,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // XP Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.spark), // Sparkles Icon
                                contentDescription = "XP",
                                tint = XPAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$currentXP XP",
                                color = XPAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Row: Progress Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progress to Level ${currentLevel + 1}".uppercase(),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontFamily = fontFamily,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$currentXP / $maxXP XP",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { currentXP.toFloat() / maxXP.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(0.5.dp, Color.White.copy(alpha=0.2f), RoundedCornerShape(4.dp)),
                    color = Color(0xFF7C4DFF), // Bright purple for progress
                    trackColor = Color(0xFF0D001A).copy(alpha = 0.5f), // Dark track
                    strokeCap = StrokeCap.Round,
                )
            }
        }
    }
}