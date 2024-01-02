package com.example.android_beacon_scanner

import android.Manifest
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
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.app.ActivityCompat
import androidx.core.util.containsKey
import com.example.android_beacon_scanner.room.DeviceDataRepository
import com.example.android_beacon_scanner.room.DeviceRoomData
import com.example.android_beacon_scanner.room.DeviceRoomDataEntity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
    private var scanList: SnapshotStateList<DeviceRoomDataEntity>? = null
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

                    val scanItem = DeviceRoomDataEntity(
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

                    scanList?.add(scanItem)
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

                    // Extract and insert the data into the Room database
                    val numDataPoints = 18
                    val startIndex = 2
                    val accXList = mutableListOf<Int>()
                    val accYList = mutableListOf<Int>()
                    val accZList = mutableListOf<Int>()
                    val gyroXList = mutableListOf<Int>()
                    val gyroYList = mutableListOf<Int>()
                    val gyroZList = mutableListOf<Int>()

                    for (i in 0 until numDataPoints) {
                        val offset = startIndex + 12 * i
                        val accX =
                            ByteBuffer.wrap(manufacturerData, offset, 2)
                                .order(ByteOrder.LITTLE_ENDIAN)
                                .short.toInt()
                        val accY =
                            ByteBuffer.wrap(manufacturerData, offset + 2, 2)
                                .order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                        val accZ =
                            ByteBuffer.wrap(manufacturerData, offset + 4, 2)
                                .order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                        val gyroX =
                            ByteBuffer.wrap(manufacturerData, offset + 6, 2)
                                .order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                        val gyroY =
                            ByteBuffer.wrap(manufacturerData, offset + 8, 2)
                                .order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                        val gyroZ =
                            ByteBuffer.wrap(manufacturerData, offset + 10, 2)
                                .order(ByteOrder.LITTLE_ENDIAN).short.toInt()

                        accXList.add(accX)
                        accYList.add(accY)
                        accZList.add(accZ)
                        gyroXList.add(gyroX)
                        gyroYList.add(gyroY)
                        gyroZList.add(gyroZ)
                    }

                    val manufacturerData = characteristic?.value
                    val serviceUuid = characteristic?.uuid.toString()

                    if (manufacturerData != null) {
                        val deviceName = gatt?.device?.name ?: "null"
                        val deviceAddress = gatt?.device?.address ?: "null"

                        // Create a data entity for each data point and insert into Room database
                        for (i in 0 until numDataPoints) {
                            val deviceData = DeviceRoomDataEntity(
                                deviceName = deviceName,
                                serviceUuid = serviceUuid,
                                deviceAddress = deviceAddress,
                                accX = accXList[i],
                                accY = accYList[i],
                                accZ = accZList[i],
                                gyroX = gyroXList[i],
                                gyroY = gyroYList[i],
                                gyroZ = gyroZList[i],
                                manufacturerData = manufacturerData // Provide the manufacturerData here
                            )
                            MainScope().launch {
                                deviceDataRepository.insertDeviceData(deviceData)
                            }
                        }

                        // Extract additional data fields and insert into Room database
                        val dataView =
                            ByteBuffer.wrap(manufacturerData).order(ByteOrder.LITTLE_ENDIAN)
                        val date = dataView.getInt(0)  // Assuming date is a 32-bit integer
                        val temperature =
                            dataView.getShort(8) / 100.0  // Assuming temperature is a 16-bit integer (centigrade)
                        val velcro =
                            dataView.get(10) != 0.toByte()  // Assuming velcro is a boolean (single byte)
                        val count = dataView.getShort(11)
                            .toInt()  // Assuming count is a 16-bit unsigned integer

                        // Insert the additional data into the Room database
                        val additionalData = DeviceRoomDataEntity(
                            deviceName = deviceName,
                            serviceUuid = serviceUuid,
                            deviceAddress = deviceAddress,
                            date = date,
                            temperature = temperature,
                            velcro = velcro,
                            count = count,
                            manufacturerData = manufacturerData // Provide the manufacturerData here as well
                        )

                        MainScope().launch {
                            deviceDataRepository.insertDeviceData(additionalData)
                        }
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
            bluetoothAdapter.getRemoteDevice(deviceData.deviceAddress)
                .connectGatt(context, false, gattCallback)
        }

        fun setScanList(pScanList: SnapshotStateList<DeviceRoomDataEntity>) {
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
        private suspend fun insertDeviceDataIfNotExists(deviceData: DeviceRoomDataEntity) {
            val deviceAddress = deviceData.deviceAddress
            if (!isDeviceDataExists(deviceAddress)) {
                deviceDataRepository.insertDeviceData(deviceData)
            }
        }
    }




