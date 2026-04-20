package com.cosmic_struck.stellar.stellar.pdfar.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cosmic_struck.stellar.stellar.pdfar.data.models.Concept
import com.cosmic_struck.stellar.stellar.pdfar.data.models.NativeImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionListScreen(
    concepts: List<Concept>,
    nativeImages: List<NativeImage>,
    onConceptClick: (String, String) -> Unit,
    onNativeImageClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Beautiful dark cosmic background
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D0B14),
            Color(0xFF1B172E)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Extraction Results",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose to generate a 3D model from AI Concepts or Native PDF Diagrams.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // --- AI Concepts Segment ---
            if (concepts.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Concepts",
                            tint = Color(0xFFA5B4FC), // Indigo-200
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generated AI Concepts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5B4FC)
                        )
                    }
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(concepts) { concept ->
                            ConceptCard(
                                concept = concept,
                                onClick = { onConceptClick(concept.id, concept.title) }
                            )
                        }
                    }
                }
            }

            // --- Native PDF Diagrams Segment ---
            if (nativeImages.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Native Images",
                            tint = Color(0xFFFCA5A5), // Red-200
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Original PDF Diagrams",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }

                items(nativeImages) { nativeImg ->
                    NativeImageCard(
                        nativeImage = nativeImg,
                        onClick = { onNativeImageClick(nativeImg.imageUrl, nativeImg.title) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
            
            if (concepts.isEmpty() && nativeImages.isEmpty()) {
                item {
                     Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                     ) {
                         Text(
                             text = "No content could be extracted.",
                             color = Color.White.copy(alpha = 0.5f)
                         )
                     }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConceptCard(
    concept: Concept,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(280.dp)
            .height(220.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (!concept.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = concept.imageUrl,
                        contentDescription = concept.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay to make text pop
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                    startY = 100f
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2D2A42)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Reference Image",
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
                
                // Title superimposed nicely at bottom of image
                Text(
                    text = concept.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Add the new beautiful visual identity badge
                SourceBadge(
                    source = concept.source ?: "unknown",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )
            }
            
            // Bottom bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Match Score: ${((concept.score ?: 0f) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFA5B4FC)
                )
                Text(
                    text = "GENERATE 3D ➔",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeImageCard(
    nativeImage: NativeImage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image Left
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = nativeImage.imageUrl,
                    contentDescription = nativeImage.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Add Textbook badge perfectly pinned
                SourceBadge(
                    source = "Textbook Scan",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }
            
            // Content Right
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = nativeImage.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sourced natively from page ${nativeImage.page}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Use Diagram", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SourceBadge(source: String, modifier: Modifier = Modifier) {
    val displaySource = source.lowercase().trim()
    val badgeColor = when {
        displaySource == "wikipedia" -> Color(0xFF60A5FA)  // Blue-400
        displaySource == "wikimedia" -> Color(0xFF34D399)  // Emerald-400
        displaySource == "duckduckgo" -> Color(0xFFF97316) // Orange-500
        displaySource == "textbook scan" -> Color(0xFFF472B6) // Pink-400
        else -> Color.White.copy(alpha = 0.5f)
    }
    
    val badgeText = when {
        displaySource == "wikipedia" -> "WIKIPEDIA"
        displaySource == "wikimedia" -> "WIKIMEDIA"
        displaySource == "duckduckgo" -> "DUCKDUCKGO"
        displaySource == "textbook scan" -> "TEXTBOOK"
        else -> displaySource.uppercase()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor.copy(alpha = 0.25f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = badgeText,
            color = badgeColor,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
