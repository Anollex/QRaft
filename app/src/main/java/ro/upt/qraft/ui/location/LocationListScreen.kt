package ro.upt.qraft.ui.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity
import ro.upt.qraft.ui.common.ConfirmDeleteDialog
import ro.upt.qraft.ui.common.QrDialog
import ro.upt.qraft.ui.common.QrImage
import ro.upt.qraft.utils.itemQrContent
import ro.upt.qraft.utils.locationQrContent
import ro.upt.qraft.ui.inventory.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationListScreen(
    viewModel: LocationViewModel = hiltViewModel(),
    onLocationClick: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onAddLocation: (parentId: String?) -> Unit,
    /** null = itemul e creat din radacina inventarului, iar locatia se alege in formular. */
    onAddItem: (locationId: String?) -> Unit,
    onEditLocation: (locationId: String) -> Unit,
    onBackClick: () -> Unit,
    onScanClick: () -> Unit
) {
    val inventory by viewModel.inventory.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val items by viewModel.items.collectAsState()
    val canEdit by viewModel.canEdit.collectAsState()

    val isRoot = viewModel.parentLocationId == null
    var showDelete by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var fabMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isRoot) inventory?.name ?: "Locații" else currentLocation?.name ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
                    }
                },
                actions = {
                    IconButton(onClick = onScanClick) {
                        Icon(Icons.Default.Search, contentDescription = "Scanează")
                    }
                    if (!isRoot && canEdit) {
                        IconButton(onClick = { viewModel.parentLocationId?.let(onEditLocation) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editează locația")
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Șterge locația")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (canEdit) {
                // Fara locatii nu se poate crea un item, asa ca butonul duce direct la "Locatie noua".
                if (isRoot && locations.isEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { onAddLocation(null) },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Locație") }
                    )
                } else {
                    Box {
                        ExtendedFloatingActionButton(
                            onClick = { fabMenuOpen = true },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Adaugă") }
                        )
                        DropdownMenu(expanded = fabMenuOpen, onDismissRequest = { fabMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Item") },
                                leadingIcon = { Icon(Icons.Default.ShoppingCart, null) },
                                onClick = {
                                    fabMenuOpen = false
                                    onAddItem(viewModel.parentLocationId)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isRoot) "Locație" else "Sub-locație") },
                                leadingIcon = { Icon(Icons.Default.Place, null) },
                                onClick = {
                                    fabMenuOpen = false
                                    onAddLocation(viewModel.parentLocationId)
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        val location = currentLocation
        if (isRoot && locations.isEmpty()) {
            EmptyState(
                text = if (canEdit) "Inventarul nu are încă locații.\nAdaugă un raft, un sertar sau o cutie."
                else "Inventarul nu are încă locații.",
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            if (!isRoot && location != null) {
                item { LocationHeader(location, onQrClick = { showQr = true }) }
            }

            if (locations.isNotEmpty()) {
                item { SectionTitle(if (isRoot) "Locații" else "Sub-locații") }
                items(locations, key = { it.id }) { loc ->
                    LocationItemRow(loc, onClick = { onLocationClick(loc.id) })
                }
            }

            if (!isRoot) {
                item { SectionTitle("Itemi (${items.size})") }
                if (items.isEmpty()) {
                    item {
                        Text(
                            "Niciun item în această locație.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    if (canEdit) {
                        item {
                            OutlinedButton(
                                onClick = { onAddItem(viewModel.parentLocationId) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Adaugă item")
                            }
                        }
                    }
                }
                items(items, key = { it.id }) { itemEntity ->
                    ItemRow(itemEntity, onClick = { onItemClick(itemEntity.id) })
                }
            }
        }
    }

    val location = currentLocation
    if (showQr && location != null) {
        QrDialog(
            title = location.name,
            code = location.code,
            qrContent = locationQrContent(location.id),
            onDismiss = { showQr = false }
        )
    }

    if (showDelete && location != null) {
        ConfirmDeleteDialog(
            title = "Ștergi locația?",
            message = "„${location.name}” va fi ștearsă împreună cu toate sub-locațiile și itemii din ea.",
            onConfirm = {
                showDelete = false
                viewModel.deleteCurrentLocation(onDeleted = onBackClick)
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun LocationHeader(location: LocationEntity, onQrClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            QrImage(
                content = locationQrContent(location.id),
                modifier = Modifier
                    .size(96.dp)
                    .clickable(onClick = onQrClick)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(location.code, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                if (location.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(location.description, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Atinge codul QR pentru a-l mări",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun LocationItemRow(location: LocationEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QrImage(locationQrContent(location.id), Modifier.size(56.dp), sizePx = 168)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = location.name, style = MaterialTheme.typography.titleMedium)
                Text(text = location.code, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun ItemRow(item: ItemEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QrImage(itemQrContent(item.id), Modifier.size(56.dp), sizePx = 168)
            Spacer(modifier = Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                Text(text = item.code, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                if (item.category.isNotBlank()) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text("×${item.quantity}", style = MaterialTheme.typography.titleMedium)
        }
    }
}
