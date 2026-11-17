package ro.upt.qraft.ui.settings

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ro.upt.qraft.data.export.InventoryExporter
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.repository.AuthRepository
import ro.upt.qraft.data.repository.InventoryRepository
import javax.inject.Inject

@HiltViewModel
class InventorySettingsViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val exporter: InventoryExporter,
    authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val inventoryId: String = checkNotNull(savedStateHandle["inventoryId"])
    val currentUserId: String? = authRepository.currentUserId

    val inventory: StateFlow<InventoryEntity?> = repository.getInventoryById(inventoryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    /** Mesaje unice (snackbar). */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun suggestedFileName(extension: String): String =
        InventoryExporter.suggestedFileName(inventory.value?.name.orEmpty(), extension)

    fun exportCsv(uri: Uri) = export(uri) {
        val n = exporter.exportCsv(inventoryId, it)
        "Exportat CSV: " + if (n == 1) "1 item" else "$n itemi"
    }

    fun exportQrLabels(uri: Uri) = export(uri) {
        val n = exporter.exportQrLabels(inventoryId, it)
        "Exportat PDF: " + if (n == 1) "1 etichetă" else "$n etichete"
    }

    private fun export(uri: Uri, block: suspend (Uri) -> String) {
        if (_isExporting.value) return
        _isExporting.value = true
        viewModelScope.launch {
            runCatching { block(uri) }
                .onSuccess { _messages.emit(it) }
                .onFailure {
                    Log.e("InventorySettings", "Export failed", it)
                    _messages.emit("Exportul a eșuat: ${it.message ?: it.javaClass.simpleName}")
                }
            _isExporting.value = false
        }
    }

    fun rename(newName: String) = withInventory { repository.renameInventory(it, newName) }

    fun share(email: String, role: String) = withInventory { inv ->
        _isSharing.value = true
        repository.shareInventory(inv, email, role)
            .onSuccess { _messages.emit("Inventar partajat cu ${email.trim()}") }
            .onFailure { _messages.emit(it.message ?: "Partajarea a eșuat") }
        _isSharing.value = false
    }

    fun changeRole(uid: String, role: String) = withInventory { repository.changeMemberRole(it, uid, role) }

    fun removeMember(uid: String) = withInventory { repository.removeMember(it, uid) }

    fun deleteInventory() = withInventory {
        repository.deleteInventory(it)
        _deleted.value = true
    }

    private fun withInventory(block: suspend (InventoryEntity) -> Unit) {
        val current = inventory.value ?: return
        viewModelScope.launch { block(current) }
    }
}
