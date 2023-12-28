package com.example.android_beacon_scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.util.containsKey
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomData
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleManager @Inject constructor(
    private val context: Context,
    private val deviceDataRepository: DeviceDataRepository
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private var scanList: SnapshotStateList<DeviceRoomData>? = null
    private var connectedStateObserver: BleInterface? = null
    var bleGatt: BluetoothGatt? = null


    private val scanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device.name
            if (deviceName != null && deviceName.contains("M")) {
                val manufacturerData = result.scanRecord?.manufacturerSpecificData
                if (manufacturerData != null && manufacturerData.containsKey(16505)) {

                    Log.d("onScanResult", result.toString())

                    val uuid = result.scanRecord?.serviceUuids?.toString() ?: "null"
                    val scanItem = DeviceRoomData(
                        deviceName = deviceName,
                        serviceUuid = uuid,
                        deviceAddress = result.device.address ?: "null",
                        manufacturerData = manufacturerData[16505]
                    )

                    // Insert the scan result into the Room database
                    MainScope().launch {
                        deviceDataRepository.insertDeviceData(scanItem)
                        Log.d("BleManager", "Inserted data into Room database: $scanItem")
                    }

                    if (!scanList!!.contains(scanItem)) {
                        scanList!!.add(scanItem)
                    }
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
            gatt: BluetoothGatt?,
            status: Int,
            newState: Int
        ) {
            super.onConnectionStateChange(gatt, status, newState)

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BleManager", "Connected")
                gatt?.discoverServices()
                connectedStateObserver?.onConnectedStateObserve(
                    true,
                    "Connected"
                )
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BleManager", "Disconnected")
                connectedStateObserver?.onConnectedStateObserve(
                    false,
                    "Disconnected"
                )
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                MainScope().launch {
                    bleGatt = gatt
                    Toast.makeText(
                        context,
                        "Connected to ${gatt?.device?.name}",
                        Toast.LENGTH_SHORT
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
                        true,
                        sendText
                    )



                }.cancel()
            }
        }



        @SuppressLint("MissingPermission")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                val manufacturerData = characteristic?.value
                val serviceUuid = characteristic?.uuid.toString()

                if (manufacturerData != null) {
                    val deviceName = gatt?.device?.name ?: "null"
                    val deviceAddress = gatt?.device?.address ?: "null"

                        // Insert the data into the Room database
                        val deviceData = DeviceRoomData(
                            deviceName = deviceName,
                            serviceUuid = serviceUuid,
                            deviceAddress = deviceAddress,
                            manufacturerData = manufacturerData
                        )
                        MainScope().launch {
                            deviceDataRepository.insertDeviceData(deviceData)
                        }

                }
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    fun startBleScan() {
        scanList?.clear()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setLegacy(false)
            .build()
        bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        bluetoothLeScanner.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun startBleConnectGatt(deviceData: DeviceRoomData) {
        bluetoothAdapter.getRemoteDevice(deviceData.deviceAddress).connectGatt(context, false, gattCallback)
    }

    fun setScanList(pScanList: SnapshotStateList<DeviceRoomData>) {
        scanList = pScanList
    }

    fun onConnectedStateObserve(pConnectedStateObserver: BleInterface) {
        connectedStateObserver = pConnectedStateObserver
    }

    @SuppressLint("MissingPermission")
    private suspend fun isDeviceDataExists(deviceAddress: String): Boolean {
        return deviceDataRepository.isDeviceDataExists(deviceAddress)
    }

    @SuppressLint("MissingPermission")
    private suspend fun insertDeviceDataIfNotExists(deviceData: DeviceRoomData) {
        val deviceAddress = deviceData.deviceAddress
        if (!isDeviceDataExists(deviceAddress)) {
            deviceDataRepository.insertDeviceData(deviceData)
        }
    }






}


//// Sample data extraction and conversion
//val numDataPoints = 18 // The number of data points to extract
//
//val ACC_X = mutableListOf<Int>()
//val ACC_Y = mutableListOf<Int>()
//val ACC_Z = mutableListOf<Int>()
//val Gyro_X = mutableListOf<Int>()
//val Gyro_Y = mutableListOf<Int>()
//val Gyro_Z = mutableListOf<Int>()
//
//for (i in 0 until numDataPoints) {
//    val startIndex = 2 + 12 * i // Adjust this index based on your data format
//
//    val accX = ByteBuffer.wrap(manufacturerSpecificDataBytes, startIndex, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
//    val accY = ByteBuffer.wrap(manufacturerSpecificDataBytes, startIndex + 2, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
//    val accZ = ByteBuffer.wrap(manufacturerSpecificDataBytes, startIndex + 4, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
//    val gyroX = ByteBuffer.wrap(manufacturerSpecificDataBytes, startIndex + 6, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
//    val gyroY = ByteBuffer.wrap(manufacturerSpecificDataBytes, startIndex + 8, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
//    val gyroZ = ByteBuffer.wrap(manufacturerSpecificDataBytes, startIndex + 10, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
//
//    ACC_X.add(accX)
//    ACC_Y.add(accY)
//    ACC_Z.add(accZ)
//    Gyro_X.add(gyroX)
//    Gyro_Y.add(gyroY)
//    Gyro_Z.add(gyroZ)
//}