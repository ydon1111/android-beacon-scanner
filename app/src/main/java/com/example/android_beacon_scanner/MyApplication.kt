package com.example.android_beacon_scanner

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.android_beacon_scanner.room.AppDatabase
import com.example.android_beacon_scanner.room.DeviceDataDao
import dagger.hilt.android.HiltAndroidApp



@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize the Room database instance
        val appDatabase = AppDatabase.getInstance(this)

        if (appDatabase != null) {
            Log.d("MyApplication", "Room database created successfully")
        } else {
            Log.e("MyApplication", "Failed to create Room database")
        }
    }
}