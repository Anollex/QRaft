package ro.upt.qraft.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.local.entity.InventoryMember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    viewModel: InventoryViewModel = hiltViewModel(),
    onInventoryClick: (String) -> Unit,
    onSettingsClick: (String) -> Unit,
    onLogoutClick: () -> Unit,
    onScanClick: () -> Unit
) {
    val inventories by viewModel.inventories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    // null = dialogul de deconectare inchis; altfel numarul de modificari nesincronizate
    var logoutPending by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventarele mele") },
                actions = {
                    IconButton(onClick = onScanClick) {
                        Icon(Icons.Default.Search, contentDescription = "Scanează")
                    }
                    IconButton(onClick = {
                        scope.launch { logoutPending = viewModel.countPendingChanges() }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Deconectare")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adaugă inventar")
            }
        }
    ) { padding ->
        if (inventories.isEmpty()) {
            EmptyState(
                text = "Nu ai încă niciun inventar.\nApasă + pentru a crea unul.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(inventories, key = { it.id }) { inventory ->
                    InventoryItemRow(
                        inventory = inventory,
                        role = inventory.roleOf(viewModel.currentUserId),
                        onClick = { onInventoryClick(inventory.id) },
                        onSettingsClick = { onSettingsClick(inventory.id) }
                    )
                }
            }
        }
    }

    logoutPending?.let { pending ->
        LogoutDialog(
            pendingChanges = pending,
            onConfirm = {
                logoutPending = null
                viewModel.logout()
                onLogoutClick()
            },
            onDismiss = { logoutPending = null }
        )
    }

    if (showAddDialog) {
        AddInventoryDialog(
            previewPrefix = viewModel::previewPrefix,
            onDismiss = { showAddDialog = false },
            onAdd = { name ->
                viewModel.createInventory(name)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun LogoutDialog(pendingChanges: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Te deconectezi?") },
        text = {
            Text(
                if (pendingChanges == 0) "Datele tale rămân în cloud și vor fi descărcate la următoarea autentificare."
                else "Ai $pendingChanges ${if (pendingChanges == 1) "modificare" else "modificări"} care nu au ajuns " +
                        "încă în cloud (probabil lipsește conexiunea la internet). Dacă te deconectezi acum, " +
                        "s-ar putea să le pierzi. Recomandat: conectează-te la internet și așteaptă sincronizarea."
            )
        },
        confirmButton = {
            if (pendingChanges == 0) {
                Button(onClick = onConfirm) { Text("Deconectare") }
            } else {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Deconectează-te oricum") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Renunță") } }
    )
}

@Composable
fun InventoryItemRow(
    inventory: InventoryEntity,
    role: String?,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(text = inventory.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Prefix: ${inventory.prefix}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    val membersCount = inventory.memberList().size
                    if (role != InventoryMember.ROLE_OWNER) {
                        Spacer(Modifier.width(8.dp))
                        RoleChip(role)
                    } else if (membersCount > 1) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "· partajat cu ${membersCount - 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Setări și partajare")
            }
        }
    }
}

@Composable
fun RoleChip(role: String?) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = roleLabel(role),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

fun roleLabel(role: String?): String = when (role) {
    InventoryMember.ROLE_OWNER -> "Proprietar"
    InventoryMember.ROLE_EDITOR -> "Editare"
    InventoryMember.ROLE_VIEWER -> "Vizualizare"
    else -> "—"
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun AddInventoryDialog(
    previewPrefix: (String) -> String,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inventar nou") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nume inventar") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (name.isBlank()) "Prefixul locațiilor = primele 3 consoane din nume."
                    else "Locațiile vor avea coduri de forma ${previewPrefix(name)}-000-001",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name) }, enabled = name.isNotBlank()) {
                Text("Creează")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Renunță") }
        }
    )
}
