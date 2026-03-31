package com.cosmic_struck.stellar.stellar.home.presentation

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import android.net.Uri
import com.cosmic_struck.stellar.common.components.BottomAppBar
import com.cosmic_struck.stellar.common.components.SimpleTopAppBar
import com.cosmic_struck.stellar.common.components.StellarScaffold
import com.cosmic_struck.stellar.common.util.HomeScreenCaptions2
import com.cosmic_struck.stellar.stellar.home.presentation.components.BottomCaptions
import com.cosmic_struck.stellar.stellar.home.presentation.components.ModelsButton
import com.cosmic_struck.stellar.stellar.home.presentation.components.UploadButton
import com.cosmic_struck.stellar.stellar.home.presentation.components.UpperCaptions

@Composable
fun StellarHomeScreen(
    navHostController: NavHostController,
    onUploadClick: () -> Unit = {},
    onModelsClick: () -> Unit = {},
    onDocumentSelected: (Uri) -> Unit = {},
    viewModel: StellarHomeScreenViewModel = hiltViewModel<StellarHomeScreenViewModel>(),
    modifier: Modifier = Modifier
) {
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onDocumentSelected(it) }
    }

    StellarScaffold(
        bottomBar = {
            BottomAppBar(navHostController)
        },
        topBar = {
            SimpleTopAppBar(
                title = "Stellar",
                popNavigation = {
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
            UpperCaptions()
            Spacer(modifier = Modifier.height(48.dp))
            UploadButton(
                onUploadClick = onUploadClick
            )
            Spacer(modifier = Modifier.height(16.dp))
            ModelsButton(
                onModelsClick = onModelsClick
            )
            Spacer(modifier = Modifier.height(32.dp))
            BottomCaptions(HomeScreenCaptions2)
        }
    }
}