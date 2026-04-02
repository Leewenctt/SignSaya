package com.mcc.signsaya

import android.app.Application
import com.mcc.signsaya.feature.auth.repository.AuthRepository
import com.mcc.signsaya.di.ViewModelFactory

class SignSayaApplication : Application() {

    lateinit var authRepository: AuthRepository
    lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate() {
        super.onCreate()
        
        authRepository = AuthRepository(this)
        viewModelFactory = ViewModelFactory(authRepository)
    }
}
