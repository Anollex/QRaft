package ro.upt.qraft.ui.location

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationEditScreen(
    viewModel: LocationEditViewModel = hiltViewModel(),
    onDone: () -> Unit
) {
    val form by viewModel.form.collectAsState()

    LaunchedEffect(form.isSaved) {
        if (form.isSaved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Editează locația" else "Locație nouă") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.Close, contentDescription = "Renunță")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = form.name.isNotBlank() && !form.isLoading) {
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
                onValueChange = viewModel::onNameChange,
                label = { Text("Nume *") },
                placeholder = { Text("ex: Raft A, Sertar 2, Cutie cabluri") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Descriere") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
