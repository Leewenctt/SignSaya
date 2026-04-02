package com.mcc.signsaya.feature.auth.screen

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mcc.signsaya.R
import com.mcc.signsaya.ui.components.AuthField
import com.mcc.signsaya.ui.components.BackButton
import com.mcc.signsaya.ui.components.BannerHost
import com.mcc.signsaya.ui.components.GhostButton
import com.mcc.signsaya.ui.components.GoogleButton
import com.mcc.signsaya.ui.components.PasswordField
import com.mcc.signsaya.ui.components.PrimaryButton
import com.mcc.signsaya.feature.auth.viewmodel.LoginEvent
import com.mcc.signsaya.feature.auth.viewmodel.LoginViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "LoginScreen"

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    viewModel: LoginViewModel
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var passwordVisible by remember { mutableStateOf(false) }
    var isNavigating by remember { mutableStateOf(false) }

    val handleBack = remember(isNavigating) {
        {
            if (!isNavigating) {
                isNavigating = true
                viewModel.dismissBanner()
                onBack()
            }
        }
    }

    BackHandler(enabled = !state.isSubmitting && !isNavigating) {
        handleBack()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is LoginEvent.LoginSuccess -> onLoginSuccess()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetState()
        }
    }

    val webClientId = stringResource(id = R.string.default_web_client_id)

    fun onGoogleSignInClick() {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        viewModel.onGoogleSignInStarted()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                viewModel.onGoogleSignIn(googleIdToken)
            } catch (_: GetCredentialCancellationException) {
                viewModel.onGoogleSignInError(null)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "GetCredentialException: ${e.type} - ${e.message}")
                viewModel.onGoogleSignInError("Google sign-in failed. Please try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected Google sign-in error: ${e.message}")
                viewModel.onGoogleSignInError("Google sign-in failed. Please try again.")
            }
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

                Spacer(Modifier.height(32.dp))

                Text(
                    text = "Hey! Welcome back.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Login with your account and pick up where you left off.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(28.dp))

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

                Spacer(Modifier.height(12.dp))

                PasswordField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Password",
                    placeholder = "••••••••",
                    error = state.passwordError,
                    hideError = state.isSubmitting,
                    enabled = !state.isSubmitting,
                    isPasswordVisible = passwordVisible,
                    onPasswordVisibleChange = { passwordVisible = it },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            passwordVisible = false
                            viewModel.submitLogin()
                        }
                    )
                )

                Spacer(Modifier.height(2.dp))

                GhostButton(
                    text = "FORGOT PASSWORD",
                    enabled = !state.isSubmitting,
                    onClick = {
                        viewModel.dismissBanner()
                        onNavigateToForgotPassword()
                    },
                    modifier = Modifier.align(Alignment.End)
                )

                Spacer(Modifier.height(12.dp))

                PrimaryButton(
                    text = "LOG IN",
                    enabled = state.isFormComplete && !state.isSubmitting,
                    loading = state.isSubmitting && !state.isGoogleSigningIn,
                    onClick = {
                        focusManager.clearFocus()
                        passwordVisible = false
                        viewModel.submitLogin()
                    }
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                    )
                    Text(
                        text = "OR",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                    )
                }

                Spacer(Modifier.height(20.dp))

                GoogleButton(
                    onClick = ::onGoogleSignInClick,
                    enabled = !state.isSubmitting,
                    loading = state.isGoogleSigningIn
                )

                Spacer(Modifier.weight(1f))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "Don't have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    GhostButton(
                        text = "SIGN UP",
                        enabled = !state.isSubmitting,
                        onClick = {
                            viewModel.dismissBanner()
                            onNavigateToSignUp()
                        }
                    )
                }
            }

            BannerHost(
                banner = state.banner,
                onDismiss = { viewModel.dismissBanner() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}