package ro.upt.qraft.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ro.upt.qraft.data.local.entity.InventoryEntity
import ro.upt.qraft.data.local.entity.InventoryMember
import ro.upt.qraft.ui.common.ConfirmDeleteDialog
import ro.upt.qraft.ui.inventory.RoleChip
import ro.upt.qraft.ui.inventory.roleLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorySettingsScreen(
    viewModel: InventorySettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onInventoryDeleted: () -> Unit
) {
    val inventory by viewModel.inventory.collectAsState()
    val isSharing by viewModel.isSharing.collectAsState()
    val deleted by viewModel.deleted.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Utilizatorul alege unde salveaza fisierul (Descarcari, Drive etc.); null = a renuntat.
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(viewModel::exportCsv)
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let(viewModel::exportQrLabels)
    }

    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(deleted) {
        if (deleted) onInventoryDeleted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setări și partajare") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val inv = inventory
        if (inv == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val isOwner = inv.isOwner(viewModel.currentUserId)

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { InventoryInfoCard(inv, isOwner, onRenameClick = { showRename = true }) }

            item {
                ExportCard(
                    isExporting = isExporting,
                    onExportCsv = { csvLauncher.launch(viewModel.suggestedFileName("csv")) },
                    onExportPdf = { pdfLauncher.launch(viewModel.suggestedFileName("pdf")) }
                )
            }

            if (isOwner) {
                item { ShareCard(isSharing = isSharing, onShare = viewModel::share) }
            }

            item {
                Text("Membri", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }
            items(inv.memberDetailList(), key = { it.uid }) { member ->
                MemberRow(
                    member = member,
                    isCurrentUser = member.uid == viewModel.currentUserId,
                    canManage = isOwner && member.role != InventoryMember.ROLE_OWNER,
                    onChangeRole = { viewModel.changeRole(member.uid, it) },
                    onRemove = { viewModel.removeMember(member.uid) }
                )
            }

            if (isOwner) {
                item {
                    OutlinedButton(
                        onClick = { showDelete = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Șterge inventarul")
                    }
                }
            }
        }

        if (showRename) {
            RenameDialog(
                currentName = inv.name,
                onDismiss = { showRename = false },
                onConfirm = {
                    viewModel.rename(it)
                    showRename = false
                }
            )
        }

        if (showDelete) {
            ConfirmDeleteDialog(
                title = "Ștergi inventarul?",
                message = "Inventarul „${inv.name}” va fi șters împreună cu toate locațiile și itemii, " +
                        "pentru toți membrii. Acțiunea nu poate fi anulată.",
                onConfirm = {
                    showDelete = false
                    viewModel.deleteInventory()
                },
                onDismiss = { showDelete = false }
            )
        }
    }
}

@Composable
private fun InventoryInfoCard(inventory: InventoryEntity, isOwner: Boolean, onRenameClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(inventory.name, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Prefix locații: ${inventory.prefix}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Stabilit la crearea inventarului; nu se schimbă la redenumire.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isOwner) {
                IconButton(onClick = onRenameClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Redenumește")
                }
            }
        }
    }
}

@Composable
private fun ExportCard(isExporting: Boolean, onExportCsv: () -> Unit, onExportPdf: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Export", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (isExporting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            }
            Text(
                "CSV: lista itemilor, pentru Excel / Google Sheets.\n" +
                        "PDF: etichete QR pe A4 (21 pe pagină) pentru toate locațiile și itemii.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onExportCsv, enabled = !isExporting, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Itemi (CSV)")
                }
                OutlinedButton(onClick = onExportPdf, enabled = !isExporting, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Etichete QR")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareCard(isSharing: Boolean, onShare: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(InventoryMember.ROLE_EDITOR) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Partajează", style = MaterialTheme.typography.titleMedium)
            Text(
                "Persoana trebuie să aibă deja un cont QRaft.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(InventoryMember.ROLE_EDITOR, InventoryMember.ROLE_VIEWER).forEach { option ->
                    FilterChip(
                        selected = role == option,
                        onClick = { role = option },
                        label = { Text(roleLabel(option)) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onShare(email, role)
                    email = ""
                },
                enabled = !isSharing && email.contains("@"),
                modifier = Modifier.align(Alignment.End)
            ) {
                if (isSharing) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Adaugă membru")
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: InventoryMember,
    isCurrentUser: Boolean,
    canManage: Boolean,
    onChangeRole: (String) -> Unit,
    onRemove: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    ListItem(
        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
        headlineContent = {
            Text(
                buildString {
                    append(member.email.ifBlank { "(email necunoscut)" })
                    if (isCurrentUser) append(" (tu)")
                }
            )
        },
        supportingContent = { RoleChip(member.role) },
        trailingContent = {
            if (canManage) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opțiuni membru")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        val other = if (member.role == InventoryMember.ROLE_VIEWER) InventoryMember.ROLE_EDITOR
                        else InventoryMember.ROLE_VIEWER
                        DropdownMenuItem(
                            text = { Text("Schimbă în: ${roleLabel(other)}") },
                            onClick = {
                                menuOpen = false
                                onChangeRole(other)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Elimină din inventar", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuOpen = false
                                onRemove()
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun RenameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redenumește inventarul") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nume") }, singleLine = true)
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Salvează") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Renunță") } }
    )
}
