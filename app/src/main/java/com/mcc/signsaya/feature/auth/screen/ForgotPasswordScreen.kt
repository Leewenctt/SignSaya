package com.mcc.signsaya.feature.auth.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mcc.signsaya.R
import com.mcc.signsaya.ui.components.AuthField
import com.mcc.signsaya.ui.components.BackButton
import com.mcc.signsaya.ui.components.BannerHost
import com.mcc.signsaya.ui.components.GhostButton
import com.mcc.signsaya.ui.components.PrimaryButton
import com.mcc.signsaya.feature.auth.utils.maskEmail
import com.mcc.signsaya.feature.auth.viewmodel.ForgotPasswordUiState
import com.mcc.signsaya.feature.auth.viewmodel.ForgotPasswordViewModel

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
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

    val handleBack = remember(isNavigating, state.isSubmitting) {
        {
            if (!isNavigating && !state.isSubmitting) {
                isNavigating = true
                viewModel.dismissBanner()
                onBack()
            }
        }
    }

    BackHandler(enabled = !isNavigating && !state.isSubmitting) {
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
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BackButton(
                    onClick = handleBack,
                    enabled = !isNavigating && !state.isSubmitting,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(Modifier.height(32.dp))

                // Illustration
                Image(
                    painter = painterResource(id = R.drawable.vector_forgot_password),
                    contentDescription = null,
                    modifier = Modifier
                        .size(200.dp)
                        .padding(bottom = 16.dp)
                )

                FormContent(
                    state = state,
                    onEmailChange = viewModel::onEmailChange,
                    onSubmit = {
                        focusManager.clearFocus()
                        viewModel.submitReset()
                    },
                    onTryAnotherEmail = viewModel::tryAnotherEmail
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = "For security purposes, password reset links expire after 24 hours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
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

@Composable
private fun FormContent(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onTryAnotherEmail: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Forgot password?",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Don't worry! It happens. Enter your email below and we'll send you a link to reset your password.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        if (state.isEmailSent) {
            Text(
                text = buildAnnotatedString {
                    append("Password reset link sent to ")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                        append(state.email.maskEmail())
                    }
                    append(".")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
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
        }

        Spacer(Modifier.height(24.dp))

        PrimaryButton(
            text = when {
                state.resendCooldownSeconds > 0 -> "RESEND IN ${state.resendCooldownSeconds}s"
                state.isEmailSent -> "RESEND RESET LINK"
                else -> "SEND RESET LINK"
            },
            enabled = state.canSubmit,
            loading = state.isSubmitting,
            onClick = onSubmit
        )

        if (state.isEmailSent) {
            Spacer(Modifier.height(8.dp))
            GhostButton(
                text = "TRY ANOTHER EMAIL",
                onClick = onTryAnotherEmail,
                enabled = !state.isSubmitting
            )
        }
    }
}
