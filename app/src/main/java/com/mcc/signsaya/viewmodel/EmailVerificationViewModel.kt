package com.mcc.signsaya.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcc.signsaya.components.Banner
import com.mcc.signsaya.components.BannerType
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
    val resendCooldownSeconds: Int = 0,
    val banner: Banner? = null,
    val networkError: Boolean = false,
    val sessionExpired: Boolean = false,
    val pollingTimedOut: Boolean = false,
    val navigateToHome: Boolean = false,
    val isInitialEmailSent: Boolean = false
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
        // No longer starting polling automatically until email is sent
    }


    fun sendInitialVerificationEmail() {
        if (_state.value.isResending) return
        
        viewModelScope.launch {
            _state.update { it.copy(isResending = true, banner = null) }

            when (val result = repository.resendVerificationEmail()) {
                is AuthResult.Success -> {
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

    fun onNavigationHandled() {
        _state.update { it.copy(navigateToHome = false) }
    }

    fun dismissBanner() {
        _state.update { it.copy(banner = null) }
    }

    /*fun showTestSuccess() {
        _state.update { 
            it.copy(
                banner = Banner(message = "Test: Success banner triggered!", type = BannerType.SUCCESS)
            ) 
        }
    }

    fun showTestError() {
        _state.update { 
            it.copy(
                banner = Banner(message = "Test: Error banner triggered!", type = BannerType.ERROR)
            ) 
        }
    }*/

    private fun handleVerificationResult(result: VerificationResult) {
        when (result) {
            is VerificationResult.Verified -> {
                pollJob?.cancel()
                _state.update { it.copy(navigateToHome = true, networkError = false) }
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
                if (current.navigateToHome || current.sessionExpired) break

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
