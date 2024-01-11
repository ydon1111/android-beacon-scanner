package com.example.android_beacon_scanner.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.R
import com.example.android_beacon_scanner.room.DeviceDataRepository


class ConnectScreenService : LifecycleService() {

    private val CHANNEL_ID = "ConnectScreenServiceChannel"
    private val NOTIFICATION_ID = 1
    private lateinit var deviceDataRepository: DeviceDataRepository
    private lateinit var bleManager: BleManager

    // Declare a variable for the wake lock
    private var wakeLock: PowerManager.WakeLock? = null

    private var count = 0
    private val handler = Handler(Looper.getMainLooper())

    private val runnable = object : Runnable {
        override fun run() {
            // Log the count
            Log.d("ConnectScreenService", "Count: $count")
            count++

            // Schedule the runnable with a delay (e.g., 1 second)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        deviceDataRepository = DeviceDataRepository.getInstance(applicationContext)
        bleManager = BleManager(applicationContext, deviceDataRepository)

        // Acquire the wake lock when the service is created
        acquireWakeLock()

        // Start the periodic task to log the count
        handler.postDelayed(runnable, 1000)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Acquire a wake lock
        acquireWakeLock()

        // Foreground Service with ongoing notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Start BLE scanning
        bleManager.startBleScan()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // BLE scanning must be stopped when the service is destroyed
        bleManager.stopBleScan()

        // Release the acquired wake lock when the service is destroyed
        releaseWakeLock()

        // Stop the periodic task
        handler.removeCallbacks(runnable)
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

    // Function to acquire the wake lock
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

    // Function to release the wake lock
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }
}




