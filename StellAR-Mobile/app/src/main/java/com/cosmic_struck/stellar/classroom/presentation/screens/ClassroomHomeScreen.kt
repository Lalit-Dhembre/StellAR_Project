package com.cosmic_struck.stellar.classroom.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.classroom.presentation.components.ClassroomTopAppBar
import com.cosmic_struck.stellar.classroom.presentation.components.MemberCard
import com.cosmic_struck.stellar.classroom.presentation.components.ModelCardClassroom
import com.cosmic_struck.stellar.classroom.presentation.viewmodel.ClassroomViewModel
import com.cosmic_struck.stellar.classroom.presentation.viewmodel.Options
import com.cosmic_struck.stellar.common.components.CustomExpandableFAB
import com.cosmic_struck.stellar.common.components.FABItem
import com.cosmic_struck.stellar.common.components.TabSwitcher
import com.cosmic_struck.stellar.common.util.Rajdhani

// Educational Theme Colors
private val EduBackground = Color(0xFFF8F9FE)
private val EduPrimary = Color(0xFF5C6BC0)
private val EduTextSecondary = Color(0xFF6B7280)

@Composable
fun ClassroomHomeScreen(
    navigateToModelScreen: () -> Unit,
    navigateToCreateModuleScreen: (String) -> Unit,
    viewmodel: ClassroomViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state = viewmodel.homeState.collectAsState().value

    Scaffold(
        containerColor = EduBackground,
        topBar = {
            ClassroomTopAppBar(
                classroomMembers = state.classroomMembers,
                classroomName = state.classroomName,
                classroomAuthor = state.classroomAuthor,
                classroomCode = state.classroomCode,
            )
        },
        floatingActionButton = {
            if (state.isCreator) {
                CustomExpandableFAB(
                    items = listOf(
                        FABItem(
                            icon = painterResource(R.drawable.add),
                            text = "Add Module"
                        )
                    ),
                    onItemClick = { item ->
                        when (item.text) {
                            "Add Module" -> {
                                navigateToCreateModuleScreen(state.classroom_id)
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8F9FE),
                            Color(0xFFE8EAF6),
                            Color(0xFFF8F9FE)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Tab Switcher
                TabSwitcher(
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 20.dp),
                    options = listOf("👥 Members", "📦 Modules"),
                    onOptionSelected = { viewmodel.onToggle(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (state.selected == Options.MEMBERS) {
                    // Members List
                    if (state.classroomMembersList.isEmpty()) {
                        EmptyState(
                            emoji = "👥",
                            title = "No Members Yet",
                            subtitle = "Share the classroom code to invite members"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.classroomMembersList) { member ->
                                MemberCard(name = member.user_name)
                            }
                        }
                    }
                } else {
                    // Modules List
                    if (state.classroomModelsList.isEmpty()) {
                        EmptyState(
                            emoji = "📦",
                            title = "No Modules Yet",
                            subtitle = if (state.isCreator) "Tap + to create your first module" else "Modules will appear here"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.classroomModelsList) { module ->
                                ModelCardClassroom(
                                    navigateToModelScreen = {
                                        viewmodel.setModuleId(module.id)
                                        navigateToModelScreen()
                                    },
                                    modelThumbnail = module.imageUrl ?: "",
                                    modelName = module.moduleName ?: "",
                                    modelDescription = module.moduleDesc ?: ""
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Rajdhani,
                color = Color(0xFF1A1A2E)
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                fontFamily = Rajdhani,
                color = EduTextSecondary
            )
        }
    }
}