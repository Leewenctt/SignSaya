package com.mcc.signsaya.screens.auth

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mcc.signsaya.R
import com.mcc.signsaya.components.BackButton
import com.mcc.signsaya.components.BannerHost
import com.mcc.signsaya.components.GhostButton
import com.mcc.signsaya.components.PrimaryButton
import com.mcc.signsaya.utils.maskEmail
import com.mcc.signsaya.viewmodel.EmailVerificationViewModel
import kotlinx.coroutines.delay

@Composable
fun EmailVerificationScreen(
    email: String,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: EmailVerificationViewModel
) {
    val decodedEmail = remember(email) { Uri.decode(email) }
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    var isNavigating by remember { mutableStateOf(false) }

    val handleBack = {
        if (!isNavigating) {
            isNavigating = true
            viewModel.dismissBanner()
            onBack()
        }
    }

    // Intercept back gesture
    BackHandler(enabled = !isNavigating) {
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
                    enabled = !isNavigating,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "Almost there!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = buildAnnotatedString {
                        append("We'll need to verify your email: ")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                            append(decodedEmail.maskEmail())
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Click the link in your email to finish your account setup. The link will remain valid for 72 hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(28.dp))

                // Status Area (Replaces Popups)
                AnimatedVisibility(
                    visible = state.isInitialEmailSent || state.sessionExpired || state.networkError,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                state.sessionExpired -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_user_forbid),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = " Session expired. Email verification failed.",
                                            fontWeight = FontWeight.Bold,
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
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = "Network error. No internet connection.",
                                            fontWeight = FontWeight.Bold,
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
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2F),
                                            strokeCap = StrokeCap.Round,
                                        )

                                        Spacer(Modifier.height(12.dp))

                                        Text(
                                            text = "Waiting for verification…",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(32.dp))
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
                            loading = state.isResending,
                            onClick = { viewModel.sendInitialVerificationEmail() }
                        )
                    } else {
                        PrimaryButton(
                            text = if (state.resendCooldownSeconds > 0) {
                                "RESEND IN ${state.resendCooldownSeconds}s"
                            } else {
                                "RESEND VERIFICATION LINK"
                            },
                            enabled = state.resendCooldownSeconds == 0 && !state.isResending,
                            loading = state.isResending,
                            onClick = { viewModel.resendVerificationEmail() }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                GhostButton(
                    text = "NOT RIGHT NOW",
                    onClick = { onVerified() }
                )

                /* if (BuildConfig.DEBUG) {
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = "SUCCESS",
                            onClick = { viewModel.showTestSuccess() },
                            modifier = Modifier.weight(1f)
                        )
                        GhostButton(
                            text = "ERROR",
                            onClick = { viewModel.showTestError() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } */

                Spacer(Modifier.height(40.dp))
            }

            BannerHost(
                banner = state.banner,
                onDismiss = { viewModel.dismissBanner() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
