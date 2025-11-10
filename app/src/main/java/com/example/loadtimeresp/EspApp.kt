package com.example.loadtimeresp

import android.app.Application
import android.content.Intent
import android.content.ServiceConnection
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.concurrent.Executor

@HiltAndroidApp
class EspApp: Application(){
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Timber.d("App created")
    }
}