package ro.upt.qraft.data.repository

import kotlinx.coroutines.flow.Flow
import ro.upt.qraft.data.local.dao.InventoryDao
import ro.upt.qraft.data.local.dao.ItemDao
import ro.upt.qraft.data.local.dao.LocationDao
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity
import javax.inject.Inject

class InventoryRepositoryImpl @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val locationDao: LocationDao,
    private val itemDao: ItemDao
) : InventoryRepository {

    override fun getAllInventories(): Flow<List<InventoryEntity>> = inventoryDao.getAllInventories()

    override fun getInventoryById(id: String): Flow<InventoryEntity?> = inventoryDao.getInventoryById(id)

    override suspend fun createInventory(name: String, ownerId: String, prefix: String) {
        val inventory = InventoryEntity(
            name = name,
            ownerId = ownerId,
            prefix = prefix
        )
        inventoryDao.insertInventory(inventory)
    }

    override suspend fun deleteInventory(inventory: InventoryEntity) {
        inventoryDao.deleteInventory(inventory)
    }

    override fun getRootLocations(inventoryId: String): Flow<List<LocationEntity>> = locationDao.getRootLocationsForInventory(inventoryId)

    override fun getChildLocations(parentId: String): Flow<List<LocationEntity>> = locationDao.getChildLocations(parentId)

    override fun getLocationById(id: String): Flow<LocationEntity?> = locationDao.getLocationById(id)

    override suspend fun createLocation(inventoryId: String, parentId: String?, code: String, name: String, description: String) {
        val location = LocationEntity(
            inventoryId = inventoryId,
            parentId = parentId,
            code = code,
            name = name,
            description = description
        )
        locationDao.insertLocation(location)
    }

    override suspend fun updateLocation(location: LocationEntity) {
        locationDao.updateLocation(location.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteLocation(location: LocationEntity) {
        locationDao.deleteLocation(location)
    }

    override fun getItems(locationId: String): Flow<List<ItemEntity>> = itemDao.getItemsForLocation(locationId)

    override fun getItemById(id: String): Flow<ItemEntity?> = itemDao.getItemById(id)

    override suspend fun createItem(inventoryId: String, locationId: String, code: String, name: String, category: String, quantity: Int, notes: String) {
        val item = ItemEntity(
            inventoryId = inventoryId,
            locationId = locationId,
            code = code,
            name = name,
            category = category,
            quantity = quantity,
            notes = notes
        )
        itemDao.insertItem(item)
    }

    override suspend fun updateItem(item: ItemEntity) {
        itemDao.updateItem(item.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteItem(item: ItemEntity) {
        itemDao.deleteItem(item)
    }
}
