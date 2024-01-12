package com.example.android_beacon_scanner.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.util.containsKey
import androidx.lifecycle.LifecycleService
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.R
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.O)
class ConnectScreenService : LifecycleService() {

    private val CHANNEL_ID = "ConnectScreenServiceChannel"
    private val NOTIFICATION_ID = 1
    private lateinit var deviceDataRepository: DeviceDataRepository

    // Declare a variable for the wake lock
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    // Variable to keep track of screen state
    private var isScreenOn = true

    // Bluetooth variables
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private val scanCallback: ScanCallback? = BleScanCallback()

    private var bleDataCount = 0

    private var isBleScanning = false // 추가: BLE 스캔 상태를 추적하기 위한 변수

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate() {
        super.onCreate()
        deviceDataRepository = DeviceDataRepository.getInstance(applicationContext)

        // Check for BLUETOOTH_SCAN permission before starting BLE scanning
        if (hasBluetoothScanPermission()) {
            checkBatteryOptimizationExemption()
        } else {
            requestBluetoothScanPermission()
        }

        // Acquire the wake lock when the service is created
        acquireWakeLock()

        // Register a BroadcastReceiver to monitor screen state changes
        val screenStateReceiver = ScreenStateReceiver()
        val screenStateFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, screenStateFilter)

        // Foreground Service with ongoing notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Acquire a wake lock
        acquireWakeLock()

        // Start the periodic task to check screen state
        handler.post(screenStateChecker)

        // Start BLE scanning
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!isBleScanning) {
                // 스크린 상태와 관계 없이 BLE 스캔을 시작합니다.
                startBleScan()
                isBleScanning = true
            }
        }

        // Foreground Service를 시작합니다.
        startForeground(NOTIFICATION_ID, createNotification())

        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onDestroy() {
        super.onDestroy()
        // Release the acquired wake lock when the service is destroyed
        releaseWakeLock()

        // Remove the screen state checker task
        handler.removeCallbacks(screenStateChecker)

        // Stop BLE scanning
//        stopBleScan()
        isBleScanning = false // 스캔이 중지되었음을 표시합니다.
    }

    private fun createNotification(): Notification {
        val notificationChannelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("ConnectScreenService")
            .setContentText("App is running in the background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(false)  // This makes the notification ongoing
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "ConnectScreenServiceChannel"
        val channelName = "ConnectScreenService"
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        return channelId
    }

    // Wake Lock을 사용하여 CPU를 활성 상태로 유지
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MyApp::MyWakeLockTag"
        )

        wakeLock?.let {
            if (!it.isHeld) {
                it.acquire(Long.MAX_VALUE)
            }
        }
    }

    // Wake Lock 해제
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    // Function to check and request battery optimization exemption
    @SuppressLint("BatteryLife")
    private fun checkBatteryOptimizationExemption() {
        val packageName = packageName
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent()
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    // Function to check if BLUETOOTH_SCAN permission is granted
    @RequiresApi(Build.VERSION_CODES.S)
    private fun hasBluetoothScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Function to request BLUETOOTH_SCAN permission
    private fun requestBluetoothScanPermission() {
        // Implement logic to request BLUETOOTH_SCAN permission from the user
        // You can use the permission request flow you have already implemented
    }

    // Function to start BLE scanning
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    fun startBleScan() {
        val scanSettings =
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setLegacy(false)
                .build()

        val filters = mutableListOf<ScanFilter>()
        // 스캔 필터 생성
        val scanFilter = ScanFilter.Builder().run {
            // 16505 키를 가진 제조사 특정 데이터를 탐지하는 필터 설정
            setManufacturerData(16505, byteArrayOf(), byteArrayOf())
            build()
        }
        // 스캔 필터 추가
        filters.add(scanFilter)

        // Request background location permission if not granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // You can request the permission here or handle it in your app's UI
        } else {
            // Start BLE scanning
            val scanCallback = BleScanCallback()
            bluetoothLeScanner!!.startScan(filters, scanSettings, scanCallback)
        }
    }

    // Function to stop BLE scanning
    @RequiresApi(Build.VERSION_CODES.S)
    private fun stopBleScan() {
        if (isBleScanning && bluetoothLeScanner != null && scanCallback != null) {
            bluetoothLeScanner.stopScan(scanCallback)
            isBleScanning = false // 스캔이 중지되었음을 표시합니다.
        }
    }

    // Runnable to check screen state periodically
    private val screenStateChecker = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun run() {
            // 스크린 상태를 확인하지 않고 스캔을 유지합니다.

            startBleScan()
            isBleScanning = true

            // Schedule the next check after a delay (e.g., 1 second)
            handler.postDelayed(this, 1000)
        }
    }

    // BroadcastReceiver to monitor screen state changes
    private inner class ScreenStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (it.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        // Screen is turned on
                        isScreenOn = true
                    }

                    Intent.ACTION_SCREEN_OFF -> {
                        // Screen is turned off
                        isScreenOn = false
                    }
                }
            }
        }
    }
    // Custom BLE scan callback
    private inner class BleScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // 스캔 결과 처리를 여기에 추가합니다.
            // result 변수를 사용하여 스캔된 BLE 디바이스에 대한 정보에 접근할 수 있습니다.
            Log.d("BleScanCallback", "Received BLE scan result: $result")

            // 예를 들어, 스캔 결과를 저장하거나 필요한 작업을 수행할 수 있습니다.
        }
    }
}




