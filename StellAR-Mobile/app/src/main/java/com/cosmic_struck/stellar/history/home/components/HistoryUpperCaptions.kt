package com.cosmic_struck.stellar.history.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.history.common.HistoryHomeCaption1
import com.cosmic_struck.stellar.history.common.HistoryHomeCaption2
import com.cosmic_struck.stellar.history.common.Rajdhani
import com.cosmic_struck.stellar.history.common.HistorySecondary

@Composable
fun HistoryUpperCaptions() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = HistoryHomeCaption1,
            style = TextStyle(
                fontSize = 32.sp,
                fontFamily = Rajdhani,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        )
        Text(
            text = HistoryHomeCaption2,
            style = TextStyle(
                fontSize = 16.sp,
                fontFamily = Rajdhani,
                fontWeight = FontWeight.Normal,
                color = HistorySecondary,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
