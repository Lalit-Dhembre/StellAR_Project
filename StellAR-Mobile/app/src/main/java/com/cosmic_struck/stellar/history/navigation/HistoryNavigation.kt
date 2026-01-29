package com.cosmic_struck.stellar.history.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.cosmic_struck.stellar.common.navigation.Screens
import com.cosmic_struck.stellar.history.arlab.HistoryARLabScreen
import com.cosmic_struck.stellar.history.home.HistoryHomeScreen
import com.cosmic_struck.stellar.history.models.HistoryModelsScreen

sealed class HistoryNavigationScreens(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList(),
    val deepLinks: List<NavDeepLink> = emptyList()
) {
    data object HistoryHomeScreen : HistoryNavigationScreens(Screens.HistoryHomeScreen.route)
    data object HistoryModels : HistoryNavigationScreens("history_models_screen")
    data object HistoryARLab : HistoryNavigationScreens("history_arlab_screen")
}

fun NavGraphBuilder.historyNavigation(navHostController: NavHostController) {
    navigation(
        startDestination = HistoryNavigationScreens.HistoryHomeScreen.route,
        route = "history_navigation"
    ) {
        composable(route = HistoryNavigationScreens.HistoryHomeScreen.route) {
            HistoryHomeScreen(navHostController)
        }
        composable(route = HistoryNavigationScreens.HistoryModels.route) {
            HistoryModelsScreen(navHostController)
        }
        composable(route = HistoryNavigationScreens.HistoryARLab.route) {
            HistoryARLabScreen(navHostController)
        }
    }
}
