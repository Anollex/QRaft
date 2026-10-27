package ro.upt.qraft.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ro.upt.qraft.data.local.entity.InventoryEntity

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventories")
    fun getAllInventories(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventories WHERE id = :id")
    fun getInventoryById(id: String): Flow<InventoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(inventory: InventoryEntity)

    @Update
    suspend fun updateInventory(inventory: InventoryEntity)

    @Delete
    suspend fun deleteInventory(inventory: InventoryEntity)
}
