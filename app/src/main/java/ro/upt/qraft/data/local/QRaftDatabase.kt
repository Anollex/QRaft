package ro.upt.qraft.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ro.upt.qraft.data.local.dao.InventoryDao
import ro.upt.qraft.data.local.dao.ItemDao
import ro.upt.qraft.data.local.dao.LocationDao
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity
import ro.upt.qraft.utils.InventoryCodes

@Database(
    entities = [InventoryEntity::class, LocationEntity::class, ItemEntity::class],
    version = 3,
    exportSchema = false
)
abstract class QRaftDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
    abstract fun locationDao(): LocationDao
    abstract fun itemDao(): ItemDao

    companion object {
        // v2: coloana pendingSync. Datele existente raman marcate ca nesincronizate (1),
        // deci vor fi urcate in Firestore la prima pornire dupa update.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventories ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE locations ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE items ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 1")
            }
        }

        // v3: membri cu roluri (partajare) + prefixul locatiilor calculat din numele inventarului
        // (inainte prefixul era introdus manual si nu era folosit).
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventories ADD COLUMN memberDetails TEXT NOT NULL DEFAULT ''")

                val now = System.currentTimeMillis()
                db.query("SELECT id, name FROM inventories").use { cursor ->
                    while (cursor.moveToNext()) {
                        val values = ContentValues().apply {
                            put("prefix", InventoryCodes.locationPrefix(cursor.getString(1)))
                            put("updatedAt", now)
                            put("pendingSync", 1)
                        }
                        db.update("inventories", SQLiteDatabase.CONFLICT_NONE, values, "id = ?", arrayOf(cursor.getString(0)))
                    }
                }
            }
        }
    }
}
