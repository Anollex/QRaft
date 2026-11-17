package ro.upt.qraft.data.repository

import kotlinx.coroutines.flow.Flow
import ro.upt.qraft.data.local.dao.InventoryDao
import ro.upt.qraft.data.local.dao.ItemDao
import ro.upt.qraft.data.local.dao.LocationDao
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.local.entity.InventoryMember
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity
import ro.upt.qraft.data.remote.FirestoreDataSource
import ro.upt.qraft.data.sync.SyncManager
import ro.upt.qraft.utils.InventoryCodes
import javax.inject.Inject

class InventoryRepositoryImpl @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val locationDao: LocationDao,
    private val itemDao: ItemDao,
    private val syncManager: SyncManager,
    private val remote: FirestoreDataSource
) : InventoryRepository {

    // ===================== Inventare =====================

    override fun getAllInventories(): Flow<List<InventoryEntity>> = inventoryDao.getAllInventories()

    override fun getInventoryById(id: String): Flow<InventoryEntity?> = inventoryDao.getInventoryById(id)

    override suspend fun createInventory(name: String, ownerId: String, ownerEmail: String) {
        val inventory = InventoryEntity(
            name = name.trim(),
            ownerId = ownerId,
            // Prefixul se fixeaza la creare: daca inventarul e redenumit, codurile locatiilor raman consistente.
            prefix = InventoryCodes.locationPrefix(name)
        ).withMembers(listOf(InventoryMember(ownerId, ownerEmail, InventoryMember.ROLE_OWNER)))

        inventoryDao.insertInventory(inventory)
        syncManager.pushInventory(inventory)
    }

    override suspend fun renameInventory(inventory: InventoryEntity, newName: String) {
        saveInventory(inventory.copy(name = newName.trim()))
    }

    override suspend fun deleteInventory(inventory: InventoryEntity) {
        // In Firestore subcolectiile nu se sterg odata cu parintele, deci le stergem explicit.
        val locationIds = locationDao.getLocationIdsForInventory(inventory.id)
        val itemIds = itemDao.getItemsForInventoryOnce(inventory.id).map { it.id }
        syncManager.pushDeletion(inventory.id, locationIds, itemIds, deleteInventory = true)
        inventoryDao.deleteInventory(inventory) // CASCADE local pe locatii si itemi
    }

    override suspend fun countPendingChanges(): Int =
        inventoryDao.getPendingInventories().size +
                locationDao.getPendingLocations().size +
                itemDao.getPendingItems().size

    override suspend fun shareInventory(inventory: InventoryEntity, email: String, role: String): Result<Unit> {
        val normalized = FirestoreDataSource.normalizeEmail(email)
        val uid = try {
            remote.findUserIdByEmail(normalized)
        } catch (e: Exception) {
            return Result.failure(IllegalStateException("Nu s-a putut verifica emailul. Este nevoie de conexiune la internet."))
        } ?: return Result.failure(IllegalArgumentException("Nu există niciun cont QRaft cu emailul $normalized."))

        if (uid == inventory.ownerId) {
            return Result.failure(IllegalArgumentException("Acesta este proprietarul inventarului."))
        }

        val others = inventory.memberDetailList().filter { it.uid != uid }
        saveInventory(inventory.withMembers(others + InventoryMember(uid, normalized, role)))
        return Result.success(Unit)
    }

    override suspend fun changeMemberRole(inventory: InventoryEntity, uid: String, role: String) {
        if (uid == inventory.ownerId) return
        val updated = inventory.memberDetailList().map { if (it.uid == uid) it.copy(role = role) else it }
        saveInventory(inventory.withMembers(updated))
    }

    override suspend fun removeMember(inventory: InventoryEntity, uid: String) {
        if (uid == inventory.ownerId) return
        saveInventory(inventory.withMembers(inventory.memberDetailList().filter { it.uid != uid }))
    }

    private suspend fun saveInventory(inventory: InventoryEntity) {
        val updated = inventory.copy(updatedAt = System.currentTimeMillis(), pendingSync = true)
        inventoryDao.updateInventory(updated)
        syncManager.pushInventory(updated)
    }

    // ===================== Locatii =====================

    override fun getRootLocations(inventoryId: String): Flow<List<LocationEntity>> = locationDao.getRootLocationsForInventory(inventoryId)

    override fun getAllLocations(inventoryId: String): Flow<List<LocationEntity>> = locationDao.getLocationsForInventory(inventoryId)

    override fun getChildLocations(parentId: String): Flow<List<LocationEntity>> = locationDao.getChildLocations(parentId)

    override fun getLocationById(id: String): Flow<LocationEntity?> = locationDao.getLocationById(id)

    override suspend fun createLocation(inventoryId: String, parentId: String?, name: String, description: String) {
        val inventory = inventoryDao.getInventoryOnce(inventoryId) ?: return

        // <primele 3 consoane>-<3 cifre>-<3 cifre>; refolosim primul numar liber din inventar
        val existingCodes = locationDao.getLocationsForInventoryOnce(inventoryId).map { it.code }
        val number = InventoryCodes.firstFreeLocationNumber(existingCodes)

        val location = LocationEntity(
            inventoryId = inventoryId,
            parentId = parentId,
            code = InventoryCodes.locationCode(inventory.prefix, number),
            name = name.trim(),
            description = description.trim()
        )
        locationDao.insertLocation(location)
        syncManager.pushLocation(location)
    }

    override suspend fun updateLocation(location: LocationEntity) {
        val updated = location.copy(updatedAt = System.currentTimeMillis(), pendingSync = true)
        locationDao.updateLocation(updated)
        syncManager.pushLocation(updated)
    }

    override suspend fun deleteLocation(location: LocationEntity) {
        // Sterge si sub-locatiile (parentId nu are cheie straina, deci nu exista CASCADE pentru ele)
        val all = locationDao.getLocationsForInventoryOnce(location.inventoryId)
        val locationIds = collectDescendants(location.id, all)
        val itemIds = itemDao.getItemIdsForLocations(locationIds)
        syncManager.pushDeletion(location.inventoryId, locationIds, itemIds, deleteInventory = false)
        locationDao.deleteLocationsByIds(locationIds) // CASCADE local pe itemi
    }

    /** Locatia data + toate sub-locatiile ei, pe orice nivel. */
    private fun collectDescendants(rootId: String, all: List<LocationEntity>): List<String> {
        val childrenByParent = all.groupBy { it.parentId }
        val result = mutableListOf<String>()
        val queue = ArrayDeque(listOf(rootId))
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            result += id
            childrenByParent[id]?.forEach { queue.addLast(it.id) }
        }
        return result
    }

    // ===================== Itemi =====================

    override fun getItems(locationId: String): Flow<List<ItemEntity>> = itemDao.getItemsForLocation(locationId)

    override fun getItemById(id: String): Flow<ItemEntity?> = itemDao.getItemById(id)

    override suspend fun createItem(inventoryId: String, locationId: String, name: String, category: String, quantity: Int, notes: String) {
        val item = ItemEntity(
            inventoryId = inventoryId,
            locationId = locationId,
            code = generateUniqueItemCode(inventoryId),
            name = name.trim(),
            category = category.trim(),
            quantity = quantity,
            notes = notes.trim()
        )
        itemDao.insertItem(item)
        syncManager.pushItem(item)
    }

    override suspend fun updateItem(item: ItemEntity) {
        val updated = item.copy(updatedAt = System.currentTimeMillis(), pendingSync = true)
        itemDao.updateItem(updated)
        syncManager.pushItem(updated)
    }

    override suspend fun deleteItem(item: ItemEntity) {
        syncManager.pushDeletion(item.inventoryId, emptyList(), listOf(item.id), deleteInventory = false)
        itemDao.deleteItem(item)
    }

    /** 4 grupe de 3 cifre, verificat sa nu mai existe in acelasi inventar. */
    private suspend fun generateUniqueItemCode(inventoryId: String): String {
        while (true) {
            val code = InventoryCodes.randomItemCode()
            if (!itemDao.codeExists(inventoryId, code)) return code
        }
    }
}
