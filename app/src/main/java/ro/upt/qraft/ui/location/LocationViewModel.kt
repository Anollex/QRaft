package ro.upt.qraft.ui.location

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity
import ro.upt.qraft.data.repository.AuthRepository
import ro.upt.qraft.data.repository.InventoryRepository
import javax.inject.Inject

/**
 * Ecranul de locatii. Cu locationId = "root" afiseaza locatiile de nivel 0 ale inventarului;
 * altfel este ecranul de detalii al unei locatii (sub-locatii + itemi + cod QR).
 */
@HiltViewModel
class LocationViewModel @Inject constructor(
    private val repository: InventoryRepository,
    authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val inventoryId: String = checkNotNull(savedStateHandle["inventoryId"])
    val parentLocationId: String? = savedStateHandle.get<String>("locationId")?.takeIf { it != "root" }
    private val currentUserId = authRepository.currentUserId

    val inventory: StateFlow<InventoryEntity?> = repository.getInventoryById(inventoryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val canEdit: StateFlow<Boolean> = inventory.map { it?.canEdit(currentUserId) == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentLocation: StateFlow<LocationEntity?> =
        (parentLocationId?.let { repository.getLocationById(it) } ?: flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val locations: StateFlow<List<LocationEntity>> =
        (parentLocationId?.let { repository.getChildLocations(it) } ?: repository.getRootLocations(inventoryId))
            .map { list -> list.sortedBy { it.code } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<ItemEntity>> =
        (parentLocationId?.let { repository.getItems(it) } ?: flowOf(emptyList()))
            .map { list -> list.sortedBy { it.name.lowercase() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteCurrentLocation(onDeleted: () -> Unit) {
        val location = currentLocation.value ?: return
        viewModelScope.launch {
            repository.deleteLocation(location)
            onDeleted()
        }
    }
}
