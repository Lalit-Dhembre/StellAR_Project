package com.cosmic_struck.stellar.physics.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.physics.common.AtomCyan
import com.cosmic_struck.stellar.physics.common.AtomPurple

@Composable
fun PhysicsScanButton(
    navigateToScanText: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .shadow(20.dp, CircleShape, spotColor = AtomCyan)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(AtomPurple, AtomCyan)
                )
            )
            .clickable { navigateToScanText() }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.scan), // Assuming same icon
            contentDescription = "Scan",
            tint = Color.White,
            modifier = Modifier.fillMaxSize()
        )
    }
}
