package com.cosmic_struck.stellar.biology.domain.model

// Hardcoded Biology model for display in Models screen
data class BiologyModel(
    val id: Int,
    val name: String,
    val description: String,
    val thumbnailUrl: String,
    val modelUrl: String,
    val rarity: String,
    val xpReward: Int
)

// Hardcoded sample biology models
val sampleBiologyModels = listOf(
    BiologyModel(
        id = 1,
        name = "Animal Cell",
        description = "A typical animal cell with all major organelles including nucleus, mitochondria, and endoplasmic reticulum.",
        thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1a/Diagram_human_cell_nucleus.svg/1200px-Diagram_human_cell_nucleus.svg.png",
        modelUrl = "",
        rarity = "Common",
        xpReward = 100
    ),
    BiologyModel(
        id = 2,
        name = "Plant Cell",
        description = "A plant cell showing cell wall, chloroplasts, and large central vacuole.",
        thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b8/Plant_cell_structure-en.svg/1200px-Plant_cell_structure-en.svg.png",
        modelUrl = "",
        rarity = "Common",
        xpReward = 100
    ),
    BiologyModel(
        id = 3,
        name = "DNA Double Helix",
        description = "The iconic double helix structure of DNA with base pairs visible.",
        thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4c/DNA_Structure%2BKey%2BLabelled.pn_NoBB.png/800px-DNA_Structure%2BKey%2BLabelled.pn_NoBB.png",
        modelUrl = "",
        rarity = "Rare",
        xpReward = 250
    ),
    BiologyModel(
        id = 4,
        name = "Human Heart",
        description = "A detailed 3D model of the human heart showing all four chambers.",
        thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/Diagram_of_the_human_heart_%28cropped%29.svg/1200px-Diagram_of_the_human_heart_%28cropped%29.svg.png",
        modelUrl = "",
        rarity = "Rare",
        xpReward = 300
    ),
    BiologyModel(
        id = 5,
        name = "Mitochondria",
        description = "The powerhouse of the cell! Explore the inner and outer membranes.",
        thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7a/Mitochondrion_mini.svg/1200px-Mitochondrion_mini.svg.png",
        modelUrl = "",
        rarity = "Legendary",
        xpReward = 500
    )
)

val lockedBiologyModels = listOf(
    BiologyModel(
        id = 6,
        name = "Human Brain",
        description = "Complete 3D model of the human brain with labeled regions.",
        thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1a/Human_brain.jpg/1200px-Human_brain.jpg",
        modelUrl = "",
        rarity = "Legendary",
        xpReward = 500
    ),
    BiologyModel(
        id = 7,
        name = "Neuron",
        description = "A nerve cell with dendrites, axon, and synaptic terminals.",
        thumbnailUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bc/Neuron_Hand-tuned.svg/1200px-Neuron_Hand-tuned.svg.png",
        modelUrl = "",
        rarity = "Rare",
        xpReward = 300
    )
)
