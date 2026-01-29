package com.cosmic_struck.stellar.history.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cosmic_struck.stellar.classroom.data.dto.ClassroomModel
import com.cosmic_struck.stellar.common.components.SimpleTopAppBar
import com.cosmic_struck.stellar.history.common.HistoryBottomAppBar
import com.cosmic_struck.stellar.history.common.HistoryScaffold
import com.cosmic_struck.stellar.stellar.models.presentation.components.PlanetCard

@Composable
fun HistoryModelsScreen(
    navHostController: NavHostController
) {
    // Mock Data for History Models
    val historyModels = remember {
        listOf(
            ClassroomModel(
                model_id = "1",
                model_name = "Great Pyramid",
                model_url = "https://example.com/pyramid.glb",
                model_thumbnail = "https://cdn.pixabay.com/photo/2016/11/14/03/46/pyramid-1822502_1280.jpg",
                description = "Wonder of Ancient Egypt",
                xp_reward = 200,
                rarity = "Legendary",
                model_subject = "History",
                min_level = 1,
                created_at = "1000 BC"
            ),
             ClassroomModel(
                 model_id = "2",
                model_name = "Colosseum",
                model_url = "https://example.com/colosseum.glb",
                model_thumbnail = "https://cdn.pixabay.com/photo/2017/08/07/19/22/rome-2608432_1280.jpg",
                description = "Amphitheatre in Rome",
                xp_reward = 180,
                rarity = "Rare",
                model_subject = "History",
                min_level = 1,
                created_at = "70 AD"
            ),
             ClassroomModel(
                 model_id = "3",
                model_name = "Taj Mahal",
                model_url = "https://example.com/tajmahal.glb",
                model_thumbnail = "https://cdn.pixabay.com/photo/2020/06/05/21/09/cultural-tourism-5264542_1280.jpg",
                description = "Symbol of Love",
                xp_reward = 150,
                rarity = "Rare",
                model_subject = "History",
                min_level = 1,
                created_at = "1632"
            ),
            ClassroomModel(
                model_id = "4",
                model_name = "Eiffel Tower",
                model_url = "https://example.com/eiffel.glb",
                model_thumbnail = "https://cdn.pixabay.com/photo/2019/08/19/15/13/eiffel-tower-4416700_1280.jpg",
                description = "Iron Lady of Paris",
                xp_reward = 100,
                rarity = "Common",
                model_subject = "History",
                min_level = 1,
                created_at = "1889"
            ),
             ClassroomModel(
                 model_id = "5",
                model_name = "Statue of Liberty",
                model_url = "https://example.com/liberty.glb",
                model_thumbnail = "https://cdn.pixabay.com/photo/2021/04/18/13/35/liberty-statue-6188654_1280.jpg",
                description = "Symbol of Freedom",
                xp_reward = 120,
                rarity = "Common",
                model_subject = "History",
                min_level = 1,
                created_at = "1886"
            )
        )
    }

    HistoryScaffold(
        bottomBar = {
            HistoryBottomAppBar(navHostController)
        },
        topBar = {
            SimpleTopAppBar(
                title = "Monuments",
                popNavigation = { navHostController.popBackStack() }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(historyModels) { model ->
                    PlanetCard(
                        locked = false,
                        navigateToModelViewer = { url, name ->
                             val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                             navHostController.navigate("model_screen/$encodedUrl/$name")
                        },
                        planet = model
                    )
                }
            }
        }
    }
}
