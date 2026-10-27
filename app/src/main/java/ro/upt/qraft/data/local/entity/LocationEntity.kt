package ro.upt.qraft.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "locations",
    foreignKeys = [
        ForeignKey(
            entity = InventoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["inventoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("inventoryId")]
)
data class LocationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val inventoryId: String,
    val parentId: String?,
    val code: String,
    val name: String,
    val description: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
