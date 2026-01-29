package com.cosmic_struck.stellar.create_module.presentation.navigation

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavArgument
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.cosmic_struck.stellar.create_module.presentation.screens.CreateModuleModelScreen
import com.cosmic_struck.stellar.create_module.presentation.screens.CreateModuleScreen
import com.cosmic_struck.stellar.create_module.presentation.screens.UploadStatusTracker
import com.cosmic_struck.stellar.create_module.presentation.viewmodel.CreateModuleViewModel

fun NavGraphBuilder.createModuleNavigation(navHostController: NavHostController) {
    navigation(
        startDestination = CreateModuleScreens.CreateModuleScreen.route,
        route = "create_module_graph/{classroom_id}",
        arguments = listOf(
            navArgument("classroom_id") { type = NavType.StringType }
        )
    ) {
        composable(route = CreateModuleScreens.CreateModuleScreen.route) { backStackEntry ->
            // Use the parent property to get the graph-scoped ViewModel
            val parentEntry = remember(backStackEntry) {
                navHostController.getBackStackEntry("create_module_graph/{classroom_id}")
            }
            val viewModel = hiltViewModel<CreateModuleViewModel>(parentEntry)

            CreateModuleScreen(
                viewmodel = viewModel,
                navigateToModelScreen = {
                    navHostController.navigate(CreateModuleScreens.CreateModuleModelScreen.route)
                }
            )
        }

        composable(route = CreateModuleScreens.CreateModuleModelScreen.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navHostController.getBackStackEntry("create_module_graph/{classroom_id}")
            }
            val viewModel = hiltViewModel<CreateModuleViewModel>(parentEntry)

            CreateModuleModelScreen(
                viewmodel = viewModel,
                navigateToUploadTracker = {
                    navHostController.navigate(CreateModuleScreens.UploadResultsScreen.route)
                }
            )
        }

        composable(route = CreateModuleScreens.UploadResultsScreen.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navHostController.getBackStackEntry("create_module_graph/{classroom_id}")
            }
            val viewModel = hiltViewModel<CreateModuleViewModel>(parentEntry)

            UploadStatusTracker(viewModel = viewModel)
        }
    }
}

sealed class CreateModuleScreens(val route: String){
    object CreateModuleScreen : CreateModuleScreens("create_module_screen")
    object CreateModuleModelScreen: CreateModuleScreens("create_module_model_screen")
    object UploadResultsScreen: CreateModuleScreens("upload_results_screen")

}