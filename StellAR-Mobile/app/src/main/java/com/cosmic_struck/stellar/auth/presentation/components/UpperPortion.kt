package com.cosmic_struck.stellar.auth.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.common.util.Rajdhani

@Composable
fun UpperPortion(modifier: Modifier = Modifier) {

    Box(
        modifier = modifier
    ){
        Text(
            text = "StellAR",
            fontFamily = Rajdhani,
            fontWeight = FontWeight.Bold,
            fontSize = 64.sp,
            color = Color.White
        )
    }
}