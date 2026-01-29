package com.cosmic_struck.stellar.chemistry.arlab

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.cosmic_struck.stellar.chemistry.arlab.components.ChemistryGameCard
import com.cosmic_struck.stellar.chemistry.common.ChemistryBottomAppBar
import com.cosmic_struck.stellar.chemistry.common.ChemistryScaffold
import com.cosmic_struck.stellar.chemistry.domain.model.chemistryGames
import com.cosmic_struck.stellar.chemistry.navigation.ChemistryNavigationScreens
import com.cosmic_struck.stellar.common.components.SimpleTopAppBar

@Composable
fun ChemistryARLabScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    ChemistryScaffold(
        modifier = Modifier
            .fillMaxSize(),
        bottomBar = {
            ChemistryBottomAppBar(navController = navController)
        },
        topBar = {
            SimpleTopAppBar(
                title = "Chemistry AR Lab",
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
                    items = chemistryGames
                ) { game ->
                    ChemistryGameCard(
                        onClick = { route ->
                            // For now, just show a toast since backend isn't implemented
                            if(game == chemistryGames[1]){
                                navController.navigate(ChemistryNavigationScreens.ReactionLab.route)
                            }
                            else{
                                Toast.makeText(context, "Coming soon: ${game.title}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        gameModel = game
                    )
                }
            }
        }
    }
}
