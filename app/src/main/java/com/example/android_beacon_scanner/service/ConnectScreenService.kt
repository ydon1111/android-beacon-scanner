package com.example.android_beacon_scanner.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.navigation.NavHostController
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.ConnectScreenActivity
import com.example.android_beacon_scanner.MainActivity
import com.example.android_beacon_scanner.R
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.O)
class ConnectScreenService : LifecycleService() {

    private val CHANNEL_ID = "ConnectScreenServiceChannel"
    private val NOTIFICATION_ID = 1
    private lateinit var deviceDataRepository: DeviceDataRepository

    private lateinit var bleManager: BleManager // BleManager 객체 선언

    // Declare a variable for the wake lock
    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var navController: NavHostController

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate() {
        super.onCreate()
        deviceDataRepository = DeviceDataRepository.getInstance(applicationContext)
        navController = NavHostController(applicationContext)

        // Acquire the wake lock when the service is created
        acquireWakeLock()

        // Foreground Service with ongoing notification
        val notification = createNotification(null)
        startForeground(NOTIFICATION_ID, notification)

        bleManager = BleManager(applicationContext, deviceDataRepository)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Acquire a wake lock
        acquireWakeLock()

        // Move the declaration of deviceData to a higher scope
        val deviceData = intent?.getParcelableExtra<DeviceRoomDataEntity>("deviceData")

        // Foreground Service를 시작합니다.
        startForeground(NOTIFICATION_ID, createNotification(deviceData))

        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onDestroy() {
        super.onDestroy()
        // Release the acquired wake lock when the service is destroyed
        releaseWakeLock()
    }

    private fun createNotification(deviceData: DeviceRoomDataEntity?): Notification {

        val notificationChannelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }

        // ConnectScreen으로 이동하는 Intent 생성
        val connectScreenIntent = Intent(applicationContext, ConnectScreenActivity::class.java)
        // Add your data as extras to the Intent
        connectScreenIntent.putExtra("deviceData", deviceData)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE // Android 12 이상에서는 FLAG_MUTABLE을 사용합니다.
        } else {
            0
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, connectScreenIntent, flags)

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("관절 보정기 앱")
            .setContentText("서울아산병원 보정기 앱이 동작중 입니다")
            .setSmallIcon(R.drawable.baseline_local_hospital_24)
            .setOngoing(false)  // This makes the notification ongoing
            .setContentIntent(pendingIntent) // PendingIntent 설정
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
}



