package ro.upt.qraft.ui.item

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity
import ro.upt.qraft.data.repository.InventoryRepository
import javax.inject.Inject

data class ItemForm(
    val name: String = "",
    val category: String = "",
    val quantity: String = "1",
    val notes: String = "",
    val locationId: String = "",
    /** Codul existent (la editare); la creare e generat automat. */
    val code: String? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false
) {
    val quantityValue: Int? get() = quantity.toIntOrNull()?.takeIf { it >= 0 }
    val isValid: Boolean get() = name.isNotBlank() && quantityValue != null && locationId.isNotEmpty()
}

@HiltViewModel
class ItemEditViewModel @Inject constructor(
    private val repository: InventoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val inventoryId: String = checkNotNull(savedStateHandle["inventoryId"])
    /** Gol cand itemul e creat din ecranul principal al inventarului; locatia se alege din formular. */
    private val initialLocationId: String = savedStateHandle.get<String>("locationId").orEmpty()
    private val itemId: String? = savedStateHandle.get<String>("itemId")?.takeIf { it.isNotEmpty() }
    val isEditing = itemId != null

    private var original: ItemEntity? = null

    private val _form = MutableStateFlow(ItemForm(locationId = initialLocationId, isLoading = isEditing))
    val form: StateFlow<ItemForm> = _form.asStateFlow()

    /** Toate locatiile inventarului, pentru mutarea itemului. */
    val locations: StateFlow<List<LocationEntity>> = repository.getAllLocations(inventoryId)
        .map { list -> list.sortedBy { it.code } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (itemId != null) {
            viewModelScope.launch {
                val item = repository.getItemById(itemId).first()
                original = item
                if (item != null) {
                    _form.value = ItemForm(
                        name = item.name,
                        category = item.category,
                        quantity = item.quantity.toString(),
                        notes = item.notes,
                        locationId = item.locationId,
                        code = item.code
                    )
                } else {
                    _form.value = _form.value.copy(isLoading = false)
                }
            }
        }
    }

    fun update(transform: (ItemForm) -> ItemForm) {
        _form.value = transform(_form.value)
    }

    fun save() {
        val f = _form.value
        val quantity = f.quantityValue ?: return
        if (!f.isValid || f.isLoading) return
        _form.value = f.copy(isLoading = true) // previne dubla salvare

        viewModelScope.launch {
            val existing = original
            if (existing != null) {
                repository.updateItem(
                    existing.copy(
                        name = f.name.trim(),
                        category = f.category.trim(),
                        quantity = quantity,
                        notes = f.notes.trim(),
                        locationId = f.locationId
                    )
                )
            } else {
                repository.createItem(inventoryId, f.locationId, f.name, f.category, quantity, f.notes)
            }
            _form.value = _form.value.copy(isSaved = true)
        }
    }
}
