package com.example.android_beacon_scanner

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.android_beacon_scanner.ble.BleViewModel
import com.example.android_beacon_scanner.room.AppDatabase
import com.example.android_beacon_scanner.room.DeviceDataDao
import com.example.android_beacon_scanner.room.DeviceDataRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


@HiltAndroidApp
class MyApplication : Application() {

    private var wakeLock: PowerManager.WakeLock? = null
    override fun onCreate() {
        super.onCreate()
        initializeWakeLock()



        try {
            // Initialize the Room database instance
            val appDatabase = AppDatabase.getInstance(this)

            if (appDatabase != null) {
                Log.d("MyApplication", "Room database created successfully")
            } else {
                Log.e("MyApplication", "Failed to create Room database")
            }

            // Dependency Injection (Dagger Hilt)을 사용한다면, 여기서 Repository를 주입합니다.
            val deviceDataDao = appDatabase?.deviceDataDao()
            val deviceDataRepository = deviceDataDao?.let { DeviceDataRepository(it) }

            // 앱 시작 시 데이터 삭제 여부를 사용자에게 확인
            showDataDeletionConfirmationDialog(deviceDataRepository)

        } catch (e: Exception) {
            Log.e("MyApplication", "Error initializing Room database: ${e.message}")
        }
    }

    private fun showDataDeletionConfirmationDialog(deviceDataRepository: DeviceDataRepository?) {
        AlertDialog.Builder(this)
            .setTitle("Confirmation")
            .setMessage("Do you want to delete all data?")
            .setPositiveButton("Yes") { _, _ ->
                // 사용자가 Yes를 선택한 경우 ViewModelProvider를 통해 ViewModel을 생성하고 데이터 삭제 작업을 호출
                val viewModelProvider = ViewModelProvider.AndroidViewModelFactory.getInstance(this)
                val viewModel = viewModelProvider.create(BleViewModel::class.java)
                viewModel.deleteAllDeviceData()
            }
            .setNegativeButton("No") { _, _ ->
                // 사용자가 No를 선택한 경우 아무 동작하지 않음
            }
            .setCancelable(false)
            .show()
    }

    // 필요한 경우 WakeLock을 릴리스하는 메서드
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    override fun onTerminate() {
        // 앱 종료 시 WakeLock을 릴리스
        releaseWakeLock()
        super.onTerminate()
    }

    private fun initializeWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MyApp::MyWakeLockTag"
        )
        wakeLock?.acquire()
    }
}