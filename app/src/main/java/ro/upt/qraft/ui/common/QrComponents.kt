package ro.upt.qraft.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ro.upt.qraft.utils.QrCodeGenerator

/** Generarea unui QR e costisitoare, deci o facem o singura data per continut. */
@Composable
fun rememberQrBitmap(content: String, sizePx: Int = 512): ImageBitmap? =
    remember(content, sizePx) { QrCodeGenerator.generateQrBitmap(content, sizePx) }

@Composable
fun QrImage(content: String, modifier: Modifier = Modifier, sizePx: Int = 512) {
    rememberQrBitmap(content, sizePx)?.let {
        Image(bitmap = it, contentDescription = "Cod QR", modifier = modifier)
    }
}

/** Dialog cu codul QR mare, potrivit pentru scanare de pe alt telefon sau printare. */
@Composable
fun QrDialog(title: String, code: String, qrContent: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                QrImage(qrContent, Modifier.size(260.dp), sizePx = 768)
                Spacer(Modifier.height(12.dp))
                Text(code, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Închide") } }
    )
}

@Composable
fun ConfirmDeleteDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Șterge") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Renunță") } }
    )
}
