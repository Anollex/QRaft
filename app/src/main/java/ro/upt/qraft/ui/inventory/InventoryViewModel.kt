package ro.upt.qraft.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.repository.AuthRepository
import ro.upt.qraft.data.repository.InventoryRepository
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val inventories: StateFlow<List<InventoryEntity>> = inventoryRepository.getAllInventories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createInventory(name: String, prefix: String) {
        viewModelScope.launch {
            authRepository.currentUser.collect { userId ->
                if (userId != null) {
                    inventoryRepository.createInventory(name, userId, prefix)
                }
            }
        }
    }

    fun deleteInventory(inventory: InventoryEntity) {
        viewModelScope.launch {
            inventoryRepository.deleteInventory(inventory)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
