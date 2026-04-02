package com.mcc.signsaya.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mcc.signsaya.feature.auth.repository.AuthRepository
import com.mcc.signsaya.feature.auth.viewmodel.EmailVerificationViewModel
import com.mcc.signsaya.feature.auth.viewmodel.ForgotPasswordViewModel
import com.mcc.signsaya.feature.auth.viewmodel.LoginViewModel
import com.mcc.signsaya.feature.auth.viewmodel.SignUpViewModel
import com.mcc.signsaya.feature.profile.viewmodel.ProfileViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SignUpViewModel::class.java) -> {
                SignUpViewModel(repository) as T
            }
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java) -> {
                ForgotPasswordViewModel(repository) as T
            }
            modelClass.isAssignableFrom(EmailVerificationViewModel::class.java) -> {
                EmailVerificationViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}