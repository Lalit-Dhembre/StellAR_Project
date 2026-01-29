package com.cosmic_struck.stellar.stellar.models.presentation.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.cosmic_struck.stellar.classroom.data.dto.ClassroomModel
import com.cosmic_struck.stellar.common.util.Rajdhani
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage

val rare = Color(0xFFAD46FF)
val common = Color(0xFFFF5252)
val secondary = Color(0xFFFFC107)

@Composable
fun PlanetCard(
    locked: Boolean,
    navigateToModelViewer: (String, String) -> Unit,
    planet: ClassroomModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Vertical Glassmorphic Card for Grid
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !locked) {
                if (!locked) {
                    navigateToModelViewer(planet.model_url, planet.model_name)
                } else {
                    Toast.makeText(context, "Locked!", Toast.LENGTH_SHORT).show()
                }
            }
            .alpha(if (locked) 0.6f else 1f)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2130).copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp) // Fixed height for image area
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                CoilImage(
                    imageModel = { planet.model_thumbnail },
                    imageOptions = ImageOptions(contentScale = ContentScale.Crop),
                    modifier = Modifier.fillMaxSize()
                )
                
                // Rarity Badge Top Right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    RarityBadge(rarity = planet.rarity)
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
                    text = planet.model_name,
                    color = Color.White,
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    fontFamily = Rajdhani,
                    fontSize = 18.sp,
                    maxLines = 1,
                    letterSpacing = 0.5.sp
                )

                // XP Value
                Text(
                    text = "${planet.xp_reward} XP",
                    color = Color(0xFF00E5FF),
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    fontFamily = Rajdhani,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))
                
                // Action Button (View) - Centered or full width?
                // Let's make a small "View" button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0061FC), Color(0xFF2979FF))
                            )
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if(locked) "LOCKED" else "VIEW 3D",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Rajdhani,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
        
        // Locked Overlay (Full Cover)
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
private fun RarityBadge(rarity: String) {
    val rarityColor = when {
        rarity.equals("Rare", ignoreCase = true) -> rare
        rarity.equals("Common", ignoreCase = true) -> common
        else -> secondary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color = rarityColor.copy(alpha = 0.9f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rarity.uppercase().take(1), // Just 'R', 'C', or 'L' for compact look? Or full text if fits. 
            // 'Rare' fits.
//            text = rarity,
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