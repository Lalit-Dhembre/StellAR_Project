package com.cosmic_struck.stellar.biology.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.cosmic_struck.stellar.biology.arlab.BiologyARLabScreen
import com.cosmic_struck.stellar.biology.home.BiologyHomeScreen
import com.cosmic_struck.stellar.biology.models.BiologyModelsScreen

fun NavGraphBuilder.biologyNavigation(
    navHostController: NavHostController,
    modifier: Modifier = Modifier
) {
    navigation(
        startDestination = BiologyNavigationScreens.BiologyHomeScreen.route,
        route = "biology_navigation"
    ) {
        composable(
            route = BiologyNavigationScreens.BiologyHomeScreen.route
        ) { backStackEntry ->
            val viewModel: com.cosmic_struck.stellar.stellar.scantext.presentation.scanScreen.ScanTextViewModel = 
                androidx.hilt.navigation.compose.hiltViewModel()
            val context = androidx.compose.ui.platform.LocalContext.current
            
            BiologyHomeScreen(
                navHostController = navHostController,
                onUploadClick = {
                    // Start document picker instead of navigating to scan_image
                },
                onModelsClick = {
                    navHostController.navigate(BiologyNavigationScreens.BiologyModels.route)
                },
                onDocumentSelected = { uri ->
                    viewModel.uploadDocument(context, uri, "biology")
                }
            )
        }

        composable(
            route = BiologyNavigationScreens.BiologyModels.route
        ) {
            BiologyModelsScreen(
                navController = navHostController
            )
        }

        composable(
            route = BiologyNavigationScreens.BiologyARLab.route
        ) {
            BiologyARLabScreen(
                navController = navHostController
            )
        }
    }
}

sealed class BiologyNavigationScreens(val route: String) {
    object BiologyHomeScreen : BiologyNavigationScreens("biology_home")
    object BiologyModels : BiologyNavigationScreens("biology_models_screen")
    object BiologyARLab : BiologyNavigationScreens("biology_arlab_screen")
}
