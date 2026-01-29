package com.cosmic_struck.stellar.biology.common

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.biology.navigation.BiologyNavigationScreens
import com.cosmic_struck.stellar.common.util.Rajdhani

data class BiologyBottomAppBarItem(
    val title: String,
    val route: String,
    val secondRoute: String,
    @DrawableRes val image: Int
)

val biologyBottomAppBarItems = listOf(
    BiologyBottomAppBarItem(
        title = "Home",
        route = "biology_navigation",
        image = R.drawable.vector,
        secondRoute = BiologyNavigationScreens.BiologyHomeScreen.route
    ),
    BiologyBottomAppBarItem(
        title = "Models",
        route = BiologyNavigationScreens.BiologyModels.route,
        image = R.drawable.db,
        secondRoute = BiologyNavigationScreens.BiologyModels.route
    ),
    BiologyBottomAppBarItem(
        title = "Bio Lab",
        route = BiologyNavigationScreens.BiologyARLab.route,
        image = R.drawable.beaker,
        secondRoute = BiologyNavigationScreens.BiologyARLab.route
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BiologyBottomAppBar(navController: NavController) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 25.dp)
            .clip(shape = RoundedCornerShape(50.dp)),
        containerColor = BiologyBackground1.copy(alpha = 0.3f),
        tonalElevation = 10.dp
    ) {
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry.value?.destination?.route
        Log.d("Biology Route Checking", currentRoute.toString())
        biologyBottomAppBarItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.secondRoute,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(item.image),
                        contentDescription = null,
                        modifier = Modifier.size(25.dp),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                },
                label = {
                    Text(
                        text = item.title,
                        style = TextStyle(
                            fontSize = 12.sp,
                            lineHeight = 20.sp,
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight(700),
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp,
                        )
                    )
                },
                colors = NavigationBarItemColors(
                    selectedIconColor = BioGlow,
                    selectedTextColor = BioGlow,
                    selectedIndicatorColor = Color.Transparent,
                    unselectedIconColor = LeafGreen.copy(alpha = 0.6f),
                    unselectedTextColor = LeafGreen.copy(alpha = 0.6f),
                    disabledIconColor = Color.Transparent,
                    disabledTextColor = Color.Transparent
                )
            )
        }
    }
}
