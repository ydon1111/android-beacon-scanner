package com.example.android_beacon_scanner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication  : Application() {
    // Add this line to include the AppModule in your Dagger Hilt component

}