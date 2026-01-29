package com.cosmic_struck.stellar.stellar.arlab.universe_lab.presentation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.R

@Composable
fun UniverseLabControlPanel(
    isPlaying: Boolean,
    timeScale: Float,
    simulationTime: Float,
    onPlayPauseClick: () -> Unit,
    onResetClick: () -> Unit,
    onTimeScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E2130).copy(alpha = 0.8f),
                        Color(0xFF0B0D17).copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Simulation Time Display
        Text(
            text = "Time: ${String.format("%.1f", simulationTime)}s",
            color = Color(0xFF00E5FF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Play/Pause and Reset Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Button
            IconButton(
                onClick = onResetClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.erase),
                    contentDescription = "Reset",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            // Play/Pause Button
            Button(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(64.dp)
                    .border(2.dp, if (isPlaying) Color(0xFFFF5252) else Color(0xFF00E5FF), CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPlaying) R.drawable.pause_circle_10435926 
                             else R.drawable.spark
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) Color(0xFFFF5252) else Color(0xFF00E5FF),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            // Placeholder to balance Reset button? or remove spacer and center properly.
            // Using Box with width 48dp to balance.
            Box(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Time Scale Slider
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Text(
                    text = "${String.format("%.1f", timeScale)}x",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Slider(
                value = timeScale,
                onValueChange = onTimeScaleChange,
                valueRange = 0.1f..5f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF7C4DFF),
                    activeTrackColor = Color(0xFF7C4DFF),
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )

            // Speed presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(0.5f, 1f, 2f, 5f).forEach { speed ->
                    Button(
                        onClick = { onTimeScaleChange(speed) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timeScale == speed) 
                                Color(0xFF7C4DFF) else Color.White.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f)
                            .padding(4.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (timeScale == speed) Color.Transparent else Color.White.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = "${speed}x",
                            color = if (timeScale == speed) Color.White else Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
