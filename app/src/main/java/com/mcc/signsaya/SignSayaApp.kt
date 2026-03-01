package com.mcc.signsaya

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.mcc.signsaya.components.SignSayaBottomBar
import com.mcc.signsaya.screens.home.HomeScreen
import com.mcc.signsaya.screens.practice.PracticeScreen
import com.mcc.signsaya.screens.profile.ProfileScreen
import com.mcc.signsaya.screens.translate.TranslateScreen
import com.mcc.signsaya.screens.auth.WelcomeScreen

@Composable
fun SignSayaApp() {

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun NavHostController.navigateBottomBar(route: String) {
        navigate(route) {
            popUpTo(graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val showBottomBar = currentDestination?.route !in listOf(
        Screen.Welcome.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SignSayaBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = navController::navigateBottomBar
                )
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Screen.Welcome.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Practice.route) { PracticeScreen() }
            composable(Screen.Translate.route) { TranslateScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(Screen.Welcome.route) { WelcomeScreen(
                onLogin = { },
                onCreateAccount = { },
                onContinueAsGuest = { navController.navigate(Screen.Home.route) }
            )}
        }
    }
}