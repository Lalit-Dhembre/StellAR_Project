package com.cosmic_struck.stellar.stellar.pdfar.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmic_struck.stellar.stellar.pdfar.presentation.PdfArUiState
import com.cosmic_struck.stellar.stellar.pdfar.presentation.PdfArViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfArScreen(
    domain: String,
    onNavigateBack: () -> Unit,
    onModelReady: (File) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PdfArViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is PdfArUiState.Error) {
            val message = (uiState as PdfArUiState.Error).message
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        } else if (uiState is PdfArUiState.ModelReady) {
            val file = (uiState as PdfArUiState.ModelReady).modelFile
            onModelReady(file)
            viewModel.resetState() // reset so if they come back, it's idle or ready
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AR Generator") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // Using a simple back icon here, this is project specific
                        Text("←") 
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is PdfArUiState.Idle -> {
                    PdfUploadScreen(
                        onPdfSelected = { file ->
                            viewModel.processPdf(file, domain)
                        }
                    )
                }
                is PdfArUiState.Uploading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PdfArUiState.SectionsLoaded -> {
                    SectionListScreen(
                        sections = state.sections,
                        onSectionClick = { entity ->
                            viewModel.resolveEntityAndPoll(entity, context.cacheDir)
                        }
                    )
                }
                is PdfArUiState.GeneratingModel -> {
                    ModelLoadingScreen()
                }
                is PdfArUiState.ModelReady -> {
                    // Handled in LaunchedEffect
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Model is ready! Opening AR...")
                    }
                }
                is PdfArUiState.Error -> {
                    // Fallback visually if idle (snack bar shows anyway)
                    PdfUploadScreen(
                        onPdfSelected = { file ->
                            viewModel.processPdf(file)
                        }
                    )
                }
            }
        }
    }
}
