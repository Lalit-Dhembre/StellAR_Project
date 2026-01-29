package com.cosmic_struck.stellar.home.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.common.components.CustomExpandableFAB
import com.cosmic_struck.stellar.common.components.FABItem
import com.cosmic_struck.stellar.common.components.TabSwitcher
import com.cosmic_struck.stellar.common.util.Rajdhani
import com.cosmic_struck.stellar.common.util.gridList
import com.cosmic_struck.stellar.home.presentation.ClassroomJoinStatus
import com.cosmic_struck.stellar.home.presentation.Options
import com.cosmic_struck.stellar.home.presentation.components.ClassroomCard
import com.cosmic_struck.stellar.home.presentation.components.GridItem
import com.cosmic_struck.stellar.home.presentation.components.JoinClassroomBottomSheet
import com.cosmic_struck.stellar.home.presentation.components.UserTopBar
import com.cosmic_struck.stellar.home.presentation.viewmodel.HomeScreenViewModel
import com.cosmic_struck.stellar.home.presentation.components.HomeBackground
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay

// Educational Theme Colors
private val EduBackground = Color(0xFFF8F9FE)
private val EduPrimary = Color(0xFF5C6BC0)
private val EduTextSecondary = Color(0xFF6B7280)

@Composable
fun HomeScreen(
    navigateToModuleScreen: (String) -> Unit,
    navigateToClassroomHomeScreen: (String) -> Unit,
    navigateToProfileScreen: () -> Unit = {},
    viewModel: HomeScreenViewModel = hiltViewModel<HomeScreenViewModel>(),
    modifier: Modifier = Modifier
) {
    val state = viewModel.state.collectAsState().value
    val context = LocalContext.current

    Scaffold(
        containerColor = EduBackground,
        floatingActionButton = {
            if (state.selected == Options.CLASSROOM) {
                CustomExpandableFAB(
                    items = listOf(
                        FABItem(
                            icon = painterResource(R.drawable.add),
                            text = "Add Classroom"
                        ),
                        FABItem(
                            icon = painterResource(R.drawable.handshake),
                            text = "Join Classroom"
                        ),
                    ),
                    onItemClick = { item ->
                        when (item.text) {
                            "Join Classroom" -> viewModel.changeModalSheetState()
                            "Add Classroom" -> {}
                        }
                    }
                )
            }
        },
        topBar = {
            UserTopBar(
                userName = state.userName,
                userLevel = state.userLevel,
                userPic = state.profile,
                onProfileClick = navigateToProfileScreen
            )
        }
    ) { paddingValues ->

        // Toast handling
        if (state.classroomJoinStatus == ClassroomJoinStatus.JOINED) {
            Toast.makeText(context, "✅ Joined Classroom Successfully", Toast.LENGTH_SHORT).show()
            viewModel.toggleJoinClassroomStatus()
        } else if (state.classroomJoinStatus == ClassroomJoinStatus.ERROR) {
            Toast.makeText(context, "❌ Error Joining Classroom", Toast.LENGTH_SHORT).show()
            viewModel.toggleJoinClassroomStatus()
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            HomeBackground() // Animated Background
            
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Tab Switcher
                TabSwitcher(
                    onOptionSelected = { viewModel.onToggle(it) },
                    options = listOf("📚 Modules", "🏫 Classrooms"),
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 20.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Swipe to refresh
                var refreshing by remember { mutableStateOf(false) }
                LaunchedEffect(refreshing) {
                    if (refreshing) {
                        delay(2000)
                        refreshing = false
                    }
                }

                SwipeRefresh(
                    state = rememberSwipeRefreshState(refreshing),
                    onRefresh = {
                        refreshing = true
                        viewModel.refresh()
                    },
                    indicator = { state, trigger ->
                        SwipeRefreshIndicator(
                            state = state,
                            refreshTriggerDistance = trigger,
                            contentColor = EduPrimary,
                            backgroundColor = Color.White
                        )
                    }
                ) {
                    if (state.selected == Options.MODULES) {
                        // Modules Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(gridList) { item ->
                                GridItem(
                                    onClick = { navigateToModuleScreen(item.navigationRoute) },
                                    color = item.color,
                                    title = item.title,
                                    icon = item.icon
                                )
                            }
                        }
                    } else {
                        // Classrooms List
                        if (state.joinedClassrooms.isEmpty()) {
                            // Empty state
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "🏫",
                                        fontSize = 64.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No Classrooms Yet",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = Rajdhani,
                                        color = Color(0xFF1A1A2E)
                                    )
                                    Text(
                                        text = "Join or create a classroom to get started",
                                        fontSize = 14.sp,
                                        fontFamily = Rajdhani,
                                        color = EduTextSecondary
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.joinedClassrooms.size) { index ->
                                    val classroom = state.joinedClassrooms[index]
                                    ClassroomCard(
                                        onClick = {
                                            navigateToClassroomHomeScreen(classroom.classroom_id)
                                        },
                                        classroom = classroom
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom sheet for joining classroom
            JoinClassroomBottomSheet(
                modalSheetState = state.modalSheetState,
                onValueChange = viewModel::setCode,
                codeText = state.codeText,
                onDismiss = viewModel::changeModalSheetState,
                onSubmit = viewModel::joinClassroom
            )
        }
    }
    }
}
