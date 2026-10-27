package ro.upt.qraft.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "inventories")
data class InventoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ownerId: String,
    val prefix: String = "INV",
    val members: String = "", // Pentru simplitate, pastram o lista separata prin virgula sau JSON
    val updatedAt: Long = System.currentTimeMillis()
)
