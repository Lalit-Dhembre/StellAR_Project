package com.cosmic_struck.stellar.common.util

import androidx.compose.ui.graphics.Color
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.common.navigation.Screens
import com.cosmic_struck.stellar.ui.theme.DarkBlue1

data class GridItems(
    val title: String,
    val icon : Int,
    val navigationRoute: String,
    val color : Color
)

val gridList = listOf<GridItems>(
    GridItems(
        title = "Physics",
        icon = R.drawable.physics_icon,
        navigationRoute = Screens.PhysicsHomeScreen.route,
        color = Color(0xff2962FF)
    ),

    GridItems(
        title = "Space",
        icon = R.drawable.space_icon,
        navigationRoute = Screens.StellarHomeScreen.route,
        color = DarkBlue1
    ),

    GridItems(
        title = "Biology",
        icon = R.drawable.biology_icon,
        navigationRoute = "biology_navigation",  // Navigate directly to biology nav graph
        color = Color(0xff00C853)
    ),

    GridItems(
        title = "Chemistry",
        icon = R.drawable.chemistry_icon,
        navigationRoute = "chemistry_navigation",
        color = Color(0xffFF6D00)
    ),

    GridItems(
        title = "History",
        icon = R.drawable.history_icon,
        navigationRoute = Screens.HistoryHomeScreen.route,
        color = Color(0xffD50000)
    )
)