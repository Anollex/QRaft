package ro.upt.qraft.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ro.upt.qraft.data.local.dao.InventoryDao
import ro.upt.qraft.data.local.dao.ItemDao
import ro.upt.qraft.data.local.dao.LocationDao
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity

@Database(
    entities = [InventoryEntity::class, LocationEntity::class, ItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class QRaftDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
    abstract fun locationDao(): LocationDao
    abstract fun itemDao(): ItemDao
}
