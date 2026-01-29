package com.cosmic_struck.stellar.create_module.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TabSwitcherModelScreen(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    // Styling constants
    val containerColor = Color(0xFFF2F2F7) // Light gray background
    val selectedColor = Color.White
    val activeTextColor = Color.Black
    val inactiveTextColor = Color.Gray

    BoxWithConstraints(
        modifier = Modifier
            .padding(16.dp)
            .height(50.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(25.dp))
            .background(containerColor)
            .padding(4.dp) // Inner padding for the slider
    ) {
        val maxWidth = maxWidth
        val tabWidth = maxWidth / tabs.size

        // The Sliding Background Indicator
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedTab,
            animationSpec = tween(durationMillis = 250),
            label = "IndicatorOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(21.dp)) // Subtle elevation
                .clip(RoundedCornerShape(21.dp))
                .background(selectedColor)
        )

        // The Tab Text Labels
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // Remove default ripple for a cleaner look
                        ) {
                            onTabSelected(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (selectedTab == index) activeTextColor else inactiveTextColor,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}