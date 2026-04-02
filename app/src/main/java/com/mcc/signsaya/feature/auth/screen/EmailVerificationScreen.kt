package com.mcc.signsaya.feature.auth.screen

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mcc.signsaya.R
import com.mcc.signsaya.ui.components.BackButton
import com.mcc.signsaya.ui.components.BannerHost
import com.mcc.signsaya.ui.components.GhostButton
import com.mcc.signsaya.ui.components.PrimaryButton
import com.mcc.signsaya.feature.auth.utils.maskEmail
import com.mcc.signsaya.feature.auth.viewmodel.EmailVerificationViewModel
import kotlinx.coroutines.delay

@Composable
fun EmailVerificationScreen(
    email: String,
    onVerified: () -> Unit,
    viewModel: EmailVerificationViewModel
) {
    val decodedEmail = remember(email) { Uri.decode(email) }
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    var isNavigating by remember { mutableStateOf(false) }

    val isBusy = state.isResending || state.isVerified

    val handleBack = {
        if (!isNavigating && !isBusy) {
            isNavigating = true
            viewModel.dismissBanner()
            // Opt-out of verification if the user backs out
            viewModel.onDismissVerification()
        }
    }

    // Intercept back gesture
    BackHandler(enabled = !isNavigating && !isBusy) {
        handleBack()
    }

    // Clear banner when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.dismissBanner()
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

    // Auto-dismiss banner after 3000ms
    LaunchedEffect(state.banner) {
        if (state.banner != null) {
            delay(3000)
            viewModel.dismissBanner()
        }
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
                    enabled = !isNavigating && !isBusy,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(Modifier.height(32.dp))

                // Illustration (Placed back above the header)
                Image(
                    painter = painterResource(id = R.drawable.vector_email_verification),
                    contentDescription = null,
                    modifier = Modifier
                        .size(200.dp)
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = "You're almost there!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = buildAnnotatedString {
                        append("To finish setting up your account, you'll need to verify your email. Tap the button below to send a verification link to ")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                            append(decodedEmail.maskEmail())
                        }
                        append(".")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                // Status Area (Replaces Popups)
                AnimatedVisibility(
                    visible = state.isInitialEmailSent || state.sessionExpired || state.networkError || state.isVerified,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                state.isVerified -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_verified),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = Color(0xFF4CAF50) // Success Green
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        @Suppress("DEPRECATION")
                                        Text(
                                            text = "Email verified successfully.",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF4CAF50),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                state.sessionExpired -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_user_forbid),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        @Suppress("DEPRECATION")
                                        Text(
                                            text = " Session expired. Email verification failed.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                state.networkError -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_connection_lost),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        @Suppress("DEPRECATION")
                                        Text(
                                            text = "Network error. No internet connection.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                else -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            strokeWidth = 3.dp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2F),
                                            strokeCap = StrokeCap.Round,
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        @Suppress("DEPRECATION")
                                        Text(
                                            text = "Waiting for verification…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }

                // Actions Area
                if (state.sessionExpired) {
                    PrimaryButton(
                        text = "GO BACK",
                        onClick = handleBack
                    )
                } else {
                    if (!state.isInitialEmailSent) {
                        PrimaryButton(
                            text = "SEND VERIFICATION LINK",
                            loading = state.isResending || state.isVerified,
                            enabled = !state.isVerified,
                            onClick = { viewModel.sendInitialVerificationEmail() }
                        )
                    } else {
                        PrimaryButton(
                            text = if (state.resendCooldownSeconds > 0) {
                                "RESEND IN ${state.resendCooldownSeconds}s"
                            } else {
                                "RESEND VERIFICATION LINK"
                            },
                            enabled = state.resendCooldownSeconds == 0 && !state.isResending && !state.isVerified,
                            loading = state.isResending || state.isVerified,
                            onClick = { viewModel.resendVerificationEmail() }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                GhostButton(
                    text = "NOT RIGHT NOW",
                    enabled = !isBusy,
                    onClick = { viewModel.onDismissVerification() }
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = "For security purposes, verification links expire after 72 hours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            BannerHost(
                banner = state.banner,
                onDismiss = { viewModel.dismissBanner() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
