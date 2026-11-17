package ro.upt.qraft.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.local.entity.InventoryMember
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity

// Entitati Room <-> documente Firestore.
// pendingSync nu ajunge in cloud; ce vine din cloud este, prin definitie, sincronizat.

// members[] e folosit de query si de reguli; roles{} decide cine poate edita; memberEmails{} e pentru afisare.
internal fun InventoryEntity.toFirestore(): Map<String, Any?> {
    val details = memberDetailList()
    return mapOf(
        "name" to name,
        "ownerId" to ownerId,
        "prefix" to prefix,
        "members" to details.map { it.uid },
        "roles" to details.associate { it.uid to it.role },
        "memberEmails" to details.associate { it.uid to it.email },
        "updatedAt" to updatedAt
    )
}

internal fun LocationEntity.toFirestore(): Map<String, Any?> = mapOf(
    "inventoryId" to inventoryId,
    "parentId" to parentId,
    "code" to code,
    "name" to name,
    "description" to description,
    "updatedAt" to updatedAt
)

internal fun ItemEntity.toFirestore(): Map<String, Any?> = mapOf(
    "inventoryId" to inventoryId,
    "locationId" to locationId,
    "code" to code,
    "name" to name,
    "category" to category,
    "quantity" to quantity,
    "notes" to notes,
    "updatedAt" to updatedAt
)

internal fun DocumentSnapshot.toInventoryEntity(): InventoryEntity? {
    val name = getString("name") ?: return null
    val ownerId = getString("ownerId") ?: return null
    val members = (get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
    val roles = (get("roles") as? Map<*, *>).orEmpty()
    val emails = (get("memberEmails") as? Map<*, *>).orEmpty()
    val details = (listOf(ownerId) + members).distinct().map { uid ->
        InventoryMember(
            uid = uid,
            email = emails[uid] as? String ?: "",
            role = if (uid == ownerId) InventoryMember.ROLE_OWNER
            else roles[uid] as? String ?: InventoryMember.ROLE_EDITOR
        )
    }
    return InventoryEntity(
        id = id,
        name = name,
        ownerId = ownerId,
        prefix = getString("prefix") ?: "INV",
        updatedAt = getLong("updatedAt") ?: 0L,
        pendingSync = false
    ).withMembers(details)
}

internal fun DocumentSnapshot.toLocationEntity(inventoryId: String): LocationEntity? {
    val name = getString("name") ?: return null
    return LocationEntity(
        id = id,
        inventoryId = inventoryId,
        parentId = getString("parentId"),
        code = getString("code").orEmpty(),
        name = name,
        description = getString("description").orEmpty(),
        updatedAt = getLong("updatedAt") ?: 0L,
        pendingSync = false
    )
}

internal fun DocumentSnapshot.toItemEntity(inventoryId: String): ItemEntity? {
    val name = getString("name") ?: return null
    val locationId = getString("locationId") ?: return null
    return ItemEntity(
        id = id,
        inventoryId = inventoryId,
        locationId = locationId,
        code = getString("code").orEmpty(),
        name = name,
        category = getString("category").orEmpty(),
        quantity = getLong("quantity")?.toInt() ?: 1,
        notes = getString("notes").orEmpty(),
        updatedAt = getLong("updatedAt") ?: 0L,
        pendingSync = false
    )
}
