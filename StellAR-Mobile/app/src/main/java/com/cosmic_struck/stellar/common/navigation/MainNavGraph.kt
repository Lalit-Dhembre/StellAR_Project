package com.cosmic_struck.stellar.common.navigation

import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cosmic_struck.stellar.auth.presentation.navigation.authGraph
import com.cosmic_struck.stellar.biology.navigation.biologyNavigation
import com.cosmic_struck.stellar.chemistry.home.ChemistryHomeScreen
import com.cosmic_struck.stellar.chemistry.navigation.chemistryNavigation
import com.cosmic_struck.stellar.classroom.presentation.navigation.classroomGraph
import com.cosmic_struck.stellar.create_module.presentation.navigation.createModuleNavigation
import com.cosmic_struck.stellar.history.home.HistoryHomeScreen
import com.cosmic_struck.stellar.home.presentation.screens.HomeScreen
import com.cosmic_struck.stellar.home.presentation.screens.ProfileScreen
import com.cosmic_struck.stellar.physics.home.PhysicsHomeScreen
import com.cosmic_struck.stellar.stellar.arlab.presentation.navigation.arLabNavigation
import com.cosmic_struck.stellar.stellar.home.presentation.StellarHomeScreen
import com.cosmic_struck.stellar.stellar.models.presentation.navigation.modelNavGraph
import com.cosmic_struck.stellar.stellar.scantext.presentation.navigation.scanImageGraph
import io.appwrite.services.Account
import com.cosmic_struck.stellar.onboarding.presentation.OnboardingScreen
import com.cosmic_struck.stellar.physics.navigation.physicsNavigation
import com.cosmic_struck.stellar.history.navigation.historyNavigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavGraph(
    account: Account,
    navHostController: NavHostController,
    onboardingCompleted: Boolean,
    modifier: Modifier = Modifier) {

        // Check if user has an active session
        val hasSession = remember { mutableStateOf<Boolean?>(null) }
        LaunchedEffect(Unit) {
            hasSession.value = try {
                account.get()
                true
            } catch (e: Exception) {
                false
            }
        }

        val sessionChecked = hasSession.value
        if (sessionChecked == null) return // Still loading

        Log.d("MAINNAVGRAPH","has session: $sessionChecked")
        val startDestination = if (!onboardingCompleted) "onboarding" else if(sessionChecked) Screens.HomeScreen.route else "auth"
        NavHost(navHostController, startDestination = startDestination) {

            composable("onboarding") {
                OnboardingScreen(
                    onOnboardingCompleted = {
                        navHostController.navigate("auth") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screens.HomeScreen.route
            ){
                    HomeScreen(
                        navigateToModuleScreen = {
                            navHostController.navigate(it)
                        },
                        navigateToClassroomHomeScreen = {it->
                            navHostController.navigate("classroom_graph/$it")
                        },
                        navigateToProfileScreen = {
                            navHostController.navigate(Screens.ProfileScreen.route)
                        }
                    )
            }

            composable(
                route = Screens.ProfileScreen.route
            ) {
                ProfileScreen(
                    onBack = { navHostController.popBackStack() },
                    onLogout = {
                        // Navigate to auth screen and clear backstack
                        navHostController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }


            composable(route = Screens.StellarHomeScreen.route){
                StellarHomeScreen(
                    navHostController = navHostController,
                    navigateToScanText = {
                        navHostController.navigate("scan_image")
                    },
                )
            }


            // Physics navigation graph
            physicsNavigation(navHostController)

            // History navigation graph
            historyNavigation(navHostController)
            
            // Biology navigation graph
            biologyNavigation(navHostController)
            chemistryNavigation(navHostController)
            arLabNavigation(navHostController)
            authGraph(navHostController)
            scanImageGraph(navHostController)
            modelNavGraph(navHostController)
            classroomGraph(navHostController)
            createModuleNavigation(navHostController)

        }
    }
