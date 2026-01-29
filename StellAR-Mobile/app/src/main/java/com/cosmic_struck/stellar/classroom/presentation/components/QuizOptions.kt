package com.cosmic_struck.stellar.classroom.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuizOptions(
    onSelectAnswer: (String) -> Unit,
    options: List<String>,
    modifier: Modifier = Modifier
) {
    var selectedOption by remember(options) { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEachIndexed { index, option ->
            OptionButton(
                text = option,
                optionLabel = ('A' + index).toString(),
                isSelected = selectedOption == option,
                onClick = {
                    selectedOption = option
                    onSelectAnswer(option)
                }
            )
        }
    }
}

@Composable
fun OptionButton(
    text: String,
    optionLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF4a4a7e) else Color(0xFF2a2a4e),
        animationSpec = tween(200),
        label = "bg_color"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF7C4DFF) else Color(0xFF3a3a5e),
        animationSpec = tween(200),
        label = "border_color"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Option label (A, B, C, D)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color(0xFF7C4DFF) else Color(0xFF3a3a5e)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optionLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Option text
            Text(
                text = text,
                color = Color.White,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C4DFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}