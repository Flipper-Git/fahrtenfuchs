package com.autokm.app

import android.app.Application
import com.autokm.app.data.AppDatabase
import com.autokm.app.data.OrtImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FahrtenfuchsApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.get(this)
        applicationScope.launch {
            OrtImporter.importiereFallsLeer(this@FahrtenfuchsApp, database.ortDao())
        }
    }
}
