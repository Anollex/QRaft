package ro.upt.qraft.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ro.upt.qraft.data.local.QRaftDatabase
import ro.upt.qraft.data.local.dao.InventoryDao
import ro.upt.qraft.data.local.dao.ItemDao
import ro.upt.qraft.data.local.dao.LocationDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QRaftDatabase {
        return Room.databaseBuilder(
            context,
            QRaftDatabase::class.java,
            "qraft_database"
        ).build()
    }

    @Provides
    fun provideInventoryDao(database: QRaftDatabase): InventoryDao = database.inventoryDao()

    @Provides
    fun provideLocationDao(database: QRaftDatabase): LocationDao = database.locationDao()

    @Provides
    fun provideItemDao(database: QRaftDatabase): ItemDao = database.itemDao()
}
