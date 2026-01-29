package com.cosmic_struck.stellar.stellar.arlab.presentation.states

import com.cosmic_struck.stellar.stellar.arlab.domain.model.PlanetComparatorModel

data class PlanetComparatorState(
    val leftPlanet: PlanetComparatorModel? = null,
    val rightPlanet: PlanetComparatorModel? = null,
    val correctPlanetId: String = "",
    val score: Int = 0,
    val round: Int = 1,
    val feedback: Feedback? = null
)

enum class Feedback {
    CORRECT,
    WRONG
}


