package com.mcc.signsaya.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.signsaya.components.BackButton
import com.mcc.signsaya.components.PrimaryButton
import com.mcc.signsaya.viewmodel.EmailVerificationViewModel
import kotlinx.coroutines.delay

@Composable
fun EmailVerificationScreen(
    email: String,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: EmailVerificationViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    var isNavigating by remember { mutableStateOf(false) }

    val handleBack = {
        if (!isNavigating) {
            isNavigating = true
            viewModel.dismissBannerError()
            viewModel.dismissBannerSuccess()
            onBack()
        }
    }

    // Clear banner when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.dismissBannerError()
            viewModel.dismissBannerSuccess()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onScreenEntered()
    }

    LaunchedEffect(state.navigateToHome) {
        if (state.navigateToHome) {
            viewModel.onNavigationHandled()
            onVerified()
        }
    }

    // Auto-dismiss error banner after 3 seconds
    LaunchedEffect(state.bannerError) {
        if (state.bannerError != null) {
            delay(3000)
            viewModel.dismissBannerError()
        }
    }

    // Auto-dismiss success banner after 3 seconds  
    LaunchedEffect(state.bannerSuccess) {
        if (state.bannerSuccess != null) {
            delay(3000)
            viewModel.dismissBannerSuccess()
        }
    }

    if (state.sessionExpired) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Session expired") },
            text = {
                Text(
                    "Your session has expired. Please sign up again to continue.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = handleBack) { Text("Go back") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (state.pollingTimedOut) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Still waiting?") },
            text = {
                Text(
                    "We've been checking for a while but your email hasn't been verified yet. " +
                            "Check your inbox or spam folder.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.checkVerification() }) { Text("Check now") }
            },
            dismissButton = {
                TextButton(onClick = handleBack) { Text("Go back") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BackButton(
                    onClick = handleBack,
                    enabled = !isNavigating,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "Check your email",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "We sent a verification link to",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Click the link in the email, then come back here. " +
                            "This page will update automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedContent(
                    targetState = state.networkError,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "network_error"
                ) { hasNetworkError ->
                    if (hasNetworkError) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No internet connection — we'll keep checking when you're back online.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Spacer(Modifier.height(0.dp))
                    }
                }

                Spacer(Modifier.height(48.dp))

                // Waiting indicator
                if (!state.pollingTimedOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Waiting for verification…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Resend button - PrimaryButton styled like Create Account
                PrimaryButton(
                    text = if (state.resendCooldownSeconds > 0) "Resend in ${state.resendCooldownSeconds}s" else "Resend verification link",
                    enabled = state.resendCooldownSeconds == 0 && !state.isResending,
                    loading = state.isResending,
                    onClick = { viewModel.resendVerificationEmail() }
                )

                Spacer(Modifier.height(40.dp))
            }

            // Full-width error banner at bottom
            AnimatedVisibility(
                visible = state.bannerError != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 32.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.bannerError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Full-width success banner at bottom (shown above error if both present)
            AnimatedVisibility(
                visible = state.bannerSuccess != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 32.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.bannerSuccess ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
