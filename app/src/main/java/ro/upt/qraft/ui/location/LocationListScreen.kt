package ro.upt.qraft.ui.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity
import ro.upt.qraft.utils.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationListScreen(
    viewModel: LocationViewModel = hiltViewModel(),
    onLocationClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onScanClick: () -> Unit
) {
    val locations by viewModel.locations.collectAsState()
    val items by viewModel.items.collectAsState()

    var showAddLocationDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    val isRoot = viewModel.parentLocationId == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRoot) "Locații" else "Conținut Locație") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Înapoi")
                    }
                },
                actions = {
                    IconButton(onClick = onScanClick) {
                        Icon(androidx.compose.material.icons.Icons.Default.Search, contentDescription = "Scan")
                    }
                }
            )
        },
        floatingActionButton = {
            Column {
                if (!isRoot) {
                    FloatingActionButton(
                        onClick = { showAddItemDialog = true },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Adaugă Item")
                    }
                }
                FloatingActionButton(onClick = { showAddLocationDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Adaugă Locație")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (locations.isNotEmpty()) {
                item { Text("Locații:", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp)) }
                items(locations) { location ->
                    LocationItemRow(location, onClick = { onLocationClick(location.id) })
                }
            }

            if (items.isNotEmpty()) {
                item { Text("Itemi:", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp)) }
                items(items) { itemEntity ->
                    ItemRow(itemEntity)
                }
            }
        }
    }

    if (showAddLocationDialog) {
        AddLocationDialog(
            onDismiss = { showAddLocationDialog = false },
            onAdd = { name ->
                viewModel.createLocation(name, "")
                showAddLocationDialog = false
            }
        )
    }

    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onAdd = { name, qty ->
                viewModel.createItem(name, "", qty, "")
                showAddItemDialog = false
            }
        )
    }
}

@Composable
fun LocationItemRow(location: LocationEntity, onClick: () -> Unit) {
    val qrBitmap = QrCodeGenerator.generateQrBitmap("qraft://location/${location.id}", 150)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            qrBitmap?.let {
                androidx.compose.foundation.Image(
                    bitmap = it,
                    contentDescription = "QR Code",
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column {
                Text(text = location.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "Cod: ${location.code}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ItemRow(item: ItemEntity) {
    val qrBitmap = QrCodeGenerator.generateQrBitmap("qraft://item/${item.id}", 150)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            qrBitmap?.let {
                androidx.compose.foundation.Image(
                    bitmap = it,
                    contentDescription = "QR Code",
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "Cod: ${item.code} | Cantitate: ${item.quantity}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun AddLocationDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adaugă Locație") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nume Locație") })
                Spacer(modifier = Modifier.height(8.dp))
                Text("Codul va fi generat automat.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name) }, enabled = name.isNotBlank()) {
                Text("Adaugă")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Renunță") } }
    )
}

@Composable
fun AddItemDialog(onDismiss: () -> Unit, onAdd: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adaugă Item") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nume Item") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Cantitate") })
                Spacer(modifier = Modifier.height(8.dp))
                Text("Codul de 12 cifre va fi generat automat.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, quantity.toIntOrNull() ?: 1) }, enabled = name.isNotBlank()) {
                Text("Adaugă")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Renunță") } }
    )
}
