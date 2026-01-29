package com.cosmic_struck.stellar.create_module.presentation.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.common.util.Rajdhani
import com.cosmic_struck.stellar.create_module.presentation.components.ModelViewer
import com.cosmic_struck.stellar.create_module.presentation.viewmodel.CreateModuleViewModel
import com.cosmic_struck.stellar.create_module.presentation.UploadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateModuleModelScreen(
    navigateToUploadTracker: () -> Unit,
    viewmodel: CreateModuleViewModel = hiltViewModel<CreateModuleViewModel>(),
    modifier: Modifier = Modifier
) {
    val state = viewmodel.state.collectAsState().value
    var selectedTab by remember { mutableIntStateOf(state.selectedOption) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            viewmodel.uploadModel(uri)
        }
    )

    var modelImageUri by remember { mutableStateOf<Uri?>(null) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { it ->
            modelImageUri = it
        }
    )

    LaunchedEffect(Unit) {
        Log.d("CreateModuleModelScreen", "State: $state")
    }

    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add 3D Model",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D0D1A),
                            Color(0xFF1a1a3e),
                            Color(0xFF0D0D1A)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Custom Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1a1a3e))
                        .padding(4.dp)
                ) {
                    TabButton(
                        text = "📦 Upload Model",
                        isSelected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            viewmodel.changeModelChoice(0)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = "✨ Generate Model",
                        isSelected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            viewmodel.changeModelChoice(1)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Content based on selected tab
                AnimatedVisibility(
                    visible = selectedTab == 0,
                    enter = fadeIn() + scaleIn(initialScale = 0.95f)
                ) {
                    UploadModelContent(
                        modelPath = state.modelPath,
                        onUploadClick = { launcher.launch("model/gltf-binary") },
                        onContinueClick = {
                            viewmodel.createModule()
                            navigateToUploadTracker()
                        }
                    )
                }

                AnimatedVisibility(
                    visible = selectedTab == 1,
                    enter = fadeIn() + scaleIn(initialScale = 0.95f)
                ) {
                    GenerateModelContent(
                        modelImageUri = modelImageUri,
                        generationStatus = state.generationStatus,
                        generatedModelUri = state.modelPath,
                        onSelectImage = { imageLauncher.launch("image/*") },
                        onGenerateClick = {
                            if (modelImageUri != null) {
                                viewmodel.generateModel(modelImageUri!!)
                            }
                        },
                        onContinueClick = {
                            viewmodel.createModule()
                            navigateToUploadTracker()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Color(0xFF7C4DFF) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color(0xFFb0b0d0),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun UploadModelContent(
    modelPath: Uri?,
    onUploadClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Upload Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1a1a3e),
                            Color(0xFF2a2a4e)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = if (modelPath != null)
                            listOf(Color(0xFF4CAF50), Color(0xFF81C784))
                        else
                            listOf(Color(0xFF7C4DFF), Color(0xFFB388FF))
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (modelPath != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ModelViewer(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        modelUri = modelPath
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "✅ Model Loaded",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7C4DFF).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📦", fontSize = 40.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Upload GLB File",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Supports .glb format only",
                        color = Color(0xFFb0b0d0),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Upload Button
        Button(
            onClick = onUploadClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (modelPath != null) Color(0xFF3a3a5e) else Color(0xFF7C4DFF)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (modelPath != null) "Change Model" else "Select GLB File",
                fontFamily = Rajdhani,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Continue Button
        Button(
            onClick = onContinueClick,
            enabled = modelPath != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Create Module",
                    fontFamily = Rajdhani,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "🚀", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun GenerateModelContent(
    modelImageUri: Uri?,
    generationStatus: UploadStatus,
    generatedModelUri: Uri?,
    onSelectImage: () -> Unit,
    onGenerateClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    val isGenerating = generationStatus == UploadStatus.LOADING
    val isSuccess = generationStatus == UploadStatus.SUCCESS && generatedModelUri != null
    
    // If generation was successful, we should probably stick to the success view, 
    // but allow re-generating by picking a new image? 
    // For now let's show the model if available.

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Info Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF7C4DFF).copy(alpha = 0.1f))
                .border(
                    width = 1.dp,
                    color = Color(0xFF7C4DFF).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "✨", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AI Model Generation",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Upload an image and we'll generate a 3D model",
                        color = Color(0xFFb0b0d0),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isSuccess && generatedModelUri != null) {
            // SUCCESS STATE: Show Model
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1a1a3e),
                                Color(0xFF2a2a4e)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ModelViewer(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        modelUri = generatedModelUri
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "✅ Model Generated!",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Continue Button
            Button(
                onClick = onContinueClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create Module",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "🚀", fontSize = 18.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Retry/New Image Button (Text only)
            Text(
                text = "Generate Another",
                color = Color(0xFF7C4DFF),
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable { onSelectImage() }
                    .padding(8.dp)
            )

        } else {
            // INPUT STATE: Image Picker + Generate Button
            
            // Image Upload Area
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1a1a3e), Color(0xFF2a2a4e))
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = if (modelImageUri != null)
                                listOf(Color(0xFF4CAF50), Color(0xFF81C784))
                            else
                                listOf(Color(0xFF7C4DFF), Color(0xFFB388FF))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable(enabled = !isGenerating, onClick = onSelectImage),
                contentAlignment = Alignment.Center
            ) {
                if (modelImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(modelImageUri),
                        contentDescription = "Selected image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // If generating, show overlay
                    if (isGenerating) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color(0xFF7C4DFF)
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🖼️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to select image",
                            color = Color(0xFFb0b0d0),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Generate Button
            Button(
                onClick = onGenerateClick,
                enabled = modelImageUri != null && !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C4DFF),
                    disabledContainerColor = Color(0xFF7C4DFF).copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGenerating) {
                         // Small progress indicator or text
                        Text(
                            text = "Generating...",
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    } else {
                        Text(text = "✨", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate 3D Model",
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
            }
            
            if (isGenerating) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "This may take up to a minute...",
                    color = Color(0xFF6a6a8e),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}