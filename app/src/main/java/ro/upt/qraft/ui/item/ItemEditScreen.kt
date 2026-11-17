package ro.upt.qraft.ui.item

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ro.upt.qraft.data.local.entity.LocationEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(
    viewModel: ItemEditViewModel = hiltViewModel(),
    onCancel: () -> Unit,
    /** Primeste locatia in care a fost salvat itemul. */
    onSaved: (locationId: String) -> Unit
) {
    val form by viewModel.form.collectAsState()
    val locations by viewModel.locations.collectAsState()

    LaunchedEffect(form.isSaved) {
        if (form.isSaved) onSaved(form.locationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Editează item" else "Item nou") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Renunță")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = form.isValid && !form.isLoading) {
                        Text("Salvează")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = form.code ?: "Generat automat la salvare",
                onValueChange = {},
                label = { Text("Cod") },
                readOnly = true,
                enabled = false,
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.name,
                onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                label = { Text("Nume *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.category,
                onValueChange = { v -> viewModel.update { it.copy(category = v) } },
                label = { Text("Categorie") },
                placeholder = { Text("ex: Electronice, Unelte, Consumabile") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.quantity,
                onValueChange = { v -> viewModel.update { it.copy(quantity = v.filter(Char::isDigit).take(6)) } },
                label = { Text("Cantitate *") },
                isError = form.quantityValue == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            LocationPicker(
                locations = locations,
                selectedId = form.locationId,
                onSelect = { id -> viewModel.update { it.copy(locationId = id) } }
            )
            OutlinedTextField(
                value = form.notes,
                onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
                label = { Text("Note") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPicker(
    locations: List<LocationEntity>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = locations.firstOrNull { it.id == selectedId }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { "${it.code} · ${it.name}" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Locație *") },
            placeholder = { Text("Alege locația") },
            supportingText = if (locations.isEmpty()) {
                { Text("Inventarul nu are locații. Creează mai întâi o locație.") }
            } else null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            locations.forEach { location ->
                DropdownMenuItem(
                    text = { Text("${location.code} · ${location.name}") },
                    onClick = {
                        onSelect(location.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
