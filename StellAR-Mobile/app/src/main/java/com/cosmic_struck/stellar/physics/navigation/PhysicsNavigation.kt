package com.cosmic_struck.stellar.physics.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.cosmic_struck.stellar.common.navigation.Screens
import com.cosmic_struck.stellar.physics.arlab.PhysicsARLabScreen
import com.cosmic_struck.stellar.physics.home.PhysicsHomeScreen
import com.cosmic_struck.stellar.physics.models.PhysicsModelsScreen

sealed class PhysicsNavigationScreens(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList(),
    val deepLinks: List<NavDeepLink> = emptyList()
) {
    // Mapping to generic Screens where applicable
    data object PhysicsHomeScreen : PhysicsNavigationScreens(Screens.PhysicsHomeScreen.route)
    data object PhysicsModels : PhysicsNavigationScreens("physics_models_screen")
    data object PhysicsARLab : PhysicsNavigationScreens("physics_arlab_screen")
}

fun NavGraphBuilder.physicsNavigation(navHostController: NavHostController) {
    navigation(
        startDestination = PhysicsNavigationScreens.PhysicsHomeScreen.route,
        route = "physics_navigation"
    ) {
        composable(route = PhysicsNavigationScreens.PhysicsHomeScreen.route) { backStackEntry ->
            val viewModel: com.cosmic_struck.stellar.stellar.scantext.presentation.scanScreen.ScanTextViewModel = 
                androidx.hilt.navigation.compose.hiltViewModel()
            val context = androidx.compose.ui.platform.LocalContext.current
            
            PhysicsHomeScreen(
                navHostController = navHostController,
                onUploadClick = { 
                    navHostController.navigate(com.cosmic_struck.stellar.stellar.pdfar.presentation.navigation.PdfArNavigationScreens.PdfArMainScreen.createRoute("physics"))
                },
                onModelsClick = {
                    navHostController.navigate(PhysicsNavigationScreens.PhysicsModels.route)
                },
                onDocumentSelected = { uri ->
                    // Unused now
                }
            )
        }
        composable(route = PhysicsNavigationScreens.PhysicsModels.route) {
            PhysicsModelsScreen(navHostController)
        }
        composable(route = PhysicsNavigationScreens.PhysicsARLab.route) {
            PhysicsARLabScreen(navHostController)
        }
    }
}
