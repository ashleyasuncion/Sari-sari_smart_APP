package com.example.sari_sari_smart

import android.app.Application
import com.example.sari_sari_smart.data.local.AppDatabase

class SariSariApp : Application() {

    /** Lazy-initialized Room database singleton */
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
