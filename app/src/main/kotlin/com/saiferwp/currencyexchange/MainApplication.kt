package com.saiferwp.currencyexchange

import android.app.Application
import com.saiferwp.currencyexchange.di.appModule
import com.saiferwp.currencyexchange.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApplication : Application(){
    override fun onCreate() {
        super.onCreate()

        startKoin{
            androidLogger()
            androidContext(this@MainApplication)
            modules(
                networkModule,
                appModule
            )
        }
    }
}
