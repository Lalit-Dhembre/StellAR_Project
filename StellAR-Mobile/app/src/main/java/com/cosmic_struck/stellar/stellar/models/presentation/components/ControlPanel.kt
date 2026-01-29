package com.cosmic_struck.stellar.stellar.models.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.common.util.Rajdhani
import com.cosmic_struck.stellar.stellar.models.presentation.viewmodel.SceneType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetControlPanel(
    scene: SceneType,
    rotationSpeed: Float,
    onRotationSpeedChange: (Float) -> Unit,
    onToggleScene: () -> Unit,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp) // Height increased slightly for padding
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E2130).copy(alpha = 0.9f),
                        Color(0xFF0B0D17).copy(alpha = 0.95f)
                    )
                ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .padding(top = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // 🔄 Rotation Speed
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ROTATION SPEED",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF),
                    fontFamily = Rajdhani,
                    letterSpacing = 1.sp
                )
                Slider(
                    value = rotationSpeed,
                    onValueChange = onRotationSpeedChange,
                    valueRange = 0f..3f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF7C4DFF),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }

            Spacer(modifier = Modifier.size(24.dp))

            // 🎛 Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Button(
                    onClick = onToggleScene,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C4DFF).copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = if (scene == SceneType.SceneView) "SWITCH TO AR" else "SWITCH TO 3D",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Rajdhani,
                        color = Color.White
                    )
                }
                
                // Reset Button option could be added here if needed, but wasn't in original logic usage
//                 Button(
//                    onClick = onReset,
//                    shape = RoundedCornerShape(12.dp),
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = Color.White.copy(alpha = 0.1f)
//                    ),
//                    modifier = Modifier.height(40.dp)
//                ) {
//                    Text(
//                        text = "RESET VIEW",
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Bold,
//                        fontFamily = Rajdhani,
//                        color = Color.White
//                    )
//                }
            }
        }
    }
}
