package com.mcc.signsaya.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.signsaya.BuildConfig
import com.mcc.signsaya.R
import com.mcc.signsaya.components.AuthField
import com.mcc.signsaya.components.BackButton
import com.mcc.signsaya.components.GhostButton
import com.mcc.signsaya.components.PasswordField
import com.mcc.signsaya.components.PrimaryButton
import com.mcc.signsaya.viewmodel.SignUpViewModel
import kotlinx.coroutines.delay

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignUpSuccess: (email: String) -> Unit,
    onNavigateToLogin: () -> Unit = onBack,
    viewModel: SignUpViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var isNavigating by remember { mutableStateOf(false) }

    val handleBack = {
        if (!isNavigating) {
            isNavigating = true
            viewModel.dismissBannerError()
            onBack()
        }
    }

    // Clear banner when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.dismissBannerError()
        }
    }

    LaunchedEffect(state.navigateToVerification) {
        state.navigateToVerification?.let { email ->
            viewModel.onNavigationHandled()
            onSignUpSuccess(email)
        }
    }

    // Auto-dismiss banner after 3 seconds
    LaunchedEffect(state.bannerError) {
        if (state.bannerError != null) {
            delay(3000)
            viewModel.dismissBannerError()
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
                    enabled = !state.isSubmitting && !isNavigating,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "Hey! Let's get you in.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Fill in your details and we'll handle the rest.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(32.dp))

                AuthField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Email",
                    placeholder = "example@email.com",
                    error = state.emailError,
                    hideError = state.isSubmitting,
                    enabled = !state.isSubmitting,
                    iconRes = R.drawable.ic_mail,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(Modifier.height(16.dp))

                PasswordField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Password",
                    placeholder = "••••••••",
                    error = state.passwordError,
                    hideError = state.isSubmitting,
                    enabled = !state.isSubmitting,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(Modifier.height(16.dp))

                PasswordField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = "Confirm Password",
                    placeholder = "••••••••",
                    error = state.confirmPasswordError,
                    hideError = state.isSubmitting,
                    enabled = !state.isSubmitting,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.submitSignUp()
                        }
                    )
                )

                Spacer(Modifier.height(28.dp))

                PrimaryButton(
                    text = "Create Account",
                    enabled = state.email.isNotBlank() && state.password.isNotBlank() && state.confirmPassword.isNotBlank() && !state.isSubmitting,
                    loading = state.isSubmitting,
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.submitSignUp()
                    }
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GhostButton(
                        text = "Log In",
                        onClick = {
                            viewModel.dismissBannerError()
                            onNavigateToLogin()
                        }
                    )
                }

                // Debug-only: Skip to verification screen for testing
                if (BuildConfig.DEBUG) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        GhostButton(
                            text = "Skip to Verification Screen",
                            onClick = {
                                viewModel.dismissBannerError()
                                onSignUpSuccess("test@example.com")
                            }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "By continuing, you agree to our",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GhostButton(
                            text = "Terms of Service",
                            onClick = { }
                        )
                        Text(
                            text = "and",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        GhostButton(
                            text = "Privacy Policy",
                            onClick = { }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
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
        }
    }
}
