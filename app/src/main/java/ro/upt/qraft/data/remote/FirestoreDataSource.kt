package ro.upt.qraft.data.remote

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity
import javax.inject.Inject
import javax.inject.Singleton

/** Rezultatul unui snapshot Firestore. [fromCache] = datele nu sunt (inca) confirmate de server. */
data class RemoteSnapshot<T>(val data: List<T>, val fromCache: Boolean)

/**
 * Structura in Firestore:
 *   inventories/{inventoryId}                      (name, ownerId, prefix, members[], updatedAt)
 *   inventories/{inventoryId}/locations/{id}       (parentId, code, name, description, updatedAt)
 *   inventories/{inventoryId}/items/{id}           (locationId, code, name, category, quantity, notes, updatedAt)
 *
 * Scrierile NU sunt suspend: Firestore le pune in coada locala (persistenta) imediat si le trimite
 * cand exista conexiune. Ordinea apelurilor este pastrata, ceea ce conteaza pentru regulile de securitate
 * (un document din subcolectie are nevoie ca inventarul parinte sa existe).
 */
@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val inventories: CollectionReference get() = firestore.collection(INVENTORIES)
    private fun locations(inventoryId: String) = inventories.document(inventoryId).collection(LOCATIONS)
    private fun items(inventoryId: String) = inventories.document(inventoryId).collection(ITEMS)

    // ---------- Scriere ----------

    fun setInventory(inventory: InventoryEntity): Task<Void> =
        inventories.document(inventory.id).set(inventory.toFirestore())

    fun setLocation(location: LocationEntity): Task<Void> =
        locations(location.inventoryId).document(location.id).set(location.toFirestore())

    fun setItem(item: ItemEntity): Task<Void> =
        items(item.inventoryId).document(item.id).set(item.toFirestore())

    /**
     * Sterge in batch-uri documentele date. Daca [deleteInventory] e true, documentul inventarului
     * e sters ultimul (regulile verifica apartenenta prin documentul parinte).
     */
    fun deleteDocuments(
        inventoryId: String,
        locationIds: Collection<String>,
        itemIds: Collection<String>,
        deleteInventory: Boolean
    ): List<Task<Void>> {
        val refs = itemIds.map { items(inventoryId).document(it) } +
                locationIds.map { locations(inventoryId).document(it) } +
                listOfNotNull(if (deleteInventory) inventories.document(inventoryId) else null)

        return refs.chunked(MAX_BATCH_SIZE).map { chunk ->
            firestore.batch().apply { chunk.forEach { delete(it) } }.commit()
        }
    }

    // ---------- Utilizatori (pentru partajare dupa email) ----------

    /**
     * userEmails/{email} -> { uid }. ID-ul documentului e emailul, astfel incat regulile pot permite
     * doar citirea unui email cunoscut (get), nu si listarea tuturor utilizatorilor.
     */
    fun registerUserEmail(uid: String, email: String): Task<Void> =
        firestore.collection(USER_EMAILS).document(normalizeEmail(email)).set(mapOf("uid" to uid))

    /** Necesita conexiune la internet (citim de pe server, nu din cache). */
    suspend fun findUserIdByEmail(email: String): String? =
        firestore.collection(USER_EMAILS).document(normalizeEmail(email))
            .get(com.google.firebase.firestore.Source.SERVER).await()
            .getString("uid")

    // ---------- Citire (listener-e in timp real) ----------

    fun observeInventories(uid: String): Flow<RemoteSnapshot<InventoryEntity>> =
        inventories.whereArrayContains("members", uid).asFlow { it.toInventoryEntity() }

    fun observeLocations(inventoryId: String): Flow<RemoteSnapshot<LocationEntity>> =
        locations(inventoryId).asFlow { it.toLocationEntity(inventoryId) }

    fun observeItems(inventoryId: String): Flow<RemoteSnapshot<ItemEntity>> =
        items(inventoryId).asFlow { it.toItemEntity(inventoryId) }

    private fun <T> Query.asFlow(mapper: (DocumentSnapshot) -> T?): Flow<RemoteSnapshot<T>> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Listener Firestore oprit", error)
                close()
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(RemoteSnapshot(snapshot.documents.mapNotNull(mapper), snapshot.metadata.isFromCache))
            }
        }
        awaitClose { registration.remove() }
    }

    companion object {
        private const val TAG = "FirestoreDataSource"
        private const val INVENTORIES = "inventories"
        private const val LOCATIONS = "locations"
        private const val ITEMS = "items"
        private const val USER_EMAILS = "userEmails"

        fun normalizeEmail(email: String) = email.trim().lowercase()
        private const val MAX_BATCH_SIZE = 450 // limita Firestore e 500 operatii / batch
    }
}
