package com.cosmic_struck.stellar.common.components

import android.app.Activity
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.ui.theme.Blue5
import com.cosmic_struck.stellar.ui.theme.DarkBlue1
import dev.chrisbanes.haze.HazeState

@Composable
fun BackgroundScaffold(
    floatingActionButton: @Composable ()-> Unit = {},
    color: Color = DarkBlue1,
    modifier: Modifier = Modifier,
    topBar: @Composable ()-> Unit = {},
    bottomBar: @Composable ()-> Unit = {},
    navController: NavController = rememberNavController(),
    content: @Composable (modifier: Modifier)-> Unit) {
    val hazeState = remember { HazeState() }
    val view = LocalView.current
    val window = (view.context as Activity).window
    SideEffect {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, view).apply {
            hide(WindowInsets.Type.navigationBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    Scaffold(
        floatingActionButton = floatingActionButton,
        topBar = topBar,
        modifier = modifier
            .background(
                color = color
            )
            .fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            bottomBar()
        }
    ) {it->
        Image(
            painter = painterResource(R.drawable.starry_bg),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
        )
        content(modifier.padding(it))
    }
}