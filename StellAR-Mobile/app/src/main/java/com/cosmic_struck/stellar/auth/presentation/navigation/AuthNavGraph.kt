package com.cosmic_struck.stellar.auth.presentation.navigation

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.cosmic_struck.stellar.auth.presentation.screens.AuthScreen
import com.cosmic_struck.stellar.auth.presentation.screens.CreateAccountScreenEmailValidation
import com.cosmic_struck.stellar.auth.presentation.screens.CreateAccountScreenPasswordValidation
import com.cosmic_struck.stellar.auth.presentation.screens.LoginAccountScreen
import com.cosmic_struck.stellar.auth.presentation.viewmodel.AuthViewModel
import com.cosmic_struck.stellar.common.components.BackgroundScaffold
import com.cosmic_struck.stellar.common.navigation.Screens

fun NavGraphBuilder.authGraph(navHostController: NavHostController){
    navigation(
        startDestination = AuthScreens.AuthScreen.route,
        route = "auth"
    ){
        composable(route = AuthScreens.AuthScreen.route){
            val entry = remember(it) {
                navHostController.getBackStackEntry("auth")
            }
            val viewModel: AuthViewModel = hiltViewModel<AuthViewModel>(entry)
            AuthScreen(
                viewmodel = viewModel,
                navigateToHomeScreen = {
                    navHostController.navigate(Screens.HomeScreen.route)
                },
                navigateToSignUpScreen = {
                    navHostController.navigate(AuthScreens.SignUpScreen1.route)
                },
                navigateToLoginScreen = {
                    navHostController.navigate(AuthScreens.LoginScreen.route)
                },
            )
        }

        composable(route = AuthScreens.SignUpScreen1.route){ it->
            val parentEntry = remember(it) {
                navHostController.getBackStackEntry("auth")
            }
            val viewModel: AuthViewModel = hiltViewModel<AuthViewModel>(parentEntry)

            CreateAccountScreenEmailValidation(
                viewModel = viewModel,
                navigateToPasswordValidation = {
                    navHostController.navigate(AuthScreens.SignUpScreen2.route)
                },
                navigateback = {
                    navHostController.popBackStack()
                },
                setImageUri = viewModel::setProfileImage
            )
        }

        composable(route = AuthScreens.SignUpScreen2.route){
            val parentEntry = remember(it) {
                navHostController.getBackStackEntry("auth")
            }
            val viewModel: AuthViewModel = hiltViewModel<AuthViewModel>(parentEntry)

            CreateAccountScreenPasswordValidation(
                viewModel = viewModel,
                navigateback = {
                    navHostController.popBackStack()
                },
                navigateToHomeScreen = {
                    navHostController.navigate(Screens.HomeScreen.route){
                        popUpTo(Screens.AuthScreen.route){
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(route = AuthScreens.LoginScreen.route){
            val parentEntry = remember(it) {
                navHostController.getBackStackEntry("auth")
            }
            val viewModel: AuthViewModel = hiltViewModel<AuthViewModel>(parentEntry)
            LoginAccountScreen(
                viewModel = viewModel,
                navigateback = {
                    navHostController.popBackStack()
                },
                navigateToHomeScreen = {
                    navHostController.navigate(Screens.HomeScreen.route){
                        popUpTo(Screens.AuthScreen.route){
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}

private sealed class AuthScreens(val route: String){
    object AuthScreen: AuthScreens(route = "AuthScreen")
    object LoginScreen: AuthScreens(route = "LoginScreen")
    object SignUpScreen1: AuthScreens(route = "SignUpScreen1")
    object SignUpScreen2: AuthScreens(route = "SignUpScreen2")

}