package com.mcc.signsaya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import com.mcc.signsaya.ui.theme.SignSayaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val currentUser = FirebaseAuth.getInstance().currentUser
        // A user is considered "logged in" if they have an active session, 
        // regardless of email verification status.
        val isUserLoggedIn = currentUser != null

        setContent {
            SignSayaTheme {
                SignSayaApp(isUserLoggedIn = isUserLoggedIn)
            }
        }
    }
}