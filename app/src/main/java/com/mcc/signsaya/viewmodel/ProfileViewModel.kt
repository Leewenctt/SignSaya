package com.mcc.signsaya.viewmodel

import androidx.lifecycle.ViewModel
import com.mcc.signsaya.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ---------------------------------------------------------------------------
// UI State
// ---------------------------------------------------------------------------

data class ProfileUiState(
    val userEmail: String? = null,
    val isLoading: Boolean = false
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class ProfileViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        val user = repository.currentUser
        _state.update { it.copy(userEmail = user?.email) }
    }

    fun logout() {
        repository.signOut()
        _state.update { it.copy(userEmail = null) }
    }
}
