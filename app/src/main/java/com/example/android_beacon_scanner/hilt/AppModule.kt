
import android.content.Context
import androidx.room.Room
import com.example.android_beacon_scanner.BleManager
import com.example.android_beacon_scanner.room.AppDatabase
import com.example.android_beacon_scanner.room.DeviceDataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDatabase(applicationContext: Context): AppDatabase {
        return Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "app-database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideDeviceDataDao(appDatabase: AppDatabase): DeviceDataDao {
        return appDatabase.deviceDataDao()
    }
    @Provides
    @Singleton
    fun provideDeviceDataRepository(deviceDataDao: DeviceDataDao): DeviceDataRepository {
        return DeviceDataRepository(deviceDataDao)
    }

    @Provides
    @Singleton
    fun provideBleManager(context: Context, deviceDataRepository: DeviceDataRepository): BleManager {
        return BleManager(context, deviceDataRepository)
    }
}
