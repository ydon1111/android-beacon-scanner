package com.example.android_beacon_scanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.service.ConnectScreenService
import com.example.android_beacon_scanner.ui.ConnectScreen
import com.example.android_beacon_scanner.ui.ScanScreen
import com.example.android_beacon_scanner.ui.theme.AndroidbeaconscannerTheme
import com.example.android_beacon_scanner.viewModel.ScanViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bleManager: BleManager

    @Inject
    lateinit var deviceDataRepository: DeviceDataRepository // DeviceDataRepository 주입

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, ConnectScreenService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        if (!isAppWhitelisted()) {
            requestAppWhitelisting()
        }

        setContent {
            AndroidbeaconscannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "ScanScreen") {
                        composable(route = "ScanScreen") {
                            ScanScreen(
                                navController,
                                ScanViewModel(bleManager, deviceDataRepository)
                            )
                        }
                        composable(route = "ConnectScreen") {
                            ConnectScreen(
                                navController,
                                deviceDataRepository,
                                bleManager
                            )
                        }
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissions()
        } else {
            requestLegacyPermissions()
        }
    }

    private fun requestBluetoothPermissions() {
        if (permissionArray.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            requestPermissionLauncher.launch(permissionArray)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val permissionArray =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Log.d("DEBUG", "${it.key} = ${it.value}")
            if (!it.value) {
                Toast.makeText(this, "Permission ${it.key} not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestLegacyPermissions() {
        requestPermissionLauncher.launch(permissionArray)
    }

    private fun isAppWhitelisted(): Boolean {
        val packageName = packageName
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isWhitelisted = powerManager.isIgnoringBatteryOptimizations(packageName)
        Log.d("MainActivity", "Is whitelisted: $isWhitelisted")
        return isWhitelisted
    }

    @SuppressLint("BatteryLife")
    private fun requestAppWhitelisting() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
}
