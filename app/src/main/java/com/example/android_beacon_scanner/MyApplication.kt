package com.example.android_beacon_scanner

import android.app.Application
import androidx.room.Room
import com.example.android_beacon_scanner.room.AppDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication  : Application() {
    // Add this line to include the AppModule in your Dagger Hilt component
    companion object {
        lateinit var database: AppDatabase
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Room database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

}