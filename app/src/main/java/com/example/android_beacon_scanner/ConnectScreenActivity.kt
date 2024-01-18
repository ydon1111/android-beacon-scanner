package com.example.android_beacon_scanner

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import com.example.android_beacon_scanner.ui.ConnectScreen
import com.example.android_beacon_scanner.ui.theme.AndroidbeaconscannerTheme
import kotlinx.coroutines.flow.firstOrNull

class ConnectScreenActivity : ComponentActivity() {



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidbeaconscannerTheme {
                // Create a NavHostController for navigation
                val navController = rememberNavController()

                // Create an instance of DeviceDataRepository
                val deviceDataRepository = DeviceDataRepository.getInstance(applicationContext)

                // Use remember to hold the deviceData while it's being loaded
                var deviceData by remember { mutableStateOf<DeviceRoomDataEntity?>(null) }

               val bleManager = BleManager(applicationContext, deviceDataRepository)

                // Load the deviceData asynchronously using a coroutine
                LaunchedEffect(deviceDataRepository) {
                    val data = deviceDataRepository.observeLatestDeviceData("").firstOrNull()
                    deviceData = data
                }

                // Set up the ConnectScreen with the NavHostController and deviceData
                ConnectScreen(navController, deviceDataRepository,bleManager)
            }
        }
    }

    companion object {
        // Helper function to start ConnectScreenActivity
        fun start(context: Context) {
            val intent = Intent(context, ConnectScreenActivity::class.java)
            context.startActivity(intent)
        }
    }
}