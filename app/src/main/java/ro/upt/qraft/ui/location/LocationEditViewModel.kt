package ro.upt.qraft.ui.location

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ro.upt.qraft.data.local.entity.LocationEntity
import ro.upt.qraft.data.repository.InventoryRepository
import javax.inject.Inject

data class LocationForm(
    val name: String = "",
    val description: String = "",
    /** Codul existent (la editare); la creare e generat automat. */
    val code: String? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class LocationEditViewModel @Inject constructor(
    private val repository: InventoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val inventoryId: String = checkNotNull(savedStateHandle["inventoryId"])
    private val parentId: String? = savedStateHandle.get<String>("parentId")?.takeIf { it.isNotEmpty() }
    private val locationId: String? = savedStateHandle.get<String>("locationId")?.takeIf { it.isNotEmpty() }
    val isEditing = locationId != null

    private var original: LocationEntity? = null

    private val _form = MutableStateFlow(LocationForm(isLoading = isEditing))
    val form: StateFlow<LocationForm> = _form.asStateFlow()

    init {
        if (locationId != null) {
            viewModelScope.launch {
                val location = repository.getLocationById(locationId).first()
                original = location
                _form.value = LocationForm(
                    name = location?.name.orEmpty(),
                    description = location?.description.orEmpty(),
                    code = location?.code
                )
            }
        }
    }

    fun onNameChange(value: String) { _form.value = _form.value.copy(name = value) }
    fun onDescriptionChange(value: String) { _form.value = _form.value.copy(description = value) }

    fun save() {
        val f = _form.value
        if (f.name.isBlank() || f.isLoading) return
        _form.value = f.copy(isLoading = true) // previne dubla salvare la doua apasari rapide
        viewModelScope.launch {
            val existing = original
            if (existing != null) {
                repository.updateLocation(existing.copy(name = f.name.trim(), description = f.description.trim()))
            } else {
                repository.createLocation(inventoryId, parentId, f.name, f.description)
            }
            _form.value = _form.value.copy(isSaved = true)
        }
    }
}
