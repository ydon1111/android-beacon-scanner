package com.example.android_beacon_scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.util.containsKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import com.example.android_beacon_scanner.worker.ConnectScreenWorker
import com.example.android_beacon_scanner.worker.DataInsertWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleManager @Inject constructor(
    private val context: Context, private val deviceDataRepository: DeviceDataRepository
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private var scanList: SnapshotStateList<DeviceRoomDataEntity>? = null
    private var connectedStateObserver: BleInterface? = null
    var bleGatt: BluetoothGatt? = null

    private var bleDataCount = 0

    private var connectedDevice: DeviceRoomDataEntity? = null

    fun setConnectedDevice(deviceData: DeviceRoomDataEntity) {
        connectedDevice = deviceData
    }

    // 현재 연결된 BLE 장치를 반환하는 메서드 추가
    fun getConnectedDevice(): DeviceRoomDataEntity? {
        return connectedDevice
    }


    private val scanCallback: ScanCallback = @RequiresApi(Build.VERSION_CODES.O)
    object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name
            if (deviceName != null && deviceName.contains("M")) {
                val manufacturerData = result.scanRecord?.manufacturerSpecificData
                if (manufacturerData != null && manufacturerData.containsKey(16505)) {

                    val manufacturerDataValue = manufacturerData[16505]
                    val manufacturerDataIntArray =
                        manufacturerDataValue?.map { it.toInt() }?.toIntArray()

                    Log.d("onScanResult", result.toString())
//                    Log.d("manufacturerData", manufacturerDataIntArray.toString())

                    val temperature = manufacturerDataIntArray?.let {
                        if (it.size >= 2) {
                            it[it.size - 2] // Second-to-last number
                        } else {
                            null // Return null if the array is too short
                        }
                    }

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                    val timestampNano = result.timestampNanos
                    // Format timestampNano to a readable date-time string
                    val formattedTimestamp =
                        dateFormat.format(Date(timestampNano / 1000000L)) // Convert nanoseconds to milliseconds

                    val currentDateAndTime = Date()
                    val formattedDate = dateFormat.format(currentDateAndTime)

//                    Log.d("BleManager", "Formatted Timestamp: $formattedTimestamp")
//                    Log.d("BleManager", "temperature: ${temperature?.toString() ?: "null"}")

                    val startIndex = 0  // 시작 인덱스
                    val step = 12       // 간격
                    val count = 18      // 추출할 값의 개수

                    for (i in 0 until count) {
                        val indexX = startIndex + i * step
                        val indexY = (startIndex + 2) + i * step
                        val indexZ = (startIndex + 4) + i * step

                        val valueX = if (indexX < (manufacturerDataIntArray?.size ?: 0)) {
                            manufacturerDataIntArray?.get(indexX) ?: 0
                        } else {
                            0
                        }

                        val valueY = if (indexY < (manufacturerDataIntArray?.size ?: 0)) {
                            manufacturerDataIntArray?.get(indexY) ?: 0
                        } else {
                            0
                        }

                        val valueZ = if (indexZ < (manufacturerDataIntArray?.size ?: 0)) {
                            manufacturerDataIntArray?.get(indexZ) ?: 0
                        } else {
                            0
                        }

                        // 데이터베이스에 값 넣기
                        val scanItem = DeviceRoomDataEntity(
                            deviceName = deviceName,
                            deviceAddress = result.device.address ?: "null",
                            manufacturerData = manufacturerData[16505],
                            temperature = temperature,
                            bleDataCount = bleDataCount, // 현재 i를 사용하여 0부터 17까지 증가
                            currentDateAndTime = formattedDate,
                            timestampNanos = formattedTimestamp,
                            valueX = valueX,
                            valueY = valueY,
                            valueZ = valueZ
                        )

                        MainScope().launch(Dispatchers.IO) {
                            deviceDataRepository.insertDeviceData(scanItem)
                            Log.d("BleManager", "Inserted data into Room database: $scanItem")
                        }
                        scanList?.add(scanItem)
                        // 추출된 값들을 로그로 출력
//                        Log.d("ACC_X", "Value $i: $valueX")
//                        Log.d("ACC_Y", "Value $i: $valueY")
//                        Log.d("ACC_Z", "Value $i: $valueZ")
                    }
                    bleDataCount++
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            println("onScanFailed  $errorCode")
        }

//        fun sendDataToDataInsertWorker(data: InsertData) {
//            val workRequest = OneTimeWorkRequestBuilder<DataInsertWorker>()
//                .setInputData(
//                    workDataOf(
//                        "deviceName" to data.deviceName,
//                        "deviceAddress" to data.deviceAddress,
//                        "manufacturerData" to data.manufacturerData,
//                        "temperature" to data.temperature,
//                        "bleDataCount" to data.bleDataCount,
//                        "currentDateAndTime" to data.currentDateAndTime,
//                        "timestampNanos" to data.timestampNanos,
//                        "valueX" to data.valueX,
//                        "valueY" to data.valueY,
//                        "valueZ" to data.valueZ
//                    )
//                )
//                .build()
//
//            WorkManager.getInstance(context).enqueue(workRequest)
//        }
//
//
//        // LifecycleObserver를 사용하여 앱의 상태를 관찰
//        private val appLifecycleObserver = object : LifecycleObserver {
//            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
//            fun onBackground() {
//                // 앱이 백그라운드로 이동할 때 BLE 스캔을 시작하도록 WorkManager를 설정
//                Handler(Looper.getMainLooper()).post {
//                    // Call addObserver on the main thread
//                    setupBleScanWorkManager()
//                }
//            }
//
//            @OnLifecycleEvent(Lifecycle.Event.ON_START)
//            fun onForeground() {
//                // 앱이 포그라운드로 돌아올 때 BLE 스캔을 중지
//                stopBleScan()
//            }
//        }
//
//
//
//


//        // 앱이 백그라운드로 이동할 때 BLE 스캔을 주기적으로 시작하는 WorkManager 설정
//        @SuppressLint("InvalidPeriodicWorkRequestInterval")
//        private fun setupBleScanWorkManager() {
//            val workRequest = PeriodicWorkRequest.Builder(
//                ConnectScreenWorker::class.java,
//                4, // 주기 (초 단위)
//                TimeUnit.SECONDS
//            ).build()
//
//            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
//                "BleScanWork",
//                ExistingPeriodicWorkPolicy.REPLACE,
//                workRequest
//            )
//        }

//        private fun setupDataInsertWorkManager() {
//            val workRequest = PeriodicWorkRequest.Builder(
//                DataInsertWorker::class.java,
//                15, // 주기 (분 단위)
//                TimeUnit.MINUTES // 원하는 주기에 맞게 설정
//            ).build()
//
//            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
//                "DataInsertWork",
//                ExistingPeriodicWorkPolicy.REPLACE,
//                workRequest
//            )
//        }

//        init {
//            Handler(Looper.getMainLooper()).post {
//                ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
//            }
//        }

    }


    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(
            gatt: BluetoothGatt?, status: Int, newState: Int
        ) {
            super.onConnectionStateChange(gatt, status, newState)

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BleManager", "Connected")
                gatt?.discoverServices()
                connectedStateObserver?.onConnectedStateObserve(
                    true, "Connected"
                )
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BleManager", "Disconnected")
                connectedStateObserver?.onConnectedStateObserve(
                    false, "Disconnected"
                )
                bleDataCount = 0
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                MainScope().launch {
                    bleGatt = gatt
                    Toast.makeText(
                        context, "Connected to ${gatt?.device?.name}", Toast.LENGTH_SHORT
                    ).show()
                    var sendText = "Services Discovered: GATT_SUCCESS\n"

                    for (service in gatt?.services!!) {
                        sendText += "- " + service.uuid.toString() + "\n"
                        for (characteristics in service.characteristics) {
                            sendText += "       " + characteristics.uuid.toString() + "\n"
                            // Read characteristic data and store in the Room database
                            gatt.readCharacteristic(characteristics)
                        }
                    }
                    sendText += "---"
                    connectedStateObserver?.onConnectedStateObserve(
                        true, sendText
                    )

                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    fun startBleScan() {
        scanList?.clear()

        val scanSettings =
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).setLegacy(false)
                .build()
        bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        bluetoothLeScanner.stopScan(scanCallback)
    }

//    @SuppressLint("MissingPermission")
//    suspend fun startBleConnectGatt(deviceData: DeviceRoomDataEntity) {
//        bluetoothAdapter.getRemoteDevice(deviceData.deviceAddress)
//            .connectGatt(context, false, gattCallback)
//
//        // Insert the data into the database when connecting to the device
//        insertDeviceDataIfNotExists(deviceData)
//    }

    fun setScanList(pScanList: SnapshotStateList<DeviceRoomDataEntity>) {
        scanList = pScanList
    }

    fun onConnectedStateObserve(pConnectedStateObserver: BleInterface) {
        connectedStateObserver = pConnectedStateObserver
    }

//    @SuppressLint("MissingPermission")
//    private suspend fun isDeviceDataExists(deviceAddress: String): Boolean {
//        return deviceDataRepository.isDeviceDataExists(deviceAddress)
//    }
//
//    private suspend fun insertDeviceDataIfNotExists(deviceData: DeviceRoomDataEntity) {
//        val deviceAddress = deviceData.deviceAddress
//        if (!isDeviceDataExists(deviceAddress)) {
//            deviceDataRepository.insertDeviceData(deviceData)
//        }
//    }

}








