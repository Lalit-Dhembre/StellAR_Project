package com.cosmic_struck.stellar.stellar.arlab.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.stellar.arlab.presentation.navigation.ARLabNavigationScreens

data class GameModel(
    val id: Int,
    val title: String,
    @DrawableRes val thumbnail: Int,
    val description: String,
    val route: String,
    val color: Color
)

val games = listOf<GameModel>(
    GameModel(
        id = 1,
        title = "Planet Comparator",
        thumbnail = R.drawable.planet_comparison_game_card,
        description = "Compare two planets in AR and tap the larger one. Learn real planet sizes by exploring them in the real world.",
        route = ARLabNavigationScreens.PlanetComparison.route,
        color = Color(0xFF231942),
    ),
    GameModel(
        id = 2,
        title = "Universe Lab",
        thumbnail = R.drawable.space_icon,
        description = "Place a solar system in AR and watch planets orbit the Sun! Control time, observe gravity, and learn celestial mechanics.",
        route = ARLabNavigationScreens.UniverseLab.route,
        color = Color(0xFF1A237E),
    )
)

