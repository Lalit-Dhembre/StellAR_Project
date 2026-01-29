package com.cosmic_struck.stellar.chemistry.domain.model

// Hardcoded Chemistry model for display in Models screen
data class ChemistryModel(
    val id: Int,
    val name: String,
    val description: String,
    val thumbnailUrl: String,
    val modelUrl: String,
    val rarity: String,
    val xpReward: Int
)

// Hardcoded sample chemistry models
val sampleChemistryModels = listOf(
    ChemistryModel(
        id = 1,
        name = "Water Molecule (H₂O)",
        description = "The essential molecule of life! Two hydrogen atoms bonded to one oxygen atom.",
        thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b7/H2O_2D_labelled.svg/1200px-H2O_2D_labelled.svg.png",
        modelUrl = "",
        rarity = "Common",
        xpReward = 100
    ),
    ChemistryModel(
        id = 2,
        name = "Carbon Dioxide (CO₂)",
        description = "A greenhouse gas molecule with one carbon and two oxygen atoms in a linear structure.",
        thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5e/Carbon-dioxide-2D-dimensions.svg/1200px-Carbon-dioxide-2D-dimensions.svg.png",
        modelUrl = "",
        rarity = "Common",
        xpReward = 100
    ),
    ChemistryModel(
        id = 3,
        name = "Methane (CH₄)",
        description = "The simplest hydrocarbon with a tetrahedral structure. Natural gas's main component.",
        thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/94/Methane-2D-stereo.svg/1200px-Methane-2D-stereo.svg.png",
        modelUrl = "",
        rarity = "Common",
        xpReward = 100
    ),
    ChemistryModel(
        id = 4,
        name = "Benzene Ring (C₆H₆)",
        description = "The iconic aromatic ring structure with alternating double bonds.",
        thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/20/Benzene-2D-full.svg/1200px-Benzene-2D-full.svg.png",
        modelUrl = "",
        rarity = "Rare",
        xpReward = 250
    ),
    ChemistryModel(
        id = 5,
        name = "Caffeine (C₈H₁₀N₄O₂)",
        description = "The world's most popular psychoactive substance found in coffee and tea.",
        thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a4/Caffeine_structure.svg/1200px-Caffeine_structure.svg.png",
        modelUrl = "",
        rarity = "Legendary",
        xpReward = 500
    )
)

val lockedChemistryModels = listOf(
    ChemistryModel(
        id = 6,
        name = "DNA Nucleotide",
        description = "The building block of genetic material with phosphate, sugar, and base.",
        thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e4/DNA_chemical_structure.svg/1200px-DNA_chemical_structure.svg.png",
        modelUrl = "",
        rarity = "Legendary",
        xpReward = 500
    ),
    ChemistryModel(
        id = 7,
        name = "Aspirin (C₉H₈O₄)",
        description = "One of the most widely used medications in the world for pain relief.",
        thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Aspirin-2D-skeletal.svg/1200px-Aspirin-2D-skeletal.svg.png",
        modelUrl = "",
        rarity = "Rare",
        xpReward = 300
    )
)
