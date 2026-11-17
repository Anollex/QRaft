package ro.upt.qraft.ui.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.ui.common.ConfirmDeleteDialog
import ro.upt.qraft.ui.common.QrDialog
import ro.upt.qraft.ui.common.QrImage
import ro.upt.qraft.utils.itemQrContent
import ro.upt.qraft.ui.inventory.EmptyState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsScreen(
    viewModel: ItemDetailsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onEditClick: (ItemEntity) -> Unit,
    onLocationClick: (inventoryId: String, locationId: String) -> Unit
) {
    val item by viewModel.item.collectAsState()
    val location by viewModel.location.collectAsState()
    val canEdit by viewModel.canEdit.collectAsState()

    var showDelete by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.name ?: "Item") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
                    }
                },
                actions = {
                    val current = item
                    if (canEdit && current != null) {
                        IconButton(onClick = { onEditClick(current) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editează")
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Șterge")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val current = item
        if (current == null) {
            EmptyState("Itemul nu mai există sau nu ai acces la el.", Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QrImage(
                content = itemQrContent(current.id),
                modifier = Modifier
                    .size(180.dp)
                    .clickable { showQr = true }
            )
            Text(current.code, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)

            QuantityCard(
                quantity = current.quantity,
                canEdit = canEdit,
                onChange = viewModel::changeQuantity
            )

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    location?.let { loc ->
                        ListItem(
                            leadingContent = { Icon(Icons.Default.Place, contentDescription = null) },
                            overlineContent = { Text("Locație") },
                            headlineContent = { Text(loc.name) },
                            supportingContent = { Text(loc.code, fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.clickable { onLocationClick(loc.inventoryId, loc.id) }
                        )
                    }
                    DetailRow("Categorie", current.category.ifBlank { "—" })
                    DetailRow("Note", current.notes.ifBlank { "—" })
                    DetailRow(
                        "Ultima modificare",
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(current.updatedAt)) +
                                if (current.pendingSync) " (nesincronizat)" else ""
                    )
                }
            }
        }
    }

    val current = item
    if (showQr && current != null) {
        QrDialog(current.name, current.code, itemQrContent(current.id)) { showQr = false }
    }

    if (showDelete && current != null) {
        ConfirmDeleteDialog(
            title = "Ștergi itemul?",
            message = "„${current.name}” (${current.code}) va fi șters definitiv.",
            onConfirm = {
                showDelete = false
                viewModel.delete(onDeleted = onBackClick)
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun QuantityCard(quantity: Int, canEdit: Boolean, onChange: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cantitate", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (canEdit) {
                FilledTonalButton(onClick = { onChange(-1) }, enabled = quantity > 0) { Text("−") }
            }
            Text(
                "$quantity",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (canEdit) {
                FilledTonalButton(onClick = { onChange(1) }) { Text("+") }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    ListItem(
        overlineContent = { Text(label) },
        headlineContent = { Text(value) }
    )
}
