package com.example.android_beacon_scanner


import android.app.Application
import android.util.Log
import com.example.android_beacon_scanner.room.AppDatabase
import com.example.android_beacon_scanner.room.DeviceDataRepository
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()


        try {
            // Initialize the Room database instance
            val appDatabase = AppDatabase.getInstance(this)

            if (appDatabase != null) {
                Log.d("MyApplication", "Room database created successfully")
            } else {
                Log.e("MyApplication", "Failed to create Room database")
            }

        } catch (e: Exception) {
            Log.e("MyApplication", "Error initializing Room database: ${e.message}")
        }
    }



    override fun onTerminate() {
        super.onTerminate()
    }

}