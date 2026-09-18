package com.example.dukkanapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DukkanApplication : Application(){
    override fun onCreate() {
        super.onCreate()
    }
}
