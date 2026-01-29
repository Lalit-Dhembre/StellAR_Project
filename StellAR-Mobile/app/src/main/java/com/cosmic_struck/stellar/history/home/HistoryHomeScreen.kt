package com.cosmic_struck.stellar.history.home

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
import com.cosmic_struck.stellar.history.common.HistoryBottomAppBar
import com.cosmic_struck.stellar.history.common.HistoryScaffold
import com.cosmic_struck.stellar.history.home.components.HistoryBottomCaptions
import com.cosmic_struck.stellar.history.home.components.HistoryScanButton
import com.cosmic_struck.stellar.history.home.components.HistoryUpperCaptions

@Composable
fun HistoryHomeScreen(
    navHostController: NavHostController,
    modifier: Modifier = Modifier
) {
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
            HistoryScanButton(
                navigateToScanText = {
                    navHostController.navigate("scan_image")
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
            HistoryBottomCaptions()
        }
    }
}