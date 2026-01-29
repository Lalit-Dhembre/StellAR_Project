package com.cosmic_struck.stellar.biology.home

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
import com.cosmic_struck.stellar.biology.common.BiologyBottomAppBar
import com.cosmic_struck.stellar.biology.common.BiologyHomeCaption2
import com.cosmic_struck.stellar.biology.common.BiologyScaffold
import com.cosmic_struck.stellar.biology.home.components.BiologyBottomCaptions
import com.cosmic_struck.stellar.biology.home.components.BiologyScanButton
import com.cosmic_struck.stellar.biology.home.components.BiologyUpperCaptions
import com.cosmic_struck.stellar.common.components.SimpleTopAppBar

@Composable
fun BiologyHomeScreen(
    navHostController: NavHostController,
    navigateToScanText: () -> Unit,
    modifier: Modifier = Modifier
) {
    BiologyScaffold(
        bottomBar = {
            BiologyBottomAppBar(navHostController)
        },
        topBar = {
            SimpleTopAppBar(
                title = "Biology",
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
            BiologyUpperCaptions()
            Spacer(modifier = Modifier.height(48.dp))
            BiologyScanButton(
                navigateToScanText = navigateToScanText
            )
            Spacer(modifier = Modifier.height(32.dp))
            BiologyBottomCaptions(BiologyHomeCaption2)
        }
    }
}