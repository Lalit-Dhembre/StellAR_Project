package com.cosmic_struck.stellar.common.navigation

import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import com.cosmic_struck.stellar.onboarding.presentation.OnboardingScreen
import com.cosmic_struck.stellar.physics.navigation.physicsNavigation
import com.cosmic_struck.stellar.history.navigation.historyNavigation
import com.cosmic_struck.stellar.stellar.pdfar.presentation.navigation.pdfArNavigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavGraph(
    supabase: SupabaseClient,
    navHostController: NavHostController,
    onboardingCompleted: Boolean,
    modifier: Modifier = Modifier) {

        val auth = supabase.auth.currentSessionOrNull()

        Log.d("MAINNAVGRAPH",auth.toString())
        val startDestination = if (!onboardingCompleted) "onboarding" else if(auth!= null) Screens.HomeScreen.route else "auth"
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


            composable(route = Screens.StellarHomeScreen.route){ backStackEntry ->
                val scanViewModel: com.cosmic_struck.stellar.stellar.scantext.presentation.scanScreen.ScanTextViewModel = 
                    androidx.hilt.navigation.compose.hiltViewModel()
                val context = androidx.compose.ui.platform.LocalContext.current
                
                StellarHomeScreen(
                    navHostController = navHostController,
                    onUploadClick = { 
                        navHostController.navigate(com.cosmic_struck.stellar.stellar.pdfar.presentation.navigation.PdfArNavigationScreens.PdfArMainScreen.createRoute("stellar"))
                    },
                    onModelsClick = {
                        navHostController.navigate("models") 
                    },
                    onDocumentSelected = { uri ->
                        scanViewModel.uploadDocument(context, uri, "stellar")
                    }
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
            
            pdfArNavigation(
                navHostController = navHostController,
                onNavigateToARViewer = { url, name ->
                    val encodedUrl = android.net.Uri.encode(url)
                    navHostController.navigate(com.cosmic_struck.stellar.stellar.models.presentation.navigation.StellarModelScreen.ModelScreen.route + "/$encodedUrl/$name")
                }
            )

        }
    }

