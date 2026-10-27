package ro.upt.qraft.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ro.upt.qraft.data.local.entity.LocationEntity

@Dao
interface LocationDao {
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
}
