package ro.upt.qraft.data.repository

import kotlinx.coroutines.flow.Flow
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity

interface InventoryRepository {
    fun getAllInventories(): Flow<List<InventoryEntity>>
    fun getInventoryById(id: String): Flow<InventoryEntity?>
    suspend fun createInventory(name: String, ownerId: String, prefix: String)
    suspend fun deleteInventory(inventory: InventoryEntity)

    fun getRootLocations(inventoryId: String): Flow<List<LocationEntity>>
    fun getChildLocations(parentId: String): Flow<List<LocationEntity>>
    fun getLocationById(id: String): Flow<LocationEntity?>
    suspend fun createLocation(inventoryId: String, parentId: String?, code: String, name: String, description: String)
    suspend fun updateLocation(location: LocationEntity)
    suspend fun deleteLocation(location: LocationEntity)

    fun getItems(locationId: String): Flow<List<ItemEntity>>
    fun getItemById(id: String): Flow<ItemEntity?>
    suspend fun createItem(inventoryId: String, locationId: String, code: String, name: String, category: String, quantity: Int, notes: String)
    suspend fun updateItem(item: ItemEntity)
    suspend fun deleteItem(item: ItemEntity)
}
