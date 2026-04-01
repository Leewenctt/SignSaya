package com.mcc.signsaya.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcc.signsaya.components.Banner
import com.mcc.signsaya.components.BannerType
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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val banner: Banner? = null,
    val isSubmitting: Boolean = false,
    val isGoogleSigningIn: Boolean = false
) {
    val isFormComplete: Boolean get() = email.isNotBlank() && password.isNotBlank()
}

sealed class LoginEvent {
    object LoginSuccess : LoginEvent()
}

class LoginViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    private var bannerJob: Job? = null

    fun onEmailChange(value: String) {
        _state.update {
            it.copy(
                email = value,
                emailError = null,
                passwordError = null,
                banner = null
            )
        }
        bannerJob?.cancel()
    }

    fun onPasswordChange(value: String) {
        _state.update {
            it.copy(
                password = value,
                emailError = null,
                passwordError = null,
                banner = null
            )
        }
        bannerJob?.cancel()
    }

    fun submitLogin() {
        if (_state.value.isSubmitting) return

        val state = _state.value
        val emailError = validateEmail(state.email)
        val passwordError = if (state.password.isBlank()) "Password is required." else null

        if (emailError != null || passwordError != null) {
            _state.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, isGoogleSigningIn = false, banner = null) }
            bannerJob?.cancel()

            when (val result = repository.loginWithEmail(state.email, state.password)) {
                is AuthResult.Success -> {
                    _state.update { it.copy(isSubmitting = false) }
                    _events.emit(LoginEvent.LoginSuccess)
                }
                is AuthResult.Error.InvalidInput -> {
                    // Highlight both fields in red (no message) and show a banner
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            emailError = "",
                            passwordError = "",
                            banner = Banner("Incorrect email or password.", BannerType.ERROR)
                        )
                    }
                    startBannerTimer()
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
                is AuthResult.Error.Conflict -> {
                    _state.update {
                        it.copy(isSubmitting = false, banner = Banner(result.message, BannerType.ERROR))
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
                    _events.emit(LoginEvent.LoginSuccess)
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

    fun resetState() {
        _state.update { LoginUiState() }
        bannerJob?.cancel()
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
}