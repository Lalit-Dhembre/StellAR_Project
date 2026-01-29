package com.cosmic_struck.stellar.common.navigation

sealed class Screens(val route: String) {
    object HomeScreen: Screens(route = "HomeScreen")
    object StellarHomeScreen : Screens(route = "StellarHomeScreen")
    object ARLabScreen : Screens(route = "ARLabScreen")
    object AuthScreen: Screens(route = "AuthScreen")
    object PhysicsHomeScreen: Screens(route = "PhysicsHomeScreen")
    object BiologyHomeScreen: Screens(route = "BiologyHomeScreen")
    object ChemistryHomeScreen: Screens(route = "ChemistryHomeScreen")
    object HistoryHomeScreen: Screens(route = "HistoryHomeScreen")
    object ProfileScreen: Screens(route = "ProfileScreen")

}
