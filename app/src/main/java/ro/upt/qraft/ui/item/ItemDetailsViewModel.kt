package ro.upt.qraft.ui.item

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity
import ro.upt.qraft.data.repository.AuthRepository
import ro.upt.qraft.data.repository.InventoryRepository
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemDetailsViewModel @Inject constructor(
    private val repository: InventoryRepository,
    authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle["itemId"])
    private val currentUserId = authRepository.currentUserId

    val item: StateFlow<ItemEntity?> = repository.getItemById(itemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val location: StateFlow<LocationEntity?> = item
        .map { it?.locationId }
        .distinctUntilChanged()
        .flatMapLatest { id -> id?.let { repository.getLocationById(it) } ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val canEdit: StateFlow<Boolean> = item
        .map { it?.inventoryId }
        .distinctUntilChanged()
        .flatMapLatest { id -> id?.let { repository.getInventoryById(it) } ?: flowOf(null) }
        .map { it?.canEdit(currentUserId) == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun changeQuantity(delta: Int) {
        val current = item.value ?: return
        val newQuantity = (current.quantity + delta).coerceAtLeast(0)
        if (newQuantity == current.quantity) return
        viewModelScope.launch { repository.updateItem(current.copy(quantity = newQuantity)) }
    }

    fun delete(onDeleted: () -> Unit) {
        val current = item.value ?: return
        viewModelScope.launch {
            repository.deleteItem(current)
            onDeleted()
        }
    }
}
