package com.cosmic_struck.stellar.stellar.scantext.presentation.components

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScanText(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    if (permissionState.status.isGranted) {
        CameraContentScanText(imageCapture)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Camera permission is required to use this feature.")
        }
    }
}

@Composable
fun CameraContentScanText(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier
){
    val context = LocalContext.current
    val lifeCycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }



    AndroidView(
        factory = {previewView},
        modifier = modifier.fillMaxSize(),
        update = {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                preview.surfaceProvider = previewView.surfaceProvider

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try{
                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        lifeCycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                }catch (e: Exception){
                    Log.e("CameraXX","Use case binding failed",e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

