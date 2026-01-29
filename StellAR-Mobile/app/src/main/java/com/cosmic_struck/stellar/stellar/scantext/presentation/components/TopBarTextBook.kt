package com.cosmic_struck.stellar.stellar.scantext.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.common.util.Rajdhani

@Composable
fun TopBarScanTextBook(
    title: String,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        IconButton(
            onClick = {
                navigateBack()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
        ) {
            Icon(
                painter = painterResource(R.drawable.cross),
                contentDescription = null,
                tint = Color.White
            )
        }

        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            fontFamily = Rajdhani,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}