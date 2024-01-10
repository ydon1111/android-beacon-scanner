package com.example.android_beacon_scanner

import android.app.AlertDialog
import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.example.android_beacon_scanner.ble.BleViewModel
import com.example.android_beacon_scanner.room.AppDatabase
import com.example.android_beacon_scanner.room.DeviceDataRepository
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

//        setupWorkManager(applicationContext)



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
        AlertDialog.Builder(this@MyApplication)
            .setTitle("Confirmation")
            .setMessage("Do you want to delete all data?")
            .setPositiveButton("Yes") { _, _ ->
                // 사용자가 Yes를 선택한 경우 ViewModelProvider를 통해 ViewModel을 생성하고 데이터 삭제 작업을 호출
                val viewModelProvider = ViewModelProvider.AndroidViewModelFactory.getInstance(this@MyApplication)
                val viewModel = viewModelProvider.create(BleViewModel::class.java)
                viewModel.deleteAllDeviceData()
            }
            .setNegativeButton("No") { _, _ ->
                // 사용자가 No를 선택한 경우 아무 동작하지 않음
            }
            .setCancelable(false)
            .show()
    }



    override fun onTerminate() {
        super.onTerminate()
    }

}