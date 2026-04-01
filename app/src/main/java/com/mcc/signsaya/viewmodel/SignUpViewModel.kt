package com.mcc.signsaya.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcc.signsaya.components.Banner
import com.mcc.signsaya.components.BannerType
import com.mcc.signsaya.repository.AuthErrorCause
import com.mcc.signsaya.repository.AuthRepository
import com.mcc.signsaya.repository.AuthResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val banner: Banner? = null,
    val isSubmitting: Boolean = false,
    val isGoogleSigningIn: Boolean = false
) {
    val isFormComplete: Boolean get() = email.isNotBlank() &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank()
}

sealed class SignUpEvent {
    data class NavigateToVerification(val email: String) : SignUpEvent()
    object SignUpSuccess : SignUpEvent()
}

class SignUpViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SignUpEvent>()
    val events: SharedFlow<SignUpEvent> = _events.asSharedFlow()

    private var bannerJob: Job? = null

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, emailError = null, banner = null) }
        bannerJob?.cancel()
    }

    fun onPasswordChange(value: String) {
        _state.update {
            it.copy(
                password = value,
                passwordError = null,
                confirmPasswordError = if (it.confirmPasswordError == "Passwords must match.") null else it.confirmPasswordError,
                banner = null
            )
        }
        bannerJob?.cancel()
    }

    fun onConfirmPasswordChange(value: String) {
        _state.update { it.copy(confirmPassword = value, confirmPasswordError = null, banner = null) }
        bannerJob?.cancel()
    }

    fun submitSignUp() {
        if (_state.value.isSubmitting) return

        val state = _state.value
        val emailError = validateEmail(state.email)
        val passwordError = validatePassword(state.password)
        val confirmPasswordError = validateConfirmPassword(state.password, state.confirmPassword)

        if (emailError != null || passwordError != null || confirmPasswordError != null) {
            _state.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, isGoogleSigningIn = false, banner = null) }
            bannerJob?.cancel()

            when (val result = repository.signUpWithEmail(state.email, state.password)) {
                is AuthResult.Success -> {
                    _state.update { it.copy(isSubmitting = false) }
                    _events.emit(SignUpEvent.NavigateToVerification(state.email))
                }
                is AuthResult.Error.InvalidInput -> {
                    val banner = if (result.cause == AuthErrorCause.UNKNOWN) Banner(result.message, BannerType.ERROR) else null
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            emailError = if (result.cause == AuthErrorCause.EMAIL) result.message else null,
                            passwordError = if (result.cause == AuthErrorCause.PASSWORD) result.message else null,
                            banner = banner
                        )
                    }
                    if (banner != null) startBannerTimer()
                }
                is AuthResult.Error.Conflict -> {
                    _state.update {
                        it.copy(isSubmitting = false, emailError = result.message)
                    }
                }
                is AuthResult.Error.Network,
                is AuthResult.Error.TooManyRequests,
                is AuthResult.Error.Unexpected -> {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            banner = Banner(result.message, BannerType.ERROR)
                        )
                    }
                    startBannerTimer()
                }
            }
        }
    }

    fun onGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, isGoogleSigningIn = true, banner = null) }
            bannerJob?.cancel()

            when (val result = repository.loginWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    _state.update { it.copy(isSubmitting = false, isGoogleSigningIn = false) }
                    _events.emit(SignUpEvent.SignUpSuccess)
                }
                is AuthResult.Error -> {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            isGoogleSigningIn = false,
                            banner = Banner(result.message, BannerType.ERROR)
                        )
                    }
                    startBannerTimer()
                }
            }
        }
    }

    // Called by the screen when the Credential Manager throws before reaching the ViewModel
    fun onGoogleSignInError(message: String) {
        _state.update {
            it.copy(
                isSubmitting = false,
                isGoogleSigningIn = false,
                banner = Banner(message, BannerType.ERROR)
            )
        }
        startBannerTimer()
    }

    fun dismissBanner() {
        bannerJob?.cancel()
        _state.update { it.copy(banner = null) }
    }

    private fun startBannerTimer() {
        bannerJob?.cancel()
        bannerJob = viewModelScope.launch {
            delay(3000)
            _state.update { it.copy(banner = null) }
        }
    }

    private fun validateEmail(email: String): String? = when {
        email.isBlank() -> "Email address is required."
        !email.matches(EMAIL_REGEX) -> "Email address is invalid."
        else -> null
    }

    private fun validatePassword(password: String): String? = when {
        password.isBlank() -> "Password is required."
        password.contains(" ") -> "Password cannot contain spaces."
        password.length < 8 -> "Password must be at least 8 characters long."
        !password.any { it.isLetter() } || !password.any { it.isDigit() } -> "Password must include a letter and a number."
        else -> null
    }

    private fun validateConfirmPassword(password: String, confirm: String): String? = when {
        confirm.isBlank() -> "Please confirm your password."
        confirm != password -> "Passwords must match."
        else -> null
    }
}