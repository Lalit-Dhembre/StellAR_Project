package com.cosmic_struck.stellar.physics.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import android.net.Uri
import com.cosmic_struck.stellar.common.components.SimpleTopAppBar
import com.cosmic_struck.stellar.physics.common.PhysicsBottomAppBar
import com.cosmic_struck.stellar.physics.common.PhysicsScaffold
import com.cosmic_struck.stellar.physics.home.components.PhysicsBottomCaptions
import com.cosmic_struck.stellar.physics.home.components.PhysicsModelsButton
import com.cosmic_struck.stellar.physics.home.components.PhysicsUploadButton
import com.cosmic_struck.stellar.physics.home.components.PhysicsUpperCaptions

@Composable
fun PhysicsHomeScreen(
    navHostController: NavHostController,
    onUploadClick: () -> Unit = {},
    onModelsClick: () -> Unit = {},
    onDocumentSelected: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onDocumentSelected(it) }
    }

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
            PhysicsUploadButton(
                onUploadClick = onUploadClick
            )
            Spacer(modifier = Modifier.height(16.dp))
            PhysicsModelsButton(
                onModelsClick = onModelsClick
            )
            Spacer(modifier = Modifier.height(32.dp))
            PhysicsBottomCaptions()
        }
    }
}