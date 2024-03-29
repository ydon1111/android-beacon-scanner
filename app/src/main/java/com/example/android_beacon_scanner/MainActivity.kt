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
import android.view.WindowManager
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
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import com.example.android_beacon_scanner.ui.ConnectScreen
import com.example.android_beacon_scanner.ui.ScanScreen
import com.example.android_beacon_scanner.ui.theme.AndroidbeaconscannerTheme
import com.example.android_beacon_scanner.service.ConnectScreenService
import com.example.android_beacon_scanner.viewModel.ScanViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var bleManager: BleManager

    @Inject
    lateinit var deviceDataRepository: DeviceDataRepository // DeviceDataRepository 주입

    // 알림 권한 요청 코드
    private val notificationPermissionRequestCode = 123

    // onCreate 메서드
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, ConnectScreenService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Doze 모드 화이트리스트 등록을 확인하고 요청
        if (!isAppWhitelisted()) {
            requestAppWhitelisting()
        }

        // always screen on
//        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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

        if (Build.VERSION.SDK_INT >= 31) {
            if (permissionArray.all {
                    ContextCompat.checkSelfPermission(
                        this,
                        it
                    ) == PackageManager.PERMISSION_GRANTED
                }) {
                Toast.makeText(this, "권한 확인", Toast.LENGTH_SHORT).show()
            } else {
                requestPermissionLauncher.launch(permissionArray)
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        // Wake Lock 해제
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val permissionArray =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BATTERY_STATS,
                Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BATTERY_STATS,
                Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }


    // 알림 권한을 확인하는 함수
    private fun isNotificationPermissionGranted(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.areNotificationsEnabled()
    }

    // 알림 권한 요청 함수
    private fun requestNotificationPermission() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        startActivityForResult(intent, notificationPermissionRequestCode)
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Log.d("DEBUG", "${it.key} = ${it.value}")
        }
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
}




