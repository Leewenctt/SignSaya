package com.mcc.signsaya.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcc.signsaya.repository.AuthRepository
import com.mcc.signsaya.repository.AuthResult
import com.mcc.signsaya.repository.VerificationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val RESEND_COOLDOWN = 60
private const val POLL_INTERVAL_MS = 5_000L
private const val POLL_MAX_DURATION_MS = 10 * 60 * 1000L // 10 minutes

data class VerificationUiState(
    val isCheckingVerification: Boolean = false,
    val isResending: Boolean = false,
    val resendCooldownSeconds: Int = 0, // Start at 0 so button is enabled immediately
    val bannerError: String? = null,
    val bannerSuccess: String? = null,
    val networkError: Boolean = false,
    val sessionExpired: Boolean = false,
    val pollingTimedOut: Boolean = false,
    val navigateToHome: Boolean = false,
)

class EmailVerificationViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(VerificationUiState())
    val state: StateFlow<VerificationUiState> = _state.asStateFlow()

    private var cooldownJob: Job? = null
    private var pollJob: Job? = null
    private var screenEntered = false // Guard to prevent recomposition restarts

    fun onScreenEntered() {
        if (screenEntered) return
        screenEntered = true
        // Don't start cooldown initially - user can resend immediately
        startPolling()
    }

    fun checkVerification() {
        if (_state.value.isCheckingVerification) return

        viewModelScope.launch {
            _state.update {
                it.copy(isCheckingVerification = true, bannerError = null, networkError = false)
            }

            val result = repository.checkEmailVerified()

            if (result is VerificationResult.NotYetVerified) {
                _state.update {
                    it.copy(
                        isCheckingVerification = false,
                        bannerError = "Your email hasn't been verified yet. " +
                                "Check your inbox and click the link we sent you."
                    )
                }
                return@launch
            }

            handleVerificationResult(result)
            _state.update { it.copy(isCheckingVerification = false) }
        }
    }

    fun resendVerificationEmail() {
        if (_state.value.resendCooldownSeconds > 0 || _state.value.isResending) return

        viewModelScope.launch {
            _state.update { it.copy(isResending = true, bannerError = null) }

            when (val result = repository.resendVerificationEmail()) {
                is AuthResult.Success -> {
                    _state.update { it.copy(isResending = false, bannerSuccess = "Verification link resent successfully. Please check your email.") }
                    startCooldown()
                }
                is AuthResult.Error.TooManyRequests -> {
                    _state.update { it.copy(isResending = false, bannerError = result.message) }
                    startCooldown()
                }
                is AuthResult.Error -> {
                    _state.update { it.copy(isResending = false, bannerError = result.message) }
                }
            }
        }
    }

    fun onNavigationHandled() {
        _state.update { it.copy(navigateToHome = false) }
    }

    fun dismissBannerError() {
        _state.update { it.copy(bannerError = null, networkError = false) }
    }

    fun dismissBannerSuccess() {
        _state.update { it.copy(bannerSuccess = null) }
    }

    private fun handleVerificationResult(result: VerificationResult) {
        when (result) {
            is VerificationResult.Verified -> {
                pollJob?.cancel()
                _state.update { it.copy(navigateToHome = true) }
            }
            is VerificationResult.NotYetVerified -> {
                // Silent — no banner spam during polling
            }
            is VerificationResult.NetworkError -> {
                _state.update { it.copy(networkError = true) }
            }
            is VerificationResult.SessionExpired -> {
                pollJob?.cancel()
                _state.update { it.copy(sessionExpired = true) }
            }
            is VerificationResult.Unexpected -> {
                _state.update { it.copy(bannerError = result.message) }
            }
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (secondsLeft in RESEND_COOLDOWN downTo 0) {
                _state.update { it.copy(resendCooldownSeconds = secondsLeft) }
                if (secondsLeft > 0) delay(1000L)
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            while (true) {
                delay(POLL_INTERVAL_MS)

                val current = _state.value
                if (current.navigateToHome || current.sessionExpired) break

                if (System.currentTimeMillis() - startTime >= POLL_MAX_DURATION_MS) {
                    _state.update { it.copy(pollingTimedOut = true) }
                    break
                }

                handleVerificationResult(repository.checkEmailVerified())
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
        pollJob?.cancel()
    }
}
