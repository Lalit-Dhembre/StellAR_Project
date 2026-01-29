package com.cosmic_struck.stellar.stellar.arlab.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.cosmic_struck.stellar.common.components.StellarScaffold
import com.cosmic_struck.stellar.common.components.BottomAppBar
import com.cosmic_struck.stellar.common.components.SimpleTopAppBar
import com.cosmic_struck.stellar.stellar.arlab.domain.model.games
import com.cosmic_struck.stellar.stellar.arlab.presentation.component.GameCard


@Composable
fun ARLabScreen(
    navController: NavController,
    modifier: Modifier = Modifier) {

    StellarScaffold(
        modifier = Modifier
            .fillMaxSize(),
        bottomBar = {
            BottomAppBar(
                navController = navController
            )
        },
        topBar = {
            SimpleTopAppBar(
                title = "AR Lab",
                popNavigation = {
                    navController.popBackStack()
                }
            )
        }
    ) {it->
        Box(
            modifier = it
                .fillMaxSize()
        ){
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(
                    items = games
                ){it->
                    GameCard(
                        onClick = {it->
                            navController.navigate(it)
                        },
                        gameModel = it
                    )
                }
            }
        }
    }
}