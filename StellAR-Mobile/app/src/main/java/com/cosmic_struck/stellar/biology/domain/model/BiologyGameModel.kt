package com.cosmic_struck.stellar.biology.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.biology.common.BiologyGreen
import com.cosmic_struck.stellar.biology.common.BiologyTeal

data class BiologyGameModel(
    val id: Int,
    val title: String,
    @DrawableRes val thumbnail: Int,
    val description: String,
    val route: String,
    val color: Color
)

// Hardcoded Biology AR Lab games
val biologyGames = listOf<BiologyGameModel>(
    BiologyGameModel(
        id = 1,
        title = "Cell Explorer",
        thumbnail = R.drawable.ic_launcher_foreground, // Placeholder - replace with actual biology image
        description = "Explore the inside of a living cell in AR! Zoom into organelles and learn their functions.",
        route = "biology_cell_explorer",
        color = BiologyGreen,
    ),
    BiologyGameModel(
        id = 2,
        title = "Anatomy Lab",
        thumbnail = R.drawable.ic_launcher_foreground, // Placeholder - replace with actual biology image
        description = "Place a 3D human body in AR and explore different organ systems. Learn anatomy interactively!",
        route = "biology_anatomy_lab",
        color = BiologyTeal,
    )
)
