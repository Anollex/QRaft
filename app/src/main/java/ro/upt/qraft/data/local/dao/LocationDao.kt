package ro.upt.qraft.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ro.upt.qraft.data.local.entity.LocationEntity

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations WHERE inventoryId = :inventoryId")
    fun getLocationsForInventory(inventoryId: String): Flow<List<LocationEntity>>
    
    @Query("SELECT * FROM locations WHERE inventoryId = :inventoryId AND parentId IS NULL")
    fun getRootLocationsForInventory(inventoryId: String): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE parentId = :parentId")
    fun getChildLocations(parentId: String): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :id")
    fun getLocationById(id: String): Flow<LocationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)

    // --- Sincronizare ---

    @Query("SELECT * FROM locations WHERE inventoryId = :inventoryId")
    suspend fun getLocationsForInventoryOnce(inventoryId: String): List<LocationEntity>

    @Query("SELECT id FROM locations WHERE inventoryId = :inventoryId")
    suspend fun getLocationIdsForInventory(inventoryId: String): List<String>

    @Query("SELECT * FROM locations WHERE pendingSync = 1")
    suspend fun getPendingLocations(): List<LocationEntity>

    @Query("UPDATE locations SET pendingSync = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Upsert
    suspend fun upsertLocations(locations: List<LocationEntity>)

    @Query("DELETE FROM locations WHERE id IN (:ids)")
    suspend fun deleteLocationsByIds(ids: List<String>)
}
