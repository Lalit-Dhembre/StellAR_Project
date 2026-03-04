package com.cosmic_struck.stellar.stellar.scantext.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmic_struck.stellar.common.components.StellarScaffold
import com.cosmic_struck.stellar.stellar.scantext.presentation.components.TopBarScanTextBook
import com.cosmic_struck.stellar.stellar.scantext.presentation.scanScreen.ScanTextViewModel

@Composable
fun ScanResultsScreen(
    onNavigateBack : () -> Unit,
    viewModel: ScanTextViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Log.d("SCAN_RESULTS_SCREEN", "State: $state")
    Log.d("SCAN_RESULTS_SCREEN", "ScanResults null: ${state.scanResults == null}")
    Log.d("SCAN_RESULTS_SCREEN", "Count: ${state.scanResults?.count}")
    Log.d("SCAN_RESULTS_SCREEN", "Documents size: ${state.scanResults?.documents?.size}")

    StellarScaffold(
        topBar = {
            TopBarScanTextBook(
                title = "Document Results",
                navigateBack = { onNavigateBack() }
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
            val documents = scanResults.documents

            if (documents.isNullOrEmpty()) {
                Box(
                    modifier = it.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No documents parsed", color = Color.White)
                }
            } else {
                LazyColumn(
                    modifier = it.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(documents.size) { index ->
                        val doc = documents[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E1E2E)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Document #${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                doc.metadata?.get("source")?.let { source ->
                                    Text(
                                        text = "Source: $source",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    text = doc.page_content.take(500) + if (doc.page_content.length > 500) "..." else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
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
