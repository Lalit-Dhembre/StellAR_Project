package com.cosmic_struck.stellar.stellar.models.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.cosmic_struck.stellar.stellar.models.presentation.screens.ModelScreen
import com.cosmic_struck.stellar.stellar.models.presentation.screens.ModelViewerScreen

fun NavGraphBuilder.modelNavGraph(navHostController: NavHostController) {

    navigation(
        startDestination = StellarModelScreen.HomeScreen.route,
        route = "model_graph"
    ){
        composable(
            route = StellarModelScreen.HomeScreen.route
        ){
            val entry = remember(it){
                navHostController.getBackStackEntry("model_graph")
            }
            ModelScreen(
                navController = navHostController,
                navigateToModelViewer = {it1,it2->
                    val encodeUrl = Uri.encode(it1)
                    navHostController.navigate(StellarModelScreen.ModelScreen.route + "/$encodeUrl/$it2")
                }
            )
        }

        composable(route = StellarModelScreen.ModelScreen.route + "/{url}/{name}?script={script}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType },
                navArgument("script") { 
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )){
            ModelViewerScreen(
                navigateBack = {
                    navHostController.popBackStack()
                }
            )
        }
    }
}

sealed class StellarModelScreen(val route: String) {
    object HomeScreen : StellarModelScreen("home_screen")
    object ModelScreen : StellarModelScreen("model_screen")
}
