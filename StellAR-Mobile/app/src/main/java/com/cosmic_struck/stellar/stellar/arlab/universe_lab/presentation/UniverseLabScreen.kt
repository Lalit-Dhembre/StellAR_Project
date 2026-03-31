package com.cosmic_struck.stellar.stellar.arlab.universe_lab.presentation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.raylib.raymob.NativeLoader

@Composable
fun UniverseLabScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val intent = Intent(context, NativeLoader::class.java)
        context.startActivity(intent)
        navigateBack()
    }
}
