package com.example.appyHighBrowser

import android.app.Application
import io.realm.Realm

class BrowserApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
    }
}