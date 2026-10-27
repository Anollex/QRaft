package ro.upt.qraft.ui.location

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity
import ro.upt.qraft.data.repository.InventoryRepository
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocationViewModel @Inject constructor(
    private val repository: InventoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Argumentele din NavHost
    val inventoryId: String? = savedStateHandle["inventoryId"]
    val parentLocationId: String? = savedStateHandle.get<String>("locationId")?.takeIf { it != "root" }

    val locations: StateFlow<List<LocationEntity>> = flow {
        if (parentLocationId != null) {
            emitAll(repository.getChildLocations(parentLocationId))
        } else if (inventoryId != null) {
            emitAll(repository.getRootLocations(inventoryId))
        } else {
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<ItemEntity>> = flow {
        if (parentLocationId != null) {
            emitAll(repository.getItems(parentLocationId))
        } else {
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createLocation(name: String, code: String, description: String) {
        viewModelScope.launch {
            if (inventoryId != null) {
                repository.createLocation(
                    inventoryId = inventoryId,
                    parentId = parentLocationId,
                    code = code,
                    name = name,
                    description = description
                )
            }
        }
    }

    fun createItem(name: String, code: String, category: String, quantity: Int, notes: String) {
        viewModelScope.launch {
            if (inventoryId != null && parentLocationId != null) {
                repository.createItem(
                    inventoryId = inventoryId,
                    locationId = parentLocationId,
                    code = code,
                    name = name,
                    category = category,
                    quantity = quantity,
                    notes = notes
                )
            }
        }
    }
}
