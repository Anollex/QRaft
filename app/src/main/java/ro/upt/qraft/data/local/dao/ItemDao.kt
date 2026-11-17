package ro.upt.qraft.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ro.upt.qraft.data.local.entity.ItemEntity

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE locationId = :locationId")
    fun getItemsForLocation(locationId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun getItemById(id: String): Flow<ItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Delete
    suspend fun deleteItem(item: ItemEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM items WHERE inventoryId = :inventoryId AND code = :code)")
    suspend fun codeExists(inventoryId: String, code: String): Boolean

    // --- Sincronizare ---

    @Query("SELECT * FROM items WHERE inventoryId = :inventoryId")
    suspend fun getItemsForInventoryOnce(inventoryId: String): List<ItemEntity>

    @Query("SELECT id FROM items WHERE locationId IN (:locationIds)")
    suspend fun getItemIdsForLocations(locationIds: List<String>): List<String>

    @Query("SELECT * FROM items WHERE pendingSync = 1")
    suspend fun getPendingItems(): List<ItemEntity>

    @Query("UPDATE items SET pendingSync = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Upsert
    suspend fun upsertItems(items: List<ItemEntity>)

    @Query("DELETE FROM items WHERE id IN (:ids)")
    suspend fun deleteItemsByIds(ids: List<String>)
}
