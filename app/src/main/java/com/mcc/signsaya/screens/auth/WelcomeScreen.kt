package com.mcc.signsaya.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
    var isNavigating by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "SignSaya",
                modifier = Modifier.size(120.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )

            Spacer(Modifier.weight(1f))

            PrimaryButton(
                text = "GET STARTED",
                onClick = {
                    if (!isNavigating) {
                        isNavigating = true
                        onCreateAccount()
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            SecondaryButton(
                text = "I ALREADY HAVE AN ACCOUNT",
                onClick = {
                    if (!isNavigating) {
                        isNavigating = true
                        onLogin()
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            GhostButton(
                text = "CONTINUE AS GUEST",
                onClick = {
                    if (!isNavigating) {
                        isNavigating = true
                        onContinueAsGuest()
                    }
                }
            )

            Spacer(Modifier.height(52.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "By continuing, you agree to our ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Terms of Service",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* TODO */ }
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "and ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* TODO */ }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}