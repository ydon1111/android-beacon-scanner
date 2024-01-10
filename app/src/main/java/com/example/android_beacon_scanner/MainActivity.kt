package com.example.android_beacon_scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.ui.ConnectScreen
import com.example.android_beacon_scanner.ui.ScanScreen
import com.example.android_beacon_scanner.ui.theme.AndroidbeaconscannerTheme
import com.example.android_beacon_scanner.service.ConnectScreenService
import com.example.android_beacon_scanner.worker.ConnectScreenWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var bleManager: BleManager

    @Inject
    lateinit var deviceDataRepository: DeviceDataRepository // DeviceDataRepository 주입



    // onCreate 메서드
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startForegroundServiceIfNeeded()

        // WorkManager를 사용하여 ConnectScreenWorker를 예약하고 실행
        val repeatIntervalMinutes: Long = 1 // 작업 주기 (분 단위)
        scheduleConnectScreenWorker(repeatIntervalMinutes)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            AndroidbeaconscannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "ScanScreen") {
                        composable(route = "ScanScreen") { ScanScreen(navController, bleManager) }
                        composable(route = "ConnectScreen") {
                            ConnectScreen(
                                navController,
                                bleManager,
                                deviceDataRepository
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

    private fun scheduleConnectScreenWorker(repeatIntervalMinutes: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // 필요한 네트워크 연결 유형을 설정 (선택 사항)
            .build()

        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            ConnectScreenWorker::class.java,
            repeatIntervalMinutes, // 작업 주기 (분 단위)
            TimeUnit.MINUTES
        )
            .setConstraints(constraints) // 작업 제약 조건 설정 (선택 사항)
            .build()

        // WorkManager에 주기적인 작업 예약
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "ConnectScreenWorker",
            ExistingPeriodicWorkPolicy.REPLACE, // 기존 작업을 대체하도록 설정
            periodicWorkRequest
        )
    }

    private fun startForegroundServiceIfNeeded() {
        val serviceIntent = Intent(this, ConnectScreenService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }


    private val permissionArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        )
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Log.d("DEBUG", "${it.key} = ${it.value}")
        }
    }
}




