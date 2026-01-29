package com.cosmic_struck.stellar.chemistry.models

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cosmic_struck.stellar.chemistry.common.ChemistryBottomAppBar
import com.cosmic_struck.stellar.chemistry.common.ChemistryScaffold
import com.cosmic_struck.stellar.chemistry.domain.model.lockedChemistryModels
import com.cosmic_struck.stellar.chemistry.domain.model.sampleChemistryModels
import com.cosmic_struck.stellar.chemistry.models.components.ChemistryModelCard
import com.cosmic_struck.stellar.common.components.SimpleTopAppBar
import com.cosmic_struck.stellar.common.components.TabSwitcher

@Composable
fun ChemistryModelsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedTabIndex = remember { mutableStateOf(0) }

    ChemistryScaffold(
        topBar = {
            SimpleTopAppBar(
                title = "Chemistry Models",
                popNavigation = {
                    navController.popBackStack()
                }
            )
        },
        bottomBar = {
            ChemistryBottomAppBar(navController = navController)
        }
    ) {
        Column(
            modifier = it.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            TabSwitcher(
                options = listOf("My Collection", "Locked Models"),
                nonActiveTextColor = Color.White,
                modifier = Modifier
                    .height(40.dp)
                    .padding(horizontal = 16.dp),
                initialIndex = selectedTabIndex.value,
                onOptionSelected = {
                    selectedTabIndex.value = if (selectedTabIndex.value == 0) 1 else 0
                },
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val modelsToShow = if (selectedTabIndex.value == 0) {
                    sampleChemistryModels
                } else {
                    lockedChemistryModels
                }

                items(items = modelsToShow) { model ->
                    ChemistryModelCard(
                        locked = selectedTabIndex.value == 1,
                        onClickModel = {
                            // Show toast for now since model viewer isn't connected
                            Toast.makeText(
                                context,
                                "Opening ${model.name}... (Coming soon!)",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        model = model,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
