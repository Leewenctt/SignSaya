package com.mcc.signsaya.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcc.signsaya.ui.components.Banner
import com.mcc.signsaya.ui.components.BannerType
import com.mcc.signsaya.feature.auth.repository.AuthRepository
import com.mcc.signsaya.feature.auth.repository.AuthResult
import com.mcc.signsaya.feature.auth.repository.VerificationResult
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
    val resendCooldownSeconds: Int = 0,
    val banner: Banner? = null,
    val networkError: Boolean = false,
    val sessionExpired: Boolean = false,
    val pollingTimedOut: Boolean = false,
    val navigateToHome: Boolean = false,
    val isInitialEmailSent: Boolean = false,
    val isVerified: Boolean = false
)

class EmailVerificationViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VerificationUiState())
    val state: StateFlow<VerificationUiState> = _state.asStateFlow()

    private var cooldownJob: Job? = null
    private var pollJob: Job? = null
    
    @Volatile
    private var screenEntered = false

    fun onScreenEntered() {
        if (screenEntered) return
        screenEntered = true
        
        // Restore state from repository
        val initialSent = repository.isInitialEmailSent()
        if (initialSent) {
            _state.update { it.copy(isInitialEmailSent = true) }
            startPolling()
        } else {
            // Check once even if not sent, in case they verified via another device/session
            // but we don't start continuous polling yet to save resources
            viewModelScope.launch {
                handleVerificationResult(repository.checkEmailVerified())
            }
        }
    }


    fun sendInitialVerificationEmail() {
        if (_state.value.isResending) return
        
        viewModelScope.launch {
            _state.update { it.copy(isResending = true, banner = null) }

            when (val result = repository.resendVerificationEmail()) {
                is AuthResult.Success -> {
                    repository.setInitialEmailSent(true)
                    _state.update { 
                        it.copy(
                            isResending = false,
                            isInitialEmailSent = true,
                            networkError = false,
                            banner = Banner(
                                message = "Verification link sent. Please check your email.",
                                type = BannerType.SUCCESS
                            )
                        ) 
                    }
                    startCooldown()
                    startPolling()
                }
                is AuthResult.Error -> {
                    _state.update { 
                        it.copy(
                            isResending = false, 
                            networkError = result is AuthResult.Error.Network,
                            banner = Banner(result.message, BannerType.ERROR)
                        ) 
                    }
                }
            }
        }
    }

    fun resendVerificationEmail() {
        if (_state.value.resendCooldownSeconds > 0 || _state.value.isResending) return

        viewModelScope.launch {
            _state.update { it.copy(isResending = true, banner = null) }

            when (val result = repository.resendVerificationEmail()) {
                is AuthResult.Success -> {
                    _state.update { 
                        it.copy(
                            isResending = false, 
                            networkError = false,
                            banner = Banner(
                                message = "Verification link sent. Please check your email.",
                                type = BannerType.SUCCESS
                            )
                        ) 
                    }
                    startCooldown()
                    startPolling()
                }
                is AuthResult.Error.TooManyRequests -> {
                    _state.update { 
                        it.copy(
                            isResending = false, 
                            networkError = false,
                            banner = Banner(result.message, BannerType.ERROR)
                        ) 
                    }
                    startCooldown()
                }
                is AuthResult.Error -> {
                    _state.update { 
                        it.copy(
                            isResending = false, 
                            networkError = result is AuthResult.Error.Network,
                            banner = Banner(result.message, BannerType.ERROR)
                        ) 
                    }
                }
            }
        }
    }

    fun onDismissVerification() {
        repository.markVerificationDismissed()
        _state.update { it.copy(navigateToHome = true) }
    }

    fun onNavigationHandled() {
        _state.update { it.copy(navigateToHome = false) }
    }

    fun dismissBanner() {
        _state.update { it.copy(banner = null) }
    }

    private fun handleVerificationResult(result: VerificationResult) {
        when (result) {
            is VerificationResult.Verified -> {
                pollJob?.cancel()
                viewModelScope.launch {
                    _state.update { it.copy(isVerified = true, networkError = false) }
                    delay(2000)
                    _state.update { it.copy(navigateToHome = true) }
                }
            }
            is VerificationResult.NotYetVerified -> {
                _state.update { it.copy(networkError = false) }
            }
            is VerificationResult.NetworkError -> {
                _state.update { it.copy(networkError = true) }
            }
            is VerificationResult.SessionExpired -> {
                pollJob?.cancel()
                _state.update { it.copy(sessionExpired = true, pollingTimedOut = false) }
            }
            is VerificationResult.Unexpected -> {
                _state.update { 
                    it.copy(
                        banner = Banner(message = result.message, type = BannerType.ERROR)
                    ) 
                }
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
            // Reset error states immediately when starting/restarting polling
            _state.update { it.copy(pollingTimedOut = false, networkError = false) }
            
            val startTime = System.currentTimeMillis()

            while (true) {
                val current = _state.value
                if (current.navigateToHome || current.sessionExpired || current.isVerified) break

                if (System.currentTimeMillis() - startTime >= POLL_MAX_DURATION_MS) {
                    _state.update { it.copy(pollingTimedOut = true) }
                    break
                }

                try {
                    handleVerificationResult(repository.checkEmailVerified())
                } catch (_: Exception) {
                    _state.update { 
                        it.copy(
                            banner = Banner(
                                message = "We couldn't check your verification status. Please try again.",
                                type = BannerType.ERROR
                            )
                        ) 
                    }
                }

                // Delay at the end of the loop so the first check is immediate
                delay(POLL_INTERVAL_MS)
            }
        }
    }
}
