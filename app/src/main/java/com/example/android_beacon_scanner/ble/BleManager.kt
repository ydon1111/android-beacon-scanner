package com.example.android_beacon_scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    private var isScanning = false // 스캔 상태를 나타내는 변수

    private var connectedDevice: DeviceRoomDataEntity? = null

    private var connectionRetries = 0

    fun setConnectedDevice(deviceData: DeviceRoomDataEntity) {
        connectedDevice = deviceData
    }

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

                    val temperature = manufacturerDataIntArray?.let {
                        if (it.size >= 2) {
                            it[it.size - 2] // Second-to-last number
                        } else {
                            null
                        }
                    }

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                    val timestampNano = result.timestampNanos
                    val formattedTimestamp =
                        dateFormat.format(Date(timestampNano / 1000000L))

                    val currentDateAndTime = Date()
                    val formattedDate = dateFormat.format(currentDateAndTime)

                    val startIndex = 0
                    val step = 12
                    val count = 18

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

                        val scanItem = DeviceRoomDataEntity(
                            deviceName = deviceName,
                            deviceAddress = result.device.address ?: "null",
                            manufacturerData = manufacturerData[16505],
                            temperature = temperature,
                            bleDataCount = bleDataCount,
                            currentDateAndTime = formattedDate,
                            timestampNanos = formattedTimestamp,
                            valueX = valueX,
                            valueY = valueY,
                            valueZ = valueZ
                        )

                        MainScope().launch(Dispatchers.IO) {
                            deviceDataRepository.insertDeviceData(scanItem)
                        }

                        scanList?.add(scanItem)
                    }
                    bleDataCount++
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            println("onScanFailed  $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(
            gatt: BluetoothGatt?, status: Int, newState: Int
        ) {
            super.onConnectionStateChange(gatt, status, newState)

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Connection successful, reset connection retries
                connectionRetries = 0
                Log.d("BleManager", "Connected")
                gatt?.discoverServices()
                connectedStateObserver?.onConnectedStateObserve(true, "Connected")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BleManager", "Disconnected")

                // Handle disconnection here
                connectedStateObserver?.onConnectedStateObserve(false, "Disconnected")
                bleDataCount = 0

                // Retry connection (up to a certain number of retries)
                if (connectionRetries < MAX_CONNECTION_RETRIES) {
                    // Retry connecting after a delay
                    val retryDelayMillis = 1000L // 1 second delay (adjust as needed)
                    Handler(Looper.getMainLooper()).postDelayed({
                        gatt?.connect()
                        connectionRetries++
                    }, retryDelayMillis)
                } else {
                    // Handle max retry limit reached
                    Log.e("BleManager", "Max connection retries reached")
                }
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
        if (!isScanning) { // 스캔 중이 아닌 경우에만 시작
            scanList?.clear()

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

            bluetoothLeScanner.startScan(filters, scanSettings, scanCallback)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        bluetoothLeScanner.stopScan(scanCallback)
    }

    fun setScanList(pScanList: SnapshotStateList<DeviceRoomDataEntity>) {
        scanList = pScanList
    }

    fun onConnectedStateObserve(pConnectedStateObserver: BleInterface) {
        connectedStateObserver = pConnectedStateObserver
    }

    // RoomDB에 데이터를 저장하는 함수 추가
    @RequiresApi(Build.VERSION_CODES.O)
    fun saveDataToDeviceDataRepository(scanItem: DeviceRoomDataEntity) {
        MainScope().launch(Dispatchers.IO) {
            deviceDataRepository.insertDeviceData(scanItem)
        }
    }

    @SuppressLint("MissingPermission")
    fun getScanItem(scanResult: ScanResult?): DeviceRoomDataEntity? {
        if (scanResult != null) {
            val deviceName = scanResult.device.name
            if (deviceName != null && deviceName.contains("M")) {
                val manufacturerData = scanResult.scanRecord?.manufacturerSpecificData
                if (manufacturerData != null && manufacturerData.containsKey(16505)) {
                    val manufacturerDataValue = manufacturerData[16505]
                    val manufacturerDataIntArray =
                        manufacturerDataValue?.map { it.toInt() }?.toIntArray()

                    Log.d("onScanResult", scanResult.toString())

                    val temperature = manufacturerDataIntArray?.let {
                        if (it.size >= 2) {
                            it[it.size - 2] // Second-to-last number
                        } else {
                            null
                        }
                    }

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                    val timestampNano = scanResult.timestampNanos
                    val formattedTimestamp =
                        dateFormat.format(Date(timestampNano / 1000000L))

                    val currentDateAndTime = Date()
                    val formattedDate = dateFormat.format(currentDateAndTime)

                    val startIndex = 0
                    val step = 12
                    val count = 18

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
                        // Create a DeviceRoomDataEntity instance
                        val scanItem = DeviceRoomDataEntity(
                            deviceName = deviceName,
                            deviceAddress = scanResult.device.address ?: "null",
                            manufacturerData = manufacturerData[16505],
                            temperature = temperature,
                            bleDataCount = bleDataCount,
                            currentDateAndTime = formattedDate,
                            timestampNanos = formattedTimestamp,
                            valueX = valueX,
                            valueY = valueY,
                            valueZ = valueZ
                        )
                        return scanItem
                    }
                }
            }
        }
        return null
    }

    private var scanResultCallback: ((ScanResult) -> Unit)? = null

    fun setScanResultCallback(callback: (ScanResult) -> Unit) {
        scanResultCallback = callback
    }

    // Inside BleManager class
    @SuppressLint("MissingPermission")
    fun pairWithBeacon(device: BluetoothDevice) {
        // Initiate a connection to the selected beacon
        bleGatt = device.connectGatt(context, false, gattCallback)
    }
    companion object {
        // Define a constant for the maximum connection retries
        private const val MAX_CONNECTION_RETRIES = 3
    }

}









