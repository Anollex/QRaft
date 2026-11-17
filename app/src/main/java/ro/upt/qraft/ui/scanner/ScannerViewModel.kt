package ro.upt.qraft.ui.scanner

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import ro.upt.qraft.data.repository.InventoryRepository
import javax.inject.Inject

sealed interface ScanTarget {
    data class Location(val inventoryId: String, val locationId: String) : ScanTarget
    data class Item(val itemId: String) : ScanTarget
    /** Cod QRaft valid, dar obiectul nu exista local (sters sau nepartajat cu acest cont). */
    data object NotFound : ScanTarget
    /** QR care nu e generat de QRaft. */
    data object Invalid : ScanTarget
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: InventoryRepository
) : ViewModel() {

    suspend fun resolve(rawValue: String): ScanTarget {
        // Format: qraft://location/<id> sau qraft://item/<id>
        val parts = rawValue.removePrefix(PREFIX).split("/")
        if (!rawValue.startsWith(PREFIX) || parts.size != 2 || parts[1].isBlank()) return ScanTarget.Invalid
        val (type, id) = parts

        return when (type) {
            "location" -> repository.getLocationById(id).first()
                ?.let { ScanTarget.Location(it.inventoryId, it.id) } ?: ScanTarget.NotFound
            "item" -> repository.getItemById(id).first()
                ?.let { ScanTarget.Item(it.id) } ?: ScanTarget.NotFound
            else -> ScanTarget.Invalid
        }
    }

    private companion object {
        const val PREFIX = "qraft://"
    }
}
