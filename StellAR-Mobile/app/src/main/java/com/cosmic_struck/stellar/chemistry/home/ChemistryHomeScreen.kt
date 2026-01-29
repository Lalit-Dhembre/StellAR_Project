package com.cosmic_struck.stellar.chemistry.home

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cosmic_struck.stellar.chemistry.common.ChemistryBottomAppBar
import com.cosmic_struck.stellar.chemistry.common.ChemistryHomeCaption2
import com.cosmic_struck.stellar.chemistry.common.ChemistryScaffold
import com.cosmic_struck.stellar.chemistry.home.components.ChemistryBottomCaptions
import com.cosmic_struck.stellar.chemistry.home.components.ChemistryScanButton
import com.cosmic_struck.stellar.chemistry.home.components.ChemistryUpperCaptions
import com.cosmic_struck.stellar.common.components.SimpleTopAppBar

@Composable
fun ChemistryHomeScreen(
    navHostController: NavHostController,
    navigateToScanText: () -> Unit = {},
) {
    ChemistryScaffold(
        bottomBar = {
            ChemistryBottomAppBar(navHostController)
        },
        topBar = {
            SimpleTopAppBar(
                title = "Chemistry",
                popNavigation = {
                    navHostController.popBackStack()
                }
            )
        }
    ) {
        LaunchedEffect(true) {
            Log.d("Navigation Checking", "${navHostController.currentDestination}")
        }
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ChemistryUpperCaptions()
            Spacer(modifier = Modifier.height(48.dp))
            ChemistryScanButton(
                navigateToScanText = navigateToScanText
            )
            Spacer(modifier = Modifier.height(32.dp))
            ChemistryBottomCaptions(ChemistryHomeCaption2)
        }
    }
}