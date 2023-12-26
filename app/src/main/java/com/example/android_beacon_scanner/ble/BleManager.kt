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
){
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private var scanList: SnapshotStateList<DeviceData>? = null
    private var connectedStateObserver: BleInterface? = null
    var bleGatt: BluetoothGatt? = null



    //FD:A3:D4:BE:09:60, C1:7A:35:49:D7:67, DE:FD:1E:16:73:0D
    private val scanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            // Check if the device name contains "M" (uppercase "M")
            val deviceName = result.device.name
            if (deviceName != null && deviceName.contains("M")) {
                // "mManufacturerSpecificData" 맵에서 16505가 포함되었는지 확인
                val manufacturerData = result.scanRecord?.manufacturerSpecificData
                if (manufacturerData != null && manufacturerData.containsKey(16505)) {
                    Log.d("onScanResult", result.toString())

                    val uuid = result.scanRecord?.serviceUuids?.toString() ?: "null"
                    val scanItem = DeviceData(
                        deviceName,
                        uuid,
                        result.device.address ?: "null"
                    )
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

        // GATT의 연결 상태 변경을 감지하는 콜백
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            // 연결이 성공적으로 이루어진 경우
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // GATT 서버에서 사용 가능한 서비스들을 비동기적으로 탐색
                Log.d("BleManager", "연결 성공")
                gatt?.discoverServices()
                connectedStateObserver?.onConnectedStateObserve(
                    true,
                    "onConnectionStateChange:  STATE_CONNECTED"
                            + "\n"
                            + "---"
                )
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // 연결 끊김
                Log.d("BleManager", "연결 해제")
                connectedStateObserver?.onConnectedStateObserve(
                    false,
                    "onConnectionStateChange:  STATE_DISCONNECTED"
                            + "\n"
                            + "---"
                )
            }
        }

        // 장치에 대한 새로운 서비스가 발견되었을 때 호출되는 콜백
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            // 원격 장치가 성공적으로 탐색된 경우
            if(status == BluetoothGatt.GATT_SUCCESS) {
                MainScope().launch {
                    bleGatt = gatt
                    Toast.makeText(context, " ${gatt?.device?.name} 연결 성공", Toast.LENGTH_SHORT).show()
                    var sendText = "onServicesDiscovered:  GATT_SUCCESS" + "\n" + "                         ↓" + "\n"

                    for(service in gatt?.services!!) {
                        sendText += "- " + service.uuid.toString() + "\n"
                        for(characteristics in service.characteristics) {
                            sendText += "       " + characteristics.uuid.toString() + "\n"
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

        @Deprecated("Deprecated in Java")
        @SuppressLint("MissingPermission")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 데이터를 Room 데이터베이스에 저장
                val manufacturerData = characteristic?.value
                val serviceUuid = characteristic?.uuid.toString()

                if (manufacturerData != null) {
                    val deviceData = DeviceRoomData(
                        deviceName = gatt?.device?.name ?: "null",
                        serviceUuid = serviceUuid.toString(),
                        deviceAddress = gatt?.device?.address ?: "null",
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

//        val scanFilter: ScanFilter = ScanFilter.Builder()
//            .setDeviceName("DeviceName")
//            .setDeviceAddress("DeviceAddress")
//            .setServiceUuid(ParcelUuid(serviceUuid))
//            .setManufacturerData(manufacturerId, manufacturerData, manufacturerDataMask)
//            .setServiceData(serviceUuid, serviceData)
//            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setLegacy(false) // 이 부분이 ble 5.0 ext adv 를 스캔 가능하게함
            .build()
        bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        bluetoothLeScanner.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun startBleConnectGatt(deviceData: DeviceData) {
        bluetoothAdapter
            .getRemoteDevice(deviceData.address)
            .connectGatt(context, false, gattCallback)
    }

    fun setScanList(pScanList: SnapshotStateList<DeviceData>) {
        scanList = pScanList
    }

    fun onConnectedStateObserve(pConnectedStateObserver: BleInterface) {
        connectedStateObserver = pConnectedStateObserver
    }
}