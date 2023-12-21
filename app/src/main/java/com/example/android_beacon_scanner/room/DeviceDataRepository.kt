
import androidx.lifecycle.LiveData
import com.example.android_beacon_scanner.room.DeviceDataDao
import com.example.android_beacon_scanner.room.DeviceRoomData
import javax.inject.Inject

class DeviceDataRepository @Inject constructor(private val deviceDataDao: DeviceDataDao) {
    val allDeviceRoomData: LiveData<List<DeviceRoomData>> = deviceDataDao.getAllDeviceData()

    suspend fun insertDeviceData(deviceRoomData: DeviceRoomData) {
        deviceDataDao.insertDeviceData(deviceRoomData)
    }
}