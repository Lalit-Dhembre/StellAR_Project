package com.cosmic_struck.stellar.common.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.common.util.Rajdhani
import androidx.compose.ui.graphics.SolidColor

// Space Theme Colors
private val SpaceDark = Color(0xFF1E2130).copy(alpha = 0.5f)
private val NeonBlue = Color(0xFF00E5FF)
private val NeonPurple = Color(0xFF7C4DFF)

@Composable
fun TabSwitcher(
    nonActiveTextColor: Color = Color.White.copy(alpha = 0.6f),
    activeTextColor: Color = Color.White,
    modifier: Modifier = Modifier,
    options: List<String> = listOf("My Collection", "Discover"), // Updated defaults to match context
    initialIndex: Int = 0,
    onOptionSelected: (Int) -> Unit = {},
    fontFamily: FontFamily = Rajdhani
) {
    var selectedIndex by remember { mutableIntStateOf(initialIndex) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SpaceDark)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, text ->
                val isSelected = index == selectedIndex

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = if (isSelected)
                                Brush.horizontalGradient(
                                    colors = listOf(NeonPurple.copy(alpha=0.6f), NeonBlue.copy(alpha=0.6f))
                                )
                            else SolidColor(Color.Transparent)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedIndex = index
                            onOptionSelected(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        color = if (isSelected) activeTextColor else nonActiveTextColor,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontFamily = fontFamily,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}