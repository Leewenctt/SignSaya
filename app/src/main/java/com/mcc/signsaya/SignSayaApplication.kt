package com.mcc.signsaya

import android.app.Application
import com.mcc.signsaya.repository.AuthRepository
import com.mcc.signsaya.viewmodel.ViewModelFactory

class SignSayaApplication : Application() {

    lateinit var authRepository: AuthRepository
    lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate() {
        super.onCreate()
        
        authRepository = AuthRepository()
        viewModelFactory = ViewModelFactory(authRepository)
    }
}
