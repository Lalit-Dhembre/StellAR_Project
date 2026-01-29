package com.cosmic_struck.stellar.biology.arlab

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.cosmic_struck.stellar.biology.arlab.components.BiologyGameCard
import com.cosmic_struck.stellar.biology.common.BiologyBottomAppBar
import com.cosmic_struck.stellar.biology.common.BiologyScaffold
import com.cosmic_struck.stellar.biology.domain.model.biologyGames
import com.cosmic_struck.stellar.common.components.SimpleTopAppBar

@Composable
fun BiologyARLabScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BiologyScaffold(
        modifier = Modifier
            .fillMaxSize(),
        bottomBar = {
            BiologyBottomAppBar(navController = navController)
        },
        topBar = {
            SimpleTopAppBar(
                title = "Biology AR Lab",
                popNavigation = {
                    navController.popBackStack()
                }
            )
        }
    ) { it ->
        Box(
            modifier = it
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(
                    items = biologyGames
                ) { game ->
                    BiologyGameCard(
                        onClick = { route ->
                            // For now, just show a toast since backend isn't implemented
                            Toast.makeText(context, "Coming soon: ${game.title}", Toast.LENGTH_SHORT).show()
                        },
                        gameModel = game
                    )
                }
            }
        }
    }
}
