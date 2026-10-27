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
}
