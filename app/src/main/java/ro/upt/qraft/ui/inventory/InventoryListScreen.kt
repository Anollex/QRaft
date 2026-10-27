package ro.upt.qraft.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ro.upt.qraft.data.local.entity.InventoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    viewModel: InventoryViewModel = hiltViewModel(),
    onInventoryClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    val inventories by viewModel.inventories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventarele mele") },
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogoutClick()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adaugă Inventar")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(inventories) { inventory ->
                InventoryItemRow(
                    inventory = inventory,
                    onClick = { onInventoryClick(inventory.id) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddInventoryDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, prefix ->
                viewModel.createInventory(name, prefix)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun InventoryItemRow(inventory: InventoryEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = inventory.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "Prefix: ${inventory.prefix}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AddInventoryDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var prefix by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adaugă Inventar nou") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nume Inventar") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text("Prefix (ex: LAB, ACASA)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, prefix) },
                enabled = name.isNotBlank() && prefix.isNotBlank()
            ) {
                Text("Adaugă")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Renunță")
            }
        }
    )
}
