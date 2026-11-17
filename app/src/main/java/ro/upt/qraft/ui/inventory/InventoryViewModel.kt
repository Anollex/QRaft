package ro.upt.qraft.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.repository.AuthRepository
import ro.upt.qraft.data.repository.InventoryRepository
import ro.upt.qraft.utils.InventoryCodes
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUserId: String? get() = authRepository.currentUserId

    val inventories: StateFlow<List<InventoryEntity>> = inventoryRepository.getAllInventories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun previewPrefix(name: String): String = InventoryCodes.locationPrefix(name)

    fun createInventory(name: String) {
        // Citim utilizatorul o singura data (nu collect: altfel fiecare schimbare de auth ar crea alt inventar)
        val userId = authRepository.currentUserId ?: return
        val email = authRepository.currentUserEmail.orEmpty()
        viewModelScope.launch {
            inventoryRepository.createInventory(name, userId, email)
        }
    }

    /** Folosit in dialogul de deconectare: la logout baza locala este golita. */
    suspend fun countPendingChanges(): Int = inventoryRepository.countPendingChanges()

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
