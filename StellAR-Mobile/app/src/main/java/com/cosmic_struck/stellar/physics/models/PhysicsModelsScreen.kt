package com.cosmic_struck.stellar.physics.models

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
import com.cosmic_struck.stellar.physics.common.PhysicsBottomAppBar
import com.cosmic_struck.stellar.physics.common.PhysicsScaffold
import com.cosmic_struck.stellar.stellar.models.presentation.components.PlanetCard

@Composable
fun PhysicsModelsScreen(
    navHostController: NavHostController
) {
    // Mock Data for Physics Models
    val physicsModels = remember {
        listOf(
            ClassroomModel(
                model_id = "1",
                model_name = "Atom Structure",
                model_url = "https://example.com/atom.glb",
                model_thumbnail = "https://cdn.pixabay.com/photo/2016/11/08/04/49/atom-1807469_1280.png",
                description = "Structure of an Atom",
                xp_reward = 100,
                rarity = "Common",
                model_subject = "Physics",
                min_level = 1,
                created_at = "2024-01-01"
            ),
            ClassroomModel(
                model_id = "2",
                model_name = "Magnetic Field",
                model_url = "https://example.com/magnet.glb",
                model_thumbnail = "https://cdn.pixabay.com/photo/2012/04/13/11/58/magnet-32066_1280.png",
                description = "Magnetic Field Lines",
                xp_reward = 150,
                rarity = "Rare",
                 model_subject = "Physics",
                min_level = 1,
                created_at = "2024-01-01"
            ),
             ClassroomModel(
                model_id = "3",
                model_name = "Gravity Well",
                model_url = "https://example.com/gravity.glb",
                model_thumbnail = "https://cdn.pixabay.com/photo/2017/08/15/12/58/black-hole-2643669_1280.jpg",
                description = "Spacetime curvature",
                xp_reward = 200,
                rarity = "Legendary",
                 model_subject = "Physics",
                min_level = 1,
                created_at = "2024-01-01"
            ),
            ClassroomModel(
                model_id = "4",
                model_name = "Prism",
                model_url = "https://example.com/prism.glb",
                model_thumbnail = "https://cdn.pixabay.com/photo/2013/07/12/12/40/prism-146054_1280.png",
                description = "Light Refraction",
                xp_reward = 80,
                rarity = "Common",
                 model_subject = "Physics",
                min_level = 1,
                created_at = "2024-01-01"
            ),
             ClassroomModel(
                model_id = "5",
                model_name = "Circuit",
                model_url = "https://example.com/circuit.glb",
                model_thumbnail = "https://cdn.pixabay.com/photo/2016/04/01/10/58/electricity-1300435_1280.png",
                description = "Simple Electric Circuit",
                xp_reward = 120,
                rarity = "Rare",
                 model_subject = "Physics",
                min_level = 1,
                created_at = "2024-01-01"
            )
        )
    }

    PhysicsScaffold(
        bottomBar = {
            PhysicsBottomAppBar(navHostController)
        },
        topBar = {
            SimpleTopAppBar(
                title = "Physics Models",
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
                items(physicsModels) { model ->
                    PlanetCard(
                        locked = false,
                        navigateToModelViewer = { url, name ->
                            // Navigate to generic Model Viewer
                            // Assuming modelNavGraph handles "model_viewer/{modelName}/{modelUrl}"
                            // Need to encode URL if passing as arg, or use a shared ViewModel.
                            // For simplicity, passing directly if the graph supports it.
                            // The route in ModelNavGraph is likely "model_screen/{url}/{name}"
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
