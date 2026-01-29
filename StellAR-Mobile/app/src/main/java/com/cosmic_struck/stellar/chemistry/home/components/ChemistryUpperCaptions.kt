package com.cosmic_struck.stellar.chemistry.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.chemistry.common.ChemistryHomeCaption
import com.cosmic_struck.stellar.chemistry.common.AtomicCyan
import com.cosmic_struck.stellar.common.util.Rajdhani

@Composable
fun ChemistryUpperCaptions(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing Title with chemistry emoji
        Text(
            text = "⚗️ ChemLab",
            style = TextStyle(
                shadow = Shadow(
                    color = AtomicCyan,
                    offset = Offset(0f, 0f),
                    blurRadius = 32f
                )
            ),
            color = Color.White,
            fontFamily = Rajdhani,
            fontWeight = FontWeight.Bold,
            fontSize = 56.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Subtitle/Description
        Text(
            text = ChemistryHomeCaption,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.8f),
            fontFamily = Rajdhani,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 24.sp
        )
    }
}
