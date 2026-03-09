package com.mcc.signsaya.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mcc.signsaya.R
import com.mcc.signsaya.components.GhostButton
import com.mcc.signsaya.components.PrimaryButton
import com.mcc.signsaya.components.SecondaryButton

@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onCreateAccount: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "SignSaya",
                modifier = Modifier.size(120.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )

            Spacer(Modifier.weight(1f))

            PrimaryButton(text = "Get started", onClick = onCreateAccount)

            Spacer(Modifier.height(12.dp))

            SecondaryButton(text = "I already have an account", onClick = onLogin)

            Spacer(Modifier.height(12.dp))

            GhostButton(text = "Continue without an account  ", onClick = onContinueAsGuest)

            Spacer(Modifier.height(48.dp))
        }
    }
}