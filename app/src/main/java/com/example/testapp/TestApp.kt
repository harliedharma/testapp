package com.example.testapp

import android.app.Application
import com.facebook.soloader.SoLoader

class TestApp: Application() {
    override fun onCreate() {
        super.onCreate()
        SoLoader.init(this, false)
    }
}