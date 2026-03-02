package com.mcc.signsaya.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcc.signsaya.repository.AuthErrorCause
import com.mcc.signsaya.repository.AuthRepository
import com.mcc.signsaya.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// UI State
// ---------------------------------------------------------------------------

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",

    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,

    // Banner for non-field errors (network, rate-limit, unexpected)
    val bannerError: String? = null,

    val isSubmitting: Boolean = false,

    // Non-null triggers navigation; always consume with onNavigationHandled()
    val navigateToVerification: String? = null,
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class SignUpViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    // -----------------------------------------------------------------------
    // Field updates
    // -----------------------------------------------------------------------

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, emailError = null, bannerError = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update {
            it.copy(
                password = value,
                passwordError = null,
                // Keep confirm error live-validated once it's been shown
                confirmPasswordError = if (it.confirmPasswordError != null && it.confirmPassword.isNotBlank())
                    validateConfirmPassword(value, it.confirmPassword)
                else it.confirmPasswordError,
                bannerError = null
            )
        }
    }

    fun onConfirmPasswordChange(value: String) {
        _state.update {
            it.copy(
                confirmPassword = value,
                confirmPasswordError = if (it.confirmPasswordError != null)
                    validateConfirmPassword(it.password, value)
                else null,
                bannerError = null
            )
        }
    }

    // -----------------------------------------------------------------------
    // Email + password sign up
    // -----------------------------------------------------------------------

    fun submitSignUp() {
        // Guard first — snapshot only after we know we're proceeding
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
            _state.update { it.copy(isSubmitting = true, bannerError = null) }

            when (val result = repository.signUpWithEmail(state.email, state.password)) {
                is AuthResult.Success -> {
                    _state.update {
                        it.copy(isSubmitting = false, navigateToVerification = state.email)
                    }
                }
                is AuthResult.Error.InvalidInput -> {
                    // Repository returns typed exceptions so we know exactly which field failed
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            emailError = if (result.cause == AuthErrorCause.EMAIL) result.message else null,
                            passwordError = if (result.cause == AuthErrorCause.PASSWORD) result.message else null
                        )
                    }
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
                            bannerError = result.message
                        )
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Events
    // -----------------------------------------------------------------------

    fun onNavigationHandled() {
        _state.update { it.copy(navigateToVerification = null) }
    }

    fun dismissBannerError() {
        _state.update { it.copy(bannerError = null) }
    }
}

// ---------------------------------------------------------------------------
// Validation
// ---------------------------------------------------------------------------

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

private fun validateEmail(email: String): String? = when {
    email.isBlank() -> "Email is required."
    !email.matches(EMAIL_REGEX) -> "That doesn't look like a valid email address."
    else -> null
}

private fun validatePassword(password: String): String? = when {
    password.isBlank() -> "Password is required."
    password.length < 8 -> "Password must be at least 8 characters long."
    !password.any { it.isUpperCase() } -> "Include at least one uppercase letter."
    !password.any { it.isDigit() } -> "Include at least one number."
    else -> null
}

private fun validateConfirmPassword(password: String, confirm: String): String? = when {
    confirm.isBlank() -> "Please confirm your password."
    confirm != password -> "Passwords do not match."
    else -> null
}
