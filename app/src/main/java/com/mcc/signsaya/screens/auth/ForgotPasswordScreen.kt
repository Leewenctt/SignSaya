package com.mcc.signsaya.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mcc.signsaya.R
import com.mcc.signsaya.components.*
import com.mcc.signsaya.utils.maskEmail
import com.mcc.signsaya.viewmodel.ForgotPasswordUiState
import com.mcc.signsaya.viewmodel.ForgotPasswordViewModel

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    initialEmail: String = "",
    viewModel: ForgotPasswordViewModel
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    var isNavigating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (initialEmail.isNotBlank() && state.email.isBlank()) {
            viewModel.onEmailChange(initialEmail)
        }
    }

    val handleBack = remember(isNavigating, state.isEmailSent) {
        {
            if (!isNavigating) {
                if (state.isEmailSent) {
                    viewModel.resetToForm()
                } else {
                    isNavigating = true
                    viewModel.dismissBanner()
                    onBack()
                }
            }
        }
    }

    BackHandler(enabled = !isNavigating) {
        handleBack()
    }

    DisposableEffect(Unit) {
        onDispose {
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

                if (state.isEmailSent) {
                    SuccessContent(
                        email = state.email,
                        resendCooldownSeconds = state.resendCooldownSeconds,
                        isSubmitting = state.isSubmitting,
                        canResend = state.canResend,
                        onNavigateToLogin = onNavigateToLogin,
                        onResend = { viewModel.resend() }
                    )
                } else {
                    FormContent(
                        state = state,
                        onEmailChange = viewModel::onEmailChange,
                        onSubmit = {
                            focusManager.clearFocus()
                            viewModel.submitReset()
                        }
                    )
                }

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

@Composable
private fun FormContent(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Forgot password?",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "No worries! Enter your email below and we'll send you a link to reset your password.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))

        AuthField(
            value = state.email,
            onValueChange = onEmailChange,
            label = "Email",
            placeholder = "example@email.com",
            error = state.emailError,
            hideError = state.isSubmitting,
            enabled = !state.isSubmitting,
            iconRes = R.drawable.ic_mail,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() }
            )
        )

        Spacer(Modifier.height(28.dp))

        PrimaryButton(
            text = if (state.resendCooldownSeconds > 0) "SEND IN ${state.resendCooldownSeconds}s" else "SEND RESET LINK",
            enabled = state.canSubmit,
            loading = state.isSubmitting,
            onClick = onSubmit
        )
    }
}

@Composable
private fun SuccessContent(
    email: String,
    resendCooldownSeconds: Int,
    isSubmitting: Boolean,
    canResend: Boolean,
    onNavigateToLogin: () -> Unit,
    onResend: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Password reset link sent!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = buildAnnotatedString {
                append("We sent an email with a link to reset your password to: ")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append(email.maskEmail())
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "The link will remain valid for 24 hours. Please check your inbox and spam folder.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))

        PrimaryButton(
            text = "RETURN TO LOGIN",
            onClick = onNavigateToLogin
        )

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Didn't receive it?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            GhostButton(
                text = if (resendCooldownSeconds > 0) "RESEND IN ${resendCooldownSeconds}s" else "RESEND",
                enabled = canResend && !isSubmitting,
                onClick = onResend
            )
        }
    }
}