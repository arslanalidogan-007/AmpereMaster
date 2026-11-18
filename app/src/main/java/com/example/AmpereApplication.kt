package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.BatteryRepository

/**
 * Anwendungsweite Application-Klasse zur Bereitstellung von Singleton-Instanzen
 * für Room-Datenbank und BatteryRepository.
 */
class AmpereApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: BatteryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = BatteryRepository(this, database)
    }
}
