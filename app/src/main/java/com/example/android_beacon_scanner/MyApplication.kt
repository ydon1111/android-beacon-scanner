package com.example.android_beacon_scanner

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.android_beacon_scanner.room.AppDatabase
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val appDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "device_data"
        ).build()

        if (appDatabase.isOpen) {
            Log.d("MyApplication", "Room database created successfully")
        } else {
            Log.e("MyApplication", "Failed to create Room database")
        }
    }
}