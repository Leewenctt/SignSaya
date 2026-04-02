package com.mcc.signsaya.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcc.signsaya.ui.components.Banner
import com.mcc.signsaya.ui.components.BannerType
import com.mcc.signsaya.feature.auth.repository.AuthRepository
import com.mcc.signsaya.feature.auth.repository.AuthResult
import com.mcc.signsaya.feature.auth.utils.EMAIL_REGEX
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val banner: Banner? = null,
    val isSubmitting: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val isEmailSent: Boolean = false
) {
    val canSubmit: Boolean get() = email.isNotBlank() && !isSubmitting && resendCooldownSeconds == 0
    val canResend: Boolean get() = !isSubmitting && resendCooldownSeconds == 0
}

class ForgotPasswordViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    private var bannerJob: Job? = null
    private var timerJob: Job? = null

    fun onEmailChange(value: String) {
        _state.update {
            it.copy(
                email = value,
                emailError = null,
                banner = null
            )
        }
        bannerJob?.cancel()
    }

    fun submitReset() {
        if (!_state.value.canSubmit) return

        val email = _state.value.email
        val emailError = validateEmail(email)

        if (emailError != null) {
            _state.update { it.copy(emailError = emailError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, banner = null) }
            bannerJob?.cancel()

            when (val result = repository.sendPasswordResetEmail(email)) {
                is AuthResult.Success -> {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            isEmailSent = true,
                            resendCooldownSeconds = 60,
                            banner = Banner("Reset link sent. Please check your email.", BannerType.SUCCESS)
                        )
                    }
                    startResendTimer()
                    startBannerTimer()
                }
                is AuthResult.Error -> {
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

    fun resend() {
        if (!_state.value.canResend) return

        val email = _state.value.email

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, banner = null) }
            bannerJob?.cancel()

            when (val result = repository.sendPasswordResetEmail(email)) {
                is AuthResult.Success -> {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            resendCooldownSeconds = 60,
                            banner = Banner("Reset link sent. Please check your email.", BannerType.SUCCESS)
                        )
                    }
                    startResendTimer()
                    startBannerTimer()
                }
                is AuthResult.Error -> {
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

    private fun startResendTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.resendCooldownSeconds > 0) {
                delay(1000)
                _state.update { it.copy(resendCooldownSeconds = it.resendCooldownSeconds - 1) }
            }
        }
    }

    fun dismissBanner() {
        bannerJob?.cancel()
        _state.update { it.copy(banner = null) }
    }

    private fun startBannerTimer() {
        bannerJob?.cancel()
        bannerJob = viewModelScope.launch {
            delay(5000)
            _state.update { it.copy(banner = null) }
        }
    }

    private fun validateEmail(email: String): String? = when {
        email.isBlank() -> "Email address is required."
        !email.matches(EMAIL_REGEX) -> "Email address is invalid."
        else -> null
    }

    fun resetToForm() {
        _state.update { it.copy(isEmailSent = false, banner = null) }
        bannerJob?.cancel()
    }

    fun tryAnotherEmail() {
        timerJob?.cancel()
        _state.update {
            it.copy(
                email = "",
                emailError = null,
                isEmailSent = false,
                resendCooldownSeconds = 0,
                banner = null
            )
        }
        bannerJob?.cancel()
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        bannerJob?.cancel()
    }
}
