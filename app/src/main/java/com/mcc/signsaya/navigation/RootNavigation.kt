package com.mcc.signsaya.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.mcc.signsaya.SignSayaApplication
import com.mcc.signsaya.ui.components.SignSayaBottomBar
import com.mcc.signsaya.feature.auth.screen.EmailVerificationScreen
import com.mcc.signsaya.feature.auth.screen.ForgotPasswordScreen
import com.mcc.signsaya.feature.auth.screen.LoginScreen
import com.mcc.signsaya.feature.auth.screen.SignUpScreen
import com.mcc.signsaya.feature.onboarding.screen.LandingScreen
import com.mcc.signsaya.feature.home.screen.HomeScreen
import com.mcc.signsaya.feature.practice.screen.PracticeScreen
import com.mcc.signsaya.feature.profile.screen.ProfileScreen
import com.mcc.signsaya.feature.signs.screen.SignsScreen
import com.mcc.signsaya.feature.translate.screen.TranslateScreen
import com.mcc.signsaya.feature.auth.viewmodel.LoginViewModel

@Composable
fun SignSayaApp(
    isUserLoggedIn: Boolean,
    isEmailVerified: Boolean,
    isVerificationDismissed: Boolean,
    userEmail: String
) {

    val app = LocalContext.current.applicationContext as SignSayaApplication
    val viewModelFactory = app.viewModelFactory
    val repository = app.authRepository

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val startDestination = when {
        !isUserLoggedIn -> Screen.Welcome.route
        isEmailVerified || isVerificationDismissed -> Screen.Home.route
        else -> Screen.EmailVerification.createRoute(userEmail)
    }

    fun NavHostController.navigateBottomBar(route: String) {
        navigate(route) {
            popUpTo(Screen.Home.route) {
                saveState = true
                inclusive = false
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Signs.route,
        Screen.Practice.route,
        Screen.Translate.route,
        Screen.Profile.route
    )

    val showBottomBar = currentDestination?.route in bottomBarRoutes
    val isOnHome = showBottomBar && currentDestination?.route == Screen.Home.route
    val isOnOtherTab = showBottomBar && currentDestination?.route != Screen.Home.route

    val context = LocalContext.current
    var lastBackPressedTime by remember { mutableLongStateOf(0L) }


    BackHandler(enabled = isOnOtherTab) {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) {
                saveState = true
                inclusive = false
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BackHandler(enabled = isOnHome) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressedTime < 2000) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressedTime = currentTime
            Toast.makeText(context, "Press back again to exit the app.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                SignSayaBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = navController::navigateBottomBar
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) },
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
        ) {
            composable(Screen.Home.route) {
                HomeScreen(innerPadding)
            }
            composable(Screen.Signs.route) {
                SignsScreen(innerPadding)
            }
            composable(Screen.Practice.route) {
                PracticeScreen(innerPadding)
            }
            composable(Screen.Translate.route) {
                TranslateScreen(innerPadding)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    paddingValues = innerPadding,
                    onLogout = {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    viewModel = viewModel(factory = viewModelFactory)
                )
            }

            composable(Screen.Welcome.route) {
                LandingScreen(
                    onLogin = {
                        navController.navigate(Screen.Login.route) {
                            launchSingleTop = true
                        }
                    },
                    onCreateAccount = {
                        navController.navigate(Screen.SignUp.route) {
                            launchSingleTop = true
                        }
                    },
                    onContinueAsGuest = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
                LoginScreen(
                    onBack = {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    },
                    onLoginSuccess = {
                        val user = repository.currentUser
                        val emailVerified = user?.isEmailVerified == true
                        val verificationDismissed = repository.isVerificationDismissed()
                        
                        if (emailVerified || verificationDismissed) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.EmailVerification.createRoute(user?.email ?: "")) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    },
                    onNavigateToSignUp = {
                        navController.navigate(Screen.SignUp.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(Screen.ForgotPassword.createRoute(loginViewModel.state.value.email)) {
                            launchSingleTop = true
                        }
                    },
                    viewModel = loginViewModel
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onBack = {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            launchSingleTop = true
                        }
                    },
                    onSignUpSuccess = { email ->
                        navController.navigate(Screen.EmailVerification.createRoute(email)) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    },
                    onGoogleSignUpSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    },
                    viewModel = viewModel(factory = viewModelFactory)
                )
            }

            composable(
                route = Screen.ForgotPassword.route,
                arguments = listOf(navArgument("email") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                ForgotPasswordScreen(
                    onBack = { navController.popBackStack() },
                    initialEmail = email,
                    viewModel = viewModel(factory = viewModelFactory)
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
                    viewModel = viewModel(factory = viewModelFactory)
                )
            }
        }
    }
}
