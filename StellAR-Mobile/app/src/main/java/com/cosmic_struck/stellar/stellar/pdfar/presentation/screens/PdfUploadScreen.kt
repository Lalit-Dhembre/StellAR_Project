package com.cosmic_struck.stellar.stellar.pdfar.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfUploadScreen(
    onPdfSelected: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val tempFile = File(context.cacheDir, "upload_pdf_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(tempFile)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            onPdfSelected(tempFile)
        }
    }

    // Temporary hook for high-res camera
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoFile?.let { file ->
                onPdfSelected(file)
            }
        }
    }

    fun launchCamera() {
        val photoFile = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
        currentPhotoFile = photoFile
        val photoURI: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
        cameraLauncher.launch(photoURI)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.UploadFile,
            contentDescription = "Upload PDF",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Upload PDF to generate an AR Learning Space",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The AI will extract biologically relevant sections and build an interactive 3D model.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { launchCamera() },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scan Page")
            }
            
            Button(
                onClick = { pdfPickerLauncher.launch("application/pdf") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Select PDF")
            }
        }
    }
}
