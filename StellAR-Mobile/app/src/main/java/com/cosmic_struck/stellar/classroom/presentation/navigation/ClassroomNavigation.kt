package com.cosmic_struck.stellar.classroom.presentation.navigation

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.cosmic_struck.stellar.classroom.presentation.screens.ChatBotScreen
import com.cosmic_struck.stellar.classroom.presentation.screens.ClassroomHomeScreen
import com.cosmic_struck.stellar.classroom.presentation.screens.ClassroomModuleScreen
import com.cosmic_struck.stellar.classroom.presentation.screens.QuizScreen
import com.cosmic_struck.stellar.classroom.presentation.screens.SummaryScreen
import com.cosmic_struck.stellar.classroom.presentation.viewmodel.ClassroomViewModel
import com.cosmic_struck.stellar.common.navigation.Screens

fun NavGraphBuilder.classroomGraph(navHostController: NavHostController){
    navigation(
        route = "classroom_graph/{classroom_id}",
        arguments = listOf(
            navArgument(name = "classroom_id"){
                type = NavType.StringType
            }
        ),
        startDestination = ClassroomScreens.ClassroomHomeScreen.route
    ){
        composable(
            route = ClassroomScreens.ClassroomHomeScreen.route
        ){
            val entry = remember(it){
                navHostController.getBackStackEntry("classroom_graph/{classroom_id}")
            }
            val viewmodel : ClassroomViewModel = hiltViewModel<ClassroomViewModel>(entry)
            ClassroomHomeScreen(
                navigateToModelScreen = {
                    navHostController.navigate(ClassroomScreens.ClassroomModuleScreen.route)
                },
                viewmodel = viewmodel,
                navigateToCreateModuleScreen = {
                    navHostController.navigate("create_module_graph/$it")
                }
            )
        }

        composable(
            route = ClassroomScreens.ClassroomModuleScreen.route,
            ){
            val entry = remember(it) {
                navHostController.getBackStackEntry("classroom_graph/{classroom_id}")
            }
            val viewmodel : ClassroomViewModel = hiltViewModel<ClassroomViewModel>(entry)
            ClassroomModuleScreen(
                navigateToSummaryScreen = {
                    navHostController.navigate(ClassroomScreens.ModuleSummaryScreen.route)
                },
                navigateToQuizScreen = {
                    navHostController.navigate(ClassroomScreens.QuizScreen.route)
                },
                navigateToChatScreen = {
                    navHostController.navigate(ClassroomScreens.ChatBotScreen.route)
                },
                viewModel = viewmodel
            )
        }

        composable(
            route = ClassroomScreens.ModuleSummaryScreen.route
        ){
            val entry = remember(it) {
                navHostController.getBackStackEntry("classroom_graph/{classroom_id}")
            }
            val viewmodel : ClassroomViewModel = hiltViewModel<ClassroomViewModel>(entry)
            SummaryScreen(
                onBack = {
                    navHostController.popBackStack()
                },
                viewModel = viewmodel
            )
        }

        composable(
            route = ClassroomScreens.QuizScreen.route
        ){
            val entry = remember(it) {
                navHostController.getBackStackEntry("classroom_graph/{classroom_id}")
            }
            val viewmodel : ClassroomViewModel = hiltViewModel<ClassroomViewModel>(entry)
            QuizScreen(
                backToHome = {
                    navHostController.popBackStack()
                },
                viewModel = viewmodel
            )
        }

        composable(
            route = ClassroomScreens.ChatBotScreen.route
        ){
            val entry = remember(it) {
                navHostController.getBackStackEntry("classroom_graph/{classroom_id}")
            }
            val viewmodel : ClassroomViewModel = hiltViewModel<ClassroomViewModel>(entry)
            ChatBotScreen(
                onBack = {
                    navHostController.popBackStack()
                },
                viewModel = viewmodel
            )
        }
    }
}

private sealed class ClassroomScreens(val route: String){
    object ClassroomHomeScreen : ClassroomScreens("classroom_home_screen")
    object ClassroomModuleScreen : ClassroomScreens("classroom_module_screen")
    object ModuleSummaryScreen : ClassroomScreens("module_summary_screen")
    object QuizScreen : ClassroomScreens("quiz_screen")
    object ChatBotScreen : ClassroomScreens("chatbot_screen")
}
