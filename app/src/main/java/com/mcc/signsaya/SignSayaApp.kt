package com.mcc.signsaya

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.mcc.signsaya.components.SignSayaBottomBar
import com.mcc.signsaya.screens.auth.EmailVerificationScreen
import com.mcc.signsaya.screens.auth.SignUpScreen
import com.mcc.signsaya.screens.auth.WelcomeScreen
import com.mcc.signsaya.screens.home.HomeScreen
import com.mcc.signsaya.screens.practice.PracticeScreen
import com.mcc.signsaya.screens.profile.ProfileScreen
import com.mcc.signsaya.screens.translate.TranslateScreen

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

    val authRoutes = listOf(
        Screen.Welcome.route,
        Screen.Login.route,
        Screen.SignUp.route,
        Screen.EmailVerification.route
    )

    val showBottomBar = currentDestination?.route !in authRoutes

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

            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onLogin = { navController.navigate(Screen.Login.route) },
                    onCreateAccount = { navController.navigate(Screen.SignUp.route) },
                    onContinueAsGuest = { navController.navigate(Screen.Home.route) }
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onBack = { navController.popBackStack() },
                    onSignUpSuccess = { email ->
                        navController.navigate(Screen.EmailVerification.createRoute(email)) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.EmailVerification.route,
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                EmailVerificationScreen(
                    email = email,
                    onVerified = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}