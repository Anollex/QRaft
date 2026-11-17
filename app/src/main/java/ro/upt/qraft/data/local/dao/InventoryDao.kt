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

    // --- Sincronizare ---

    @Query("SELECT * FROM inventories")
    suspend fun getAllInventoriesOnce(): List<InventoryEntity>

    @Query("SELECT * FROM inventories WHERE id = :id")
    suspend fun getInventoryOnce(id: String): InventoryEntity?

    @Query("SELECT * FROM inventories WHERE pendingSync = 1")
    suspend fun getPendingInventories(): List<InventoryEntity>

    // Marcam ca sincronizat doar daca randul nu s-a modificat intre timp
    @Query("UPDATE inventories SET pendingSync = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    // @Upsert (spre deosebire de REPLACE) nu sterge randul, deci nu declanseaza CASCADE pe copii
    @Upsert
    suspend fun upsertInventories(inventories: List<InventoryEntity>)

    @Query("DELETE FROM inventories WHERE id IN (:ids)")
    suspend fun deleteInventoriesByIds(ids: List<String>)
}
