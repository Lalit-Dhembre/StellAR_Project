package com.cosmic_struck.stellar.chemistry.models.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.chemistry.common.AtomicCyan
import com.cosmic_struck.stellar.chemistry.common.ChemistryPurple
import com.cosmic_struck.stellar.chemistry.domain.model.ChemistryModel
import com.cosmic_struck.stellar.common.util.Rajdhani
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage

// Rarity colors
private val chemistryRare = Color(0xFF00F5FF)
private val chemistryCommon = Color(0xFF4FC3F7)
private val chemistryLegendary = Color(0xFFFFD700)

@Composable
fun ChemistryModelCard(
    locked: Boolean,
    onClickModel: () -> Unit,
    model: ChemistryModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !locked) {
                if (!locked) {
                    onClickModel()
                } else {
                    Toast.makeText(context, "Locked!", Toast.LENGTH_SHORT).show()
                }
            }
            .alpha(if (locked) 0.6f else 1f)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        AtomicCyan.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1F3C).copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                CoilImage(
                    imageModel = { model.thumbnailUrl },
                    imageOptions = ImageOptions(contentScale = ContentScale.Crop),
                    modifier = Modifier.fillMaxSize()
                )

                // Rarity Badge Top Right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    ChemistryRarityBadge(rarity = model.rarity)
                }
            }

            // Bottom Content Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text = model.name,
                    color = Color.White,
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    fontFamily = Rajdhani,
                    fontSize = 18.sp,
                    maxLines = 1,
                    letterSpacing = 0.5.sp
                )

                // XP Value
                Text(
                    text = "${model.xpReward} XP",
                    color = AtomicCyan,
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    fontFamily = Rajdhani,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Action Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(ChemistryPurple, Color(0xFF1B263B))
                            )
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (locked) "LOCKED" else "VIEW 3D",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Rajdhani,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Locked Overlay
        if (locked) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_block_24),
                    contentDescription = "Locked",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun ChemistryRarityBadge(rarity: String) {
    val rarityColor = when {
        rarity.equals("Rare", ignoreCase = true) -> chemistryRare
        rarity.equals("Common", ignoreCase = true) -> chemistryCommon
        rarity.equals("Legendary", ignoreCase = true) -> chemistryLegendary
        else -> chemistryCommon
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color = rarityColor.copy(alpha = 0.9f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rarity.uppercase().take(1),
            color = Color.White,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            fontFamily = Rajdhani
        )
    }
}
