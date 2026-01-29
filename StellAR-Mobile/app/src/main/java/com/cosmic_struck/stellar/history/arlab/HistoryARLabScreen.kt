package com.cosmic_struck.stellar.history.arlab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cosmic_struck.stellar.common.components.SimpleTopAppBar
import com.cosmic_struck.stellar.history.common.HistoryBottomAppBar
import com.cosmic_struck.stellar.history.common.HistoryScaffold
import com.cosmic_struck.stellar.history.common.Rajdhani

@Composable
fun HistoryARLabScreen(
    navHostController: NavHostController
) {
    HistoryScaffold(
        bottomBar = {
            HistoryBottomAppBar(navHostController)
        },
        topBar = {
            SimpleTopAppBar(
                title = "Time Lab",
                popNavigation = { navHostController.popBackStack() }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Time Capsule AR",
                style = TextStyle(
                    fontFamily = Rajdhani,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = Color.White
                )
            )
            Text(
                text = "Coming Soon: Walk through historic gates",
                 style = TextStyle(
                    fontFamily = Rajdhani,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
        }
    }
}
