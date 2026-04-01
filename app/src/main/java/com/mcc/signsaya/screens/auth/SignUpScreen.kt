package com.mcc.signsaya.screens.auth

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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mcc.signsaya.R
import com.mcc.signsaya.components.*
import com.mcc.signsaya.viewmodel.SignUpEvent
import com.mcc.signsaya.viewmodel.SignUpViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "SignUpScreen"

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignUpSuccess: (email: String) -> Unit,
    onNavigateToLogin: () -> Unit = onBack,
    onGoogleSignUpSuccess: () -> Unit,
    viewModel: SignUpViewModel
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
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

    // Handle one-shot events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SignUpEvent.NavigateToVerification -> onSignUpSuccess(event.email)
                is SignUpEvent.SignUpSuccess -> onGoogleSignUpSuccess()
            }
        }
    }

    // Clear banner when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.dismissBanner()
        }
    }

    val webClientId = stringResource(id = R.string.default_web_client_id)

    fun onGoogleSignInClick() {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                viewModel.onGoogleSignIn(googleIdToken)
            } catch (e: GetCredentialCancellationException) {
                // User dismissed the picker — no action needed
                Log.d(TAG, "Google sign-in cancelled by user")
            } catch (e: GetCredentialException) {
                // Credential API error (e.g. no accounts, misconfigured client ID, missing SHA-1)
                Log.e(TAG, "GetCredentialException: ${e.type} — ${e.message}", e)
                viewModel.onGoogleSignInError("Google sign-in failed. Please try again.")
            } catch (e: Exception) {
                // Unexpected error (e.g. token parsing failure)
                Log.e(TAG, "Unexpected Google sign-in error: ${e.message}", e)
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

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "Great! Let's get you in",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Sign up below and start learning with SignSaya today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
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
                    isPasswordVisible = passwordVisible,
                    onPasswordVisibleChange = { passwordVisible = it },
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
                    isPasswordVisible = confirmPasswordVisible,
                    onPasswordVisibleChange = { confirmPasswordVisible = it },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            passwordVisible = false
                            confirmPasswordVisible = false
                            viewModel.submitSignUp()
                        }
                    )
                )

                Spacer(Modifier.height(28.dp))

                PrimaryButton(
                    text = "CREATE ACCOUNT",
                    enabled = state.isFormComplete && !state.isSubmitting,
                    loading = state.isSubmitting && !state.isGoogleSigningIn,
                    onClick = {
                        focusManager.clearFocus()
                        passwordVisible = false
                        confirmPasswordVisible = false
                        viewModel.submitSignUp()
                    }
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "OR",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                Spacer(Modifier.height(24.dp))

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
                        text = "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GhostButton(
                        text = "LOG IN",
                        onClick = {
                            viewModel.dismissBanner()
                            onNavigateToLogin()
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