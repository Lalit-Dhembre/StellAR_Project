package com.cosmic_struck.stellar.chemistry.arlab.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.chemistry.domain.model.ChemistryGameModel
import com.cosmic_struck.stellar.common.util.Rajdhani

@Composable
fun ChemistryGameCard(
    onClick: (String) -> Unit,
    gameModel: ChemistryGameModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clickable {
                onClick(gameModel.route)
            },
        colors = CardDefaults.cardColors(
            containerColor = gameModel.color,
            contentColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Image(
                painter = painterResource(gameModel.thumbnail),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.FillWidth
            )
            Spacer(
                modifier = Modifier
                    .height(10.dp)
            )

            Text(
                text = gameModel.title,
                modifier = Modifier
                    .padding(horizontal = 10.dp),
                fontFamily = Rajdhani,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = gameModel.description,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp),
                fontFamily = Rajdhani,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
