package com.cosmic_struck.stellar.physics.arlab

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
import com.cosmic_struck.stellar.common.util.Rajdhani
import com.cosmic_struck.stellar.physics.common.PhysicsBottomAppBar
import com.cosmic_struck.stellar.physics.common.PhysicsScaffold

@Composable
fun PhysicsARLabScreen(
    navHostController: NavHostController
) {
    PhysicsScaffold(
        bottomBar = {
            PhysicsBottomAppBar(navHostController)
        },
        topBar = {
            SimpleTopAppBar(
                title = "Atom Lab",
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
                text = "AR Physics Lab",
                fontFamily = Rajdhani,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Color.White,
            )
            Text(
                text = "Coming Soon: Visualize atomic structures in AR",
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
