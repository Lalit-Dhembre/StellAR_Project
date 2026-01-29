package com.cosmic_struck.stellar.chemistry.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.cosmic_struck.stellar.chemistry.arlab.ChemistryARLabScreen
import com.cosmic_struck.stellar.chemistry.arlab.reaction_lab.presentation.ReactionLabHomeScreen
import com.cosmic_struck.stellar.chemistry.home.ChemistryHomeScreen
import com.cosmic_struck.stellar.chemistry.models.ChemistryModelsScreen

fun NavGraphBuilder.chemistryNavigation(
    navHostController: NavHostController,
    modifier: Modifier = Modifier
) {
    navigation(
        startDestination = ChemistryNavigationScreens.ChemistryHomeScreen.route,
        route = "chemistry_navigation"
    ) {
        composable(
            route = ChemistryNavigationScreens.ChemistryHomeScreen.route
        ) {
            ChemistryHomeScreen(
                navHostController = navHostController,
                navigateToScanText = {
                    // Navigate to models screen for now
                    navHostController.navigate(ChemistryNavigationScreens.ChemistryModels.route).apply {

                    }
                }
            )
        }
        composable(
            route = ChemistryNavigationScreens.ReactionLab.route
        ){
            ReactionLabHomeScreen()
        }

        composable(
            route = ChemistryNavigationScreens.ChemistryModels.route
        ) {
            ChemistryModelsScreen(
                navController = navHostController
            )
        }

        composable(
            route = ChemistryNavigationScreens.ChemistryARLab.route
        ) {
            ChemistryARLabScreen(
                navController = navHostController
            )
        }
    }
}

sealed class ChemistryNavigationScreens(val route: String) {
    object ChemistryHomeScreen : ChemistryNavigationScreens("chemistry_home")
    object ChemistryModels : ChemistryNavigationScreens("chemistry_models_screen")
    object ChemistryARLab : ChemistryNavigationScreens("chemistry_arlab_screen")

    object ReactionLab : ChemistryNavigationScreens("reaction_lab")
}
