package com.mcc.signsaya

sealed class Screen(val route: String, val title: String, val icon: Int) {
    object Home : Screen("home", "Home", R.drawable.ic_home)
    object Practice : Screen("practice", "Practice", R.drawable.ic_practice)
    object Translate : Screen("translate", "Translate", R.drawable.ic_translate)
    object Profile : Screen("profile", "Profile", R.drawable.ic_profile)
    object Welcome : Screen("welcome", "", 0)
}

val navItems = listOf(Screen.Home, Screen.Practice, Screen.Translate, Screen.Profile)