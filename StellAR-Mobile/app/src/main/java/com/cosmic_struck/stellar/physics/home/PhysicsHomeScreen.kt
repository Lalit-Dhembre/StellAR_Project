package com.cosmic_struck.stellar.physics.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cosmic_struck.stellar.common.components.SimpleTopAppBar
import com.cosmic_struck.stellar.physics.common.PhysicsBottomAppBar
import com.cosmic_struck.stellar.physics.common.PhysicsScaffold
import com.cosmic_struck.stellar.physics.home.components.PhysicsBottomCaptions
import com.cosmic_struck.stellar.physics.home.components.PhysicsScanButton
import com.cosmic_struck.stellar.physics.home.components.PhysicsUpperCaptions

@Composable
fun PhysicsHomeScreen(
    navHostController: NavHostController,
    modifier: Modifier = Modifier
) {
    PhysicsScaffold(
        bottomBar = {
            PhysicsBottomAppBar(navHostController)
        },
        topBar = {
            SimpleTopAppBar(
                title = "Physics",
                popNavigation = {
                    // Decide where 'Back' goes. Usually back to Main Home.
                    // If this is a nested graph, popBackStack() works if called from Main.
                    navHostController.popBackStack() 
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PhysicsUpperCaptions()
            Spacer(modifier = Modifier.height(48.dp))
            PhysicsScanButton(
                navigateToScanText = {
                    // Navigate to scan screen within Physics graph or global?
                    // Reusing generic scan or specific? Plan said reuse or create. 
                    // For now, let's leave it as a TODO or navigate to a placeholder if needed.
                    // PhysicsBottomAppBar navigates to 'physics_scan' if it existed? 
                    // Let's assume global scan for now or add to plan.
                    // Plan item 19: "Reuse ScanText or create PhysicsScanScreen"
                    // I will route to global 'scan_image' for now as it's cleaner.
                    navHostController.navigate("scan_image")
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
            PhysicsBottomCaptions()
        }
    }
}