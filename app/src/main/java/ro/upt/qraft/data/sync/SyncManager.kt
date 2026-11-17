package ro.upt.qraft.data.sync

import android.util.Log
import androidx.room.withTransaction
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.tasks.await
import ro.upt.qraft.data.local.QRaftDatabase
import ro.upt.qraft.data.local.dao.InventoryDao
import ro.upt.qraft.data.local.dao.ItemDao
import ro.upt.qraft.data.local.dao.LocationDao
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity
import ro.upt.qraft.data.remote.FirestoreDataSource
import ro.upt.qraft.data.remote.RemoteSnapshot
import ro.upt.qraft.data.repository.AuthRepository
import ro.upt.qraft.di.ApplicationScope
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sincronizare Offline-First intre Room (sursa de adevar pentru UI) si Cloud Firestore.
 *
 * PUSH: fiecare modificare locala e scrisa in Room cu pendingSync = true, apoi trimisa in Firestore.
 *       Firestore pastreaza scrierea in coada locala pana exista internet; cand serverul confirma,
 *       randul e marcat sincronizat. La pornire se retrimit randurile ramase nesincronizate.
 *
 * PULL: cat timp un utilizator e logat, ascultam in timp real inventarele in care e membru si,
 *       pentru fiecare, locatiile si itemii. Fiecare snapshot e reconciliat cu Room:
 *         - randurile locale cu pendingSync = true nu sunt atinse (modificarea locala castiga
 *           pana ajunge pe server);
 *         - restul sunt aduse la starea din cloud (insert / update / delete).
 *       Stergerile se aplica doar din snapshot-uri confirmate de server (nu din cache).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SyncManager @Inject constructor(
    private val database: QRaftDatabase,
    private val inventoryDao: InventoryDao,
    private val locationDao: LocationDao,
    private val itemDao: ItemDao,
    private val remote: FirestoreDataSource,
    private val authRepository: AuthRepository,
    @ApplicationScope private val appScope: CoroutineScope
) {
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return

        appScope.launch {
            var hadUser = false
            authRepository.currentUser.distinctUntilChanged().collectLatest { uid ->
                if (uid == null) {
                    // Logout: datele apartin contului anterior, nu le lasam pe telefon.
                    if (hadUser) database.clearAllTables()
                    hadUser = false
                    return@collectLatest
                }
                hadUser = true
                try {
                    // Ca alti utilizatori sa ne poata gasi dupa email la partajare.
                    authRepository.currentUserEmail?.let { email ->
                        remote.registerUserEmail(uid, email).confirm("email utilizator") {}
                    }
                    pushAllPending()
                    syncInventories(uid)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Sincronizarea s-a oprit", e)
                }
            }
        }
    }

    // ===================== PUSH =====================

    fun pushInventory(inventory: InventoryEntity) =
        remote.setInventory(inventory).confirm("inventar ${inventory.id}") {
            inventoryDao.markSynced(inventory.id, inventory.updatedAt)
        }

    fun pushLocation(location: LocationEntity) =
        remote.setLocation(location).confirm("locatie ${location.id}") {
            locationDao.markSynced(location.id, location.updatedAt)
        }

    fun pushItem(item: ItemEntity) =
        remote.setItem(item).confirm("item ${item.id}") {
            itemDao.markSynced(item.id, item.updatedAt)
        }

    /** Trebuie apelat INAINTE de stergerea locala, ca un snapshot intermediar sa nu readuca randurile. */
    fun pushDeletion(
        inventoryId: String,
        locationIds: Collection<String>,
        itemIds: Collection<String>,
        deleteInventory: Boolean
    ) {
        remote.deleteDocuments(inventoryId, locationIds, itemIds, deleteInventory)
            .forEach { it.confirm("stergere in $inventoryId") {} }
    }

    private suspend fun pushAllPending() {
        // Ordinea conteaza: parintii inaintea copiilor.
        inventoryDao.getPendingInventories().forEach { pushInventory(it) }
        locationDao.getPendingLocations().forEach { pushLocation(it) }
        itemDao.getPendingItems().forEach { pushItem(it) }
    }

    /** Asteapta confirmarea serverului in fundal (poate dura oricat daca suntem offline). */
    private fun Task<Void>.confirm(what: String, onSuccess: suspend () -> Unit) {
        appScope.launch {
            try {
                await()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Sincronizare esuata pentru $what", e)
            }
        }
    }

    // ===================== PULL =====================

    private suspend fun syncInventories(uid: String) = supervisorScope {
        val scope = this
        val contentJobs = mutableMapOf<String, Job>()

        remote.observeInventories(uid).collect { snapshot ->
            val remoteIds = snapshot.data.map { it.id }.toSet()

            // Oprim intai listener-ele inventarelor disparute, apoi le stergem local.
            (contentJobs.keys - remoteIds).forEach { contentJobs.remove(it)?.cancelAndJoin() }

            applyInventories(snapshot)

            (remoteIds - contentJobs.keys).forEach { inventoryId ->
                contentJobs[inventoryId] = scope.launch { syncInventoryContent(inventoryId) }
            }
        }
    }

    private suspend fun syncInventoryContent(inventoryId: String) {
        combine(
            remote.observeLocations(inventoryId),
            remote.observeItems(inventoryId)
        ) { locations, items -> locations to items }
            .collect { (locations, items) ->
                try {
                    applyInventoryContent(inventoryId, locations, items)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Nu am putut aplica datele pentru inventarul $inventoryId", e)
                }
            }
    }

    private suspend fun applyInventories(snapshot: RemoteSnapshot<InventoryEntity>) = database.withTransaction {
        val local = inventoryDao.getAllInventoriesOnce().associateBy { it.id }

        val toUpsert = snapshot.data.filter { it.shouldReplace(local[it.id], InventoryEntity::pendingSync) }
        if (toUpsert.isNotEmpty()) inventoryDao.upsertInventories(toUpsert)

        if (!snapshot.fromCache) {
            val remoteIds = snapshot.data.map { it.id }.toSet()
            val toDelete = local.values.filter { it.id !in remoteIds && !it.pendingSync }.map { it.id }
            if (toDelete.isNotEmpty()) inventoryDao.deleteInventoriesByIds(toDelete) // CASCADE pe copii
        }
    }

    private suspend fun applyInventoryContent(
        inventoryId: String,
        locations: RemoteSnapshot<LocationEntity>,
        items: RemoteSnapshot<ItemEntity>
    ) = database.withTransaction {
        if (inventoryDao.getInventoryOnce(inventoryId) == null) return@withTransaction
        val canDelete = !locations.fromCache && !items.fromCache

        // --- Locatii ---
        val localLocations = locationDao.getLocationsForInventoryOnce(inventoryId).associateBy { it.id }
        val locationUpserts = locations.data.filter { it.shouldReplace(localLocations[it.id], LocationEntity::pendingSync) }
        if (locationUpserts.isNotEmpty()) locationDao.upsertLocations(locationUpserts)

        if (canDelete) {
            val remoteIds = locations.data.map { it.id }.toSet()
            val toDelete = localLocations.values.filter { it.id !in remoteIds && !it.pendingSync }.map { it.id }
            if (toDelete.isNotEmpty()) locationDao.deleteLocationsByIds(toDelete) // CASCADE pe itemi
        }

        // --- Itemi (doar cei a caror locatie exista local, altfel ar pica cheia straina) ---
        val existingLocationIds = locationDao.getLocationIdsForInventory(inventoryId).toSet()
        val localItems = itemDao.getItemsForInventoryOnce(inventoryId).associateBy { it.id }
        val itemUpserts = items.data.filter {
            it.locationId in existingLocationIds && it.shouldReplace(localItems[it.id], ItemEntity::pendingSync)
        }
        if (itemUpserts.isNotEmpty()) itemDao.upsertItems(itemUpserts)

        if (canDelete) {
            val remoteIds = items.data.map { it.id }.toSet()
            val toDelete = localItems.values.filter { it.id !in remoteIds && !it.pendingSync }.map { it.id }
            if (toDelete.isNotEmpty()) itemDao.deleteItemsByIds(toDelete)
        }
    }

    /** Versiunea din cloud se aplica daca randul lipseste local, sau exista, e sincronizat si difera. */
    private inline fun <T> T.shouldReplace(local: T?, isPending: (T) -> Boolean): Boolean =
        local == null || (!isPending(local) && local != this)

    companion object {
        private const val TAG = "SyncManager"
    }
}
