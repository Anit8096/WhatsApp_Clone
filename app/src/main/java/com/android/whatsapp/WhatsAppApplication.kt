package com.android.whatsapp

import android.app.Application
import com.android.whatsapp.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class WhatsAppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@WhatsAppApplication)
            modules(appModules)
        }
    }
}