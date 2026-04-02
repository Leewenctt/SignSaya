package com.mcc.signsaya.navigation

import android.net.Uri
import com.mcc.signsaya.R

sealed class Screen(val route: String, val title: String, val icon: Int) {
    object Home : Screen("home", "Home", R.drawable.ic_home)
    object Signs : Screen("signs", "Signs", R.drawable.ic_signs)
    object Practice : Screen("practice", "Practice", R.drawable.ic_practice)
    object Translate : Screen("translate", "Translate", R.drawable.ic_translate)
    object Profile : Screen("profile", "Profile", R.drawable.ic_profile)
    object Welcome : Screen("welcome", "", 0)
    object Login : Screen("login", "", 0)
    object SignUp : Screen("signup", "", 0)
    object ForgotPassword : Screen("forgot_password?email={email}", "", 0) {
        fun createRoute(email: String = "") = "forgot_password?email=${Uri.encode(email)}"
    }
    object EmailVerification : Screen("email_verification/{email}", "", 0) {
        fun createRoute(email: String) = "email_verification/${Uri.encode(email)}"
    }
}

val navItems = listOf(Screen.Home, Screen.Signs, Screen.Practice, Screen.Translate)
