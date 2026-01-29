package com.cosmic_struck.stellar.stellar.models.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cosmic_struck.stellar.R

@Composable
fun ScoreRow(modifier: Modifier = Modifier) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScoreCard(
            modifier = Modifier
                .weight(1f),
            icon = {

                Icon(
                    painter = painterResource(R.drawable.checkmark),
                    contentDescription = null,
                    tint = Color.Green,
                    modifier = Modifier
                        .size(50.dp)
                )
            },
            title = "Downloaded",
            score = "3"
        )
        ScoreCard(
            modifier = Modifier
                .weight(1f),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.material_symbols_target),
                    contentDescription = null,
                    tint = Color.Blue,
                    modifier = Modifier
                        .size(50.dp)
                )
            },
            title = "Available",
            score = "3"
        )
        ScoreCard(
            modifier = Modifier
                .weight(1f),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.medal),
                    contentDescription = null,
                    tint = Color.Yellow,
                    modifier = Modifier
                        .size(50.dp)
                )
            },
            title = "Total XP",
            score = "350"
        )
    }
}