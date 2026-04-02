package com.mcc.signsaya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mcc.signsaya.navigation.SignSayaApp
import com.mcc.signsaya.ui.theme.SignSayaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = applicationContext as SignSayaApplication
        val repository = app.authRepository
        val currentUser = repository.currentUser

        val isUserLoggedIn = currentUser != null
        val isEmailVerified = currentUser?.isEmailVerified == true
        val isVerificationDismissed = repository.isVerificationDismissed()
        val userEmail = currentUser?.email ?: ""

        setContent {
            SignSayaTheme {
                SignSayaApp(
                    isUserLoggedIn = isUserLoggedIn,
                    isEmailVerified = isEmailVerified,
                    isVerificationDismissed = isVerificationDismissed,
                    userEmail = userEmail
                )
            }
        }
    }
}
