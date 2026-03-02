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

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private const val RESEND_COOLDOWN = 60
private const val POLL_INTERVAL_MS = 5_000L

// ---------------------------------------------------------------------------
// UI State
// ---------------------------------------------------------------------------

data class VerificationUiState(
    val isCheckingVerification: Boolean = false,
    val isResending: Boolean = false,
    val resendCooldownSeconds: Int = RESEND_COOLDOWN,

    // Banner for transient user-facing messages
    val bannerError: String? = null,

    // Distinct flags — screen can show different UI for each
    val networkError: Boolean = false,
    val sessionExpired: Boolean = false,

    // Consume with onNavigationHandled()
    val navigateToHome: Boolean = false,
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class EmailVerificationViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(VerificationUiState())
    val state: StateFlow<VerificationUiState> = _state.asStateFlow()

    private var cooldownJob: Job? = null
    private var pollJob: Job? = null

    // Guard so recompositions don't restart the timer and polling
    private var screenEntered = false

    // Call once from LaunchedEffect(Unit) in the screen
    fun onScreenEntered() {
        if (screenEntered) return
        screenEntered = true
        startCooldown()
        startPolling()
    }

    // -----------------------------------------------------------------------
    // Manual check — user taps "I've verified my email"
    // -----------------------------------------------------------------------

    fun checkVerification() {
        if (_state.value.isCheckingVerification) return

        viewModelScope.launch {
            _state.update {
                it.copy(isCheckingVerification = true, bannerError = null, networkError = false)
            }

            val result = repository.checkEmailVerified()

            // For manual checks, show feedback on NotYetVerified
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

    // -----------------------------------------------------------------------
    // Resend
    // -----------------------------------------------------------------------

    fun resendVerificationEmail() {
        if (_state.value.resendCooldownSeconds > 0 || _state.value.isResending) return

        viewModelScope.launch {
            _state.update { it.copy(isResending = true, bannerError = null) }

            when (val result = repository.resendVerificationEmail()) {
                is AuthResult.Success -> {
                    _state.update { it.copy(isResending = false) }
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

    // -----------------------------------------------------------------------
    // Events
    // -----------------------------------------------------------------------

    fun onNavigationHandled() {
        _state.update { it.copy(navigateToHome = false) }
    }

    fun dismissBannerError() {
        _state.update { it.copy(bannerError = null, networkError = false) }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    // Used by polling only — silent on NotYetVerified
    private fun handleVerificationResult(result: VerificationResult) {
        when (result) {
            is VerificationResult.Verified -> {
                pollJob?.cancel()
                _state.update { it.copy(navigateToHome = true) }
            }
            is VerificationResult.NotYetVerified -> {
                // Polling is silent — no banner spam every 5 seconds
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
            while (true) {
                delay(POLL_INTERVAL_MS)
                val current = _state.value
                if (current.navigateToHome || current.sessionExpired) break
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
