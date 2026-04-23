package com.cosmic_struck.stellar.stellar.pdfar.presentation.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.cosmic_struck.stellar.stellar.pdfar.presentation.screens.PdfArScreen
import java.io.File

sealed class PdfArNavigationScreens(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList(),
    val deepLinks: List<NavDeepLink> = emptyList()
) {
    data object PdfArMainScreen : PdfArNavigationScreens(
        route = "pdf_ar_main_screen/{domain}",
        arguments = listOf(androidx.navigation.navArgument("domain") { type = androidx.navigation.NavType.StringType })
    ) {
        fun createRoute(domain: String) = "pdf_ar_main_screen/$domain"
    }
}

fun NavGraphBuilder.pdfArNavigation(
    navHostController: NavHostController,
    onNavigateToARViewer: (String, String, String?) -> Unit
) {
    composable(
        route = PdfArNavigationScreens.PdfArMainScreen.route,
        arguments = PdfArNavigationScreens.PdfArMainScreen.arguments
    ) { backStackEntry ->
        val domain = backStackEntry.arguments?.getString("domain") ?: "stellar"
        PdfArScreen(
            domain = domain,
            onNavigateBack = {
                navHostController.popBackStack()
            },
            onModelReady = { url, name, script ->
                onNavigateToARViewer(url, name, script)
            }
        )
    }
}
