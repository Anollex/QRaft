package ro.upt.qraft.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONObject
import java.util.UUID

@Entity(tableName = "inventories")
data class InventoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ownerId: String,
    /** Primele 3 consoane din numele inventarului, calculate la creare. Folosit in codurile locatiilor. */
    val prefix: String = "INV",
    val members: String = "", // UID-uri separate prin virgula (in Firestore e lista)
    val updatedAt: Long = System.currentTimeMillis(),
    // true = modificare locala neconfirmata inca de server
    @ColumnInfo(defaultValue = "1") val pendingSync: Boolean = true,
    /** JSON {uid: {"email": ..., "role": ...}} - vezi [InventoryMember]. */
    val memberDetails: String = ""
) {
    fun memberList(): List<String> =
        (members.split(",") + ownerId).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    /** Toti membrii, proprietarul primul. */
    fun memberDetailList(): List<InventoryMember> {
        val parsed = decodeMembers(memberDetails)
        val owner = parsed[ownerId]?.copy(role = InventoryMember.ROLE_OWNER)
            ?: InventoryMember(ownerId, "", InventoryMember.ROLE_OWNER)
        val others = memberList()
            .filter { it != ownerId }
            .map { uid -> parsed[uid] ?: InventoryMember(uid, "", InventoryMember.ROLE_EDITOR) }
        return listOf(owner) + others
    }

    fun roleOf(uid: String?): String? = when {
        uid == null -> null
        uid == ownerId -> InventoryMember.ROLE_OWNER
        uid !in memberList() -> null
        else -> decodeMembers(memberDetails)[uid]?.role ?: InventoryMember.ROLE_EDITOR
    }

    fun isOwner(uid: String?): Boolean = uid != null && uid == ownerId

    fun canEdit(uid: String?): Boolean =
        roleOf(uid).let { it == InventoryMember.ROLE_OWNER || it == InventoryMember.ROLE_EDITOR }

    /** Copie cu lista de membri inlocuita (pastreaza `members` si `memberDetails` consistente). */
    fun withMembers(list: List<InventoryMember>): InventoryEntity = copy(
        members = list.map { it.uid }.distinct().joinToString(","),
        memberDetails = encodeMembers(list)
    )

    companion object {
        fun encodeMembers(list: List<InventoryMember>): String {
            val json = JSONObject()
            list.forEach { json.put(it.uid, JSONObject().put("email", it.email).put("role", it.role)) }
            return json.toString()
        }

        fun decodeMembers(raw: String): Map<String, InventoryMember> {
            if (raw.isBlank()) return emptyMap()
            return try {
                val json = JSONObject(raw)
                json.keys().asSequence().associateWith { uid ->
                    val obj = json.optJSONObject(uid)
                    InventoryMember(
                        uid = uid,
                        email = obj?.optString("email").orEmpty(),
                        role = obj?.optString("role")?.takeIf { it.isNotEmpty() } ?: InventoryMember.ROLE_EDITOR
                    )
                }
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
}

data class InventoryMember(
    val uid: String,
    val email: String,
    val role: String
) {
    companion object {
        const val ROLE_OWNER = "owner"
        const val ROLE_EDITOR = "editor"
        const val ROLE_VIEWER = "viewer"
    }
}
