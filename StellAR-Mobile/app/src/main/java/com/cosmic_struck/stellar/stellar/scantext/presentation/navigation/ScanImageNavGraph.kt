package com.cosmic_struck.stellar.stellar.scantext.presentation.navigation

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.cosmic_struck.stellar.stellar.scantext.presentation.screens.ScanResultsScreen
import com.cosmic_struck.stellar.stellar.scantext.presentation.screens.ScanTextScreen
import com.cosmic_struck.stellar.stellar.scantext.presentation.scanScreen.ScanTextViewModel

fun NavGraphBuilder.scanImageGraph(navHostController: NavHostController){
    navigation(
        startDestination = ScanImageScreens.ScanImage.route,
        route = "scan_image"
    ){
        composable(route = ScanImageScreens.ScanImage.route){
            val entry = remember(it) {
                navHostController.getBackStackEntry("scan_image")
            }
            val viewModel : ScanTextViewModel = hiltViewModel(entry)
            ScanTextScreen(
                navigateBack = {
                    navHostController.popBackStack()
                },
                viewModel = viewModel,
                navigateToResults = {
                    navHostController.navigate(ScanImageScreens.ResultScreen.route){
                        popUpTo(ScanImageScreens.ScanImage.route){
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(route = ScanImageScreens.ResultScreen.route){
            val entry = remember(it) {
                navHostController.getBackStackEntry("scan_image")
            }
            val viewModel : ScanTextViewModel = hiltViewModel(entry)
            ScanResultsScreen(
                onNavigateBack = {
                    navHostController.popBackStack()
                },
                viewModel = viewModel
            )
        }
    }
}

sealed class ScanImageScreens(val route: String){
    object ScanImage : ScanImageScreens(route = "scan_image_screen")
    object ResultScreen : ScanImageScreens(route = "result_screen")
}