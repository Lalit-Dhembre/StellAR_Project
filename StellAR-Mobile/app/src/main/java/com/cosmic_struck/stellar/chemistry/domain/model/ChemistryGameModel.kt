package com.cosmic_struck.stellar.chemistry.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.chemistry.common.ChemistryPurple
import com.cosmic_struck.stellar.chemistry.common.ChemistryNavy

data class ChemistryGameModel(
    val id: Int,
    val title: String,
    @DrawableRes val thumbnail: Int,
    val description: String,
    val route: String,
    val color: Color
)

// Hardcoded Chemistry AR Lab games
val chemistryGames = listOf<ChemistryGameModel>(
    ChemistryGameModel(
        id = 1,
        title = "Molecule Builder",
        thumbnail = R.drawable.ic_launcher_foreground, // Placeholder - replace with actual chemistry image
        description = "Build 3D molecules in AR! Connect atoms and learn about chemical bonds and molecular geometry.",
        route = "chemistry_molecule_builder",
        color = ChemistryPurple,
    ),
    ChemistryGameModel(
        id = 2,
        title = "Reaction Lab",
        thumbnail = R.drawable.ic_launcher_foreground, // Placeholder - replace with actual chemistry image
        description = "Conduct virtual chemistry experiments in AR. Mix chemicals and observe reactions safely!",
        route = "chemistry_reaction_lab",
        color = ChemistryNavy,
    )
)
