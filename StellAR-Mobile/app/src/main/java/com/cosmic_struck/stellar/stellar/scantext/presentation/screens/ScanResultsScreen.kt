package com.cosmic_struck.stellar.stellar.scantext.presentation.scanScreen

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmic_struck.stellar.common.components.StellarScaffold
import com.cosmic_struck.stellar.stellar.scantext.presentation.components.ScanCard
import com.cosmic_struck.stellar.stellar.scantext.presentation.components.TopBarScanTextBook

@Composable
fun ScanResultsScreen(
    onNavigateBack : () -> Unit,
    viewModel: ScanTextViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val imageUrls by viewModel.imageUrls.collectAsState()

    Log.d("SCAN_RESULTS_SCREEN", "State: $state")
    Log.d("SCAN_RESULTS_SCREEN", "ScanResults null: ${state.scanResults == null}")
    Log.d("SCAN_RESULTS_SCREEN", "Count: ${state.scanResults?.count}")
    Log.d("SCAN_RESULTS_SCREEN", "Info size: ${state.scanResults?.info?.size}")
    Log.d("SCAN_RESULTS_SCREEN", "Image URLs: $imageUrls")


    StellarScaffold(
        topBar = {
            TopBarScanTextBook(
                title = "Scan Results",
                navigateBack ={onNavigateBack()}
            )
        }
    ) {
        if (state.isLoading) {
            Box(
                modifier = it.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.isError.isNotEmpty()) {
            Box(
                modifier = it.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: ${state.isError}", color = Color.Red)
            }
        } else if (state.scanResults != null) {
            val scanResults = state.scanResults!!

            Log.d("SCAN_RESULTS_SCREEN", "Rendering items. Count: ${scanResults.count}")
            Log.d("SCAN_RESULTS_SCREEN", "Info list: ${scanResults.info}")

            // Check if we have data
            if (scanResults.count == 0 || scanResults.info.isEmpty()) {
                Box(
                    modifier = it.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No detections found", color = Color.White)
                }
            } else {
                LazyColumn(
                    modifier = it.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(scanResults.info.size) { index ->
                        Log.d("SCAN_RESULTS_ITEM", "Rendering item: $index")

                        val info = scanResults.info[index]
                        val imageUrl = if (index < imageUrls.size) imageUrls[index] else ""

                        Log.d("SCAN_RESULTS_ITEM", "Title: ${info.title}, Image: $imageUrl")

                        ScanCard(
                            info1 = info.facts.getOrNull(0) ?: "N/A",
                            info2 = info.facts.getOrNull(1) ?: "N/A",
                            info3 = info.facts.getOrNull(2) ?: "N/A",
                            title = info.title,
                            description = info.summary,
                            badge = info.badge,
                            image = imageUrl
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = it.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No results available", color = Color.White)
            }
        }
    }
}