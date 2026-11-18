package com.example

import android.app.Application
import com.example.data.repository.LevelUpRepository
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LevelUpApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val repository: LevelUpRepository by lazy {
        LevelUpRepository(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        applicationScope.launch {
            repository.ensureInitialized()
        }
    }
}
