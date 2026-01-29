package com.cosmic_struck.stellar.stellar.scantext.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.common.util.Rajdhani
import com.cosmic_struck.stellar.common.util.ScanTextBookCaptions

@Composable
fun CameraScanCaption(modifier: Modifier = Modifier) {
    Text(
        text = ScanTextBookCaptions,
        textAlign = TextAlign.Center,
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        fontSize = 24.sp,
        modifier = modifier
    )
}