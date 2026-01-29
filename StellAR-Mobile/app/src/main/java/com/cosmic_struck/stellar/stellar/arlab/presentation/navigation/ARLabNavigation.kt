package com.cosmic_struck.stellar.stellar.arlab.presentation.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.cosmic_struck.stellar.stellar.arlab.presentation.screens.ARLabScreen
import com.cosmic_struck.stellar.stellar.arlab.presentation.screens.PlanetsComparatorScreen
import com.cosmic_struck.stellar.stellar.arlab.universe_lab.presentation.UniverseLabScreen

fun NavGraphBuilder.arLabNavigation(
    navHostController: NavHostController,
    modifier: Modifier = Modifier) {

    navigation(
        startDestination = ARLabNavigationScreens.ARLabHomeScreen.route,
        route = "ar_lab_navigation"
    ){
        composable(
            route = ARLabNavigationScreens.ARLabHomeScreen.route
        ){
            ARLabScreen(
                navController = navHostController
            )
        }

        composable(
            route = ARLabNavigationScreens.PlanetComparison.route
        ) {
            PlanetsComparatorScreen(
                navigateBack = {
                    navHostController.popBackStack()
                }
            )
        }

        composable(
            route = ARLabNavigationScreens.UniverseLab.route
        ) {
            UniverseLabScreen(
                navigateBack = {
                    navHostController.popBackStack()
                }
            )
        }
    }
}

sealed class ARLabNavigationScreens(val route: String){
    object ARLabHomeScreen : ARLabNavigationScreens("ar_lab_home_screen")
    object PlanetComparison : ARLabNavigationScreens("planet_comparison")
    object UniverseLab : ARLabNavigationScreens("universe_lab")
}
