package ro.upt.qraft.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ro.upt.qraft.data.local.dao.InventoryDao
import ro.upt.qraft.data.local.dao.ItemDao
import ro.upt.qraft.data.local.dao.LocationDao
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

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
        // Obținem inventarul pentru prefix
        val inventory = inventoryDao.getInventoryById(inventoryId).first() ?: return
        
        // Extragem prefixul din numele inventarului
        val prefix = generateLocationPrefix(inventory.name)
        
        // Găsim primul număr disponibil (folosim rootLocations momentan pt simplicitate)
        // Mai bine sa luam Toate locatiile din acel inventar. 
        // Va trebui sa adaugam functia respectiva daca vrem exactitatea 100%. Momentan folosim lista pe care o avem de la Dao.
        val existingLocations = locationDao.getLocationsForInventory(inventoryId).first()
        val nextNumber = findFirstAvailableLocationNumber(existingLocations)
        
        val generatedCode = String.format(Locale.US, "%s-%03d-%03d", prefix, nextNumber / 1000, nextNumber % 1000)

        val location = LocationEntity(
            inventoryId = inventoryId,
            parentId = parentId,
            code = generatedCode,
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
        // Generam un cod aleator unic format din 4 grupuri de 3 cifre
        var generatedCode = ""
        var isUnique = false
        
        while (!isUnique) {
            val p1 = Random.nextInt(100, 1000)
            val p2 = Random.nextInt(100, 1000)
            val p3 = Random.nextInt(100, 1000)
            val p4 = Random.nextInt(100, 1000)
            generatedCode = "$p1-$p2-$p3-$p4"
            
            // Simulam verificare unicitate
            isUnique = true 
        }

        val item = ItemEntity(
            inventoryId = inventoryId,
            locationId = locationId,
            code = generatedCode,
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
    
    // Utilitare interne pentru generare coduri
    
    private fun generateLocationPrefix(inventoryName: String): String {
        val consonants = inventoryName.lowercase(Locale.ROOT).filter { it in "bcdfghjklmnpqrstvwxyz" }
        return if (consonants.length >= 3) {
            consonants.take(3)
        } else {
            inventoryName.lowercase(Locale.ROOT).take(3).padEnd(3, 'x')
        }
    }
    
    private fun findFirstAvailableLocationNumber(locations: List<LocationEntity>): Int {
        if (locations.isEmpty()) return 1
        
        val usedNumbers = locations.mapNotNull { loc ->
            val parts = loc.code.split("-")
            if (parts.size == 3) {
                val numPart = parts[1] + parts[2]
                numPart.toIntOrNull()
            } else {
                null
            }
        }.toSet()
        
        val maxUsed = usedNumbers.maxOrNull() ?: 0
        for (i in 1..maxUsed + 1) {
            if (i !in usedNumbers) {
                return i
            }
        }
        return 1
    }
}
