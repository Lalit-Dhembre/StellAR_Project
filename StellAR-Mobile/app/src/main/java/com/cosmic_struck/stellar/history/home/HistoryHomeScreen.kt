package com.cosmic_struck.stellar.history.home

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
import com.cosmic_struck.stellar.history.common.HistoryBottomAppBar
import com.cosmic_struck.stellar.history.common.HistoryScaffold
import com.cosmic_struck.stellar.history.home.components.HistoryBottomCaptions
import com.cosmic_struck.stellar.history.home.components.HistoryModelsButton
import com.cosmic_struck.stellar.history.home.components.HistoryUploadButton
import com.cosmic_struck.stellar.history.home.components.HistoryUpperCaptions

@Composable
fun HistoryHomeScreen(
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

    HistoryScaffold(
        bottomBar = {
            HistoryBottomAppBar(navHostController)
        },
        topBar = {
            SimpleTopAppBar(
                title = "History",
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
            HistoryUpperCaptions()
            Spacer(modifier = Modifier.height(48.dp))
            HistoryUploadButton(
                onUploadClick = {
                    documentPickerLauncher.launch("application/pdf")
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            HistoryModelsButton(
                onModelsClick = onModelsClick
            )
            Spacer(modifier = Modifier.height(32.dp))
            HistoryBottomCaptions()
        }
    }
}