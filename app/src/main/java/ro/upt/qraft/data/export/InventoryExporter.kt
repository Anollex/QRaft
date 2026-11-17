package ro.upt.qraft.data.export

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ro.upt.qraft.data.local.dao.InventoryDao
import ro.upt.qraft.data.local.dao.ItemDao
import ro.upt.qraft.data.local.dao.LocationDao
import ro.upt.qraft.utils.itemQrContent
import ro.upt.qraft.utils.locationQrContent
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scrie exporturile in fisierul ales de utilizator (Storage Access Framework),
 * deci nu sunt necesare permisiuni de stocare. Datele vin din baza locala (functioneaza si offline).
 */
@Singleton
class InventoryExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val inventoryDao: InventoryDao,
    private val locationDao: LocationDao,
    private val itemDao: ItemDao
) {

    /** @return numarul de itemi exportati */
    suspend fun exportCsv(inventoryId: String, uri: Uri): Int = withContext(Dispatchers.IO) {
        val locations = locationDao.getLocationsForInventoryOnce(inventoryId)
        val items = itemDao.getItemsForInventoryOnce(inventoryId)
        val csv = InventoryCsv.build(locations, items)
        open(uri).use { out ->
            out.write(InventoryCsv.UTF8_BOM)
            out.write(csv.toByteArray(Charsets.UTF_8))
        }
        items.size
    }

    /** Etichete pentru toate locatiile, apoi pentru toti itemii. @return numarul de etichete */
    suspend fun exportQrLabels(inventoryId: String, uri: Uri): Int = withContext(Dispatchers.IO) {
        val inventory = inventoryDao.getInventoryOnce(inventoryId) ?: throw IOException("Inventarul nu există")
        val locations = locationDao.getLocationsForInventoryOnce(inventoryId)
        val items = itemDao.getItemsForInventoryOnce(inventoryId)
        val byId = locations.associateBy { it.id }

        val locationLabels = locations.sortedBy { it.code }.map { location ->
            QrLabel(
                qrContent = locationQrContent(location.id),
                code = location.code,
                title = location.name,
                subtitle = location.parentId?.let { InventoryCsv.locationPath(it, byId) }
                    ?.let { "în $it" } ?: "Locație"
            )
        }
        val itemLabels = items
            .sortedWith(compareBy({ byId[it.locationId]?.code.orEmpty() }, { it.name.lowercase() }))
            .map { item ->
                QrLabel(
                    qrContent = itemQrContent(item.id),
                    code = item.code,
                    title = item.name,
                    subtitle = byId[item.locationId]?.let { "în ${it.code} · ${it.name}" }.orEmpty()
                )
            }

        val labels = locationLabels + itemLabels
        open(uri).use { out -> QrLabelsPdf.write(out, "QRaft · ${inventory.name}", labels) }
        labels.size
    }

    private fun open(uri: Uri): OutputStream =
        context.contentResolver.openOutputStream(uri) ?: throw IOException("Fișierul nu poate fi deschis")

    companion object {
        /** Nume implicit propus in dialogul de salvare, ex. "QRaft_Laborator_2026-11-17.csv". */
        fun suggestedFileName(inventoryName: String, extension: String): String {
            val safeName = inventoryName.trim()
                .replace(Regex("[^\\p{L}\\p{N}._-]+"), "_")
                .trim('_')
                .ifEmpty { "inventar" }
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            return "QRaft_${safeName}_$date.$extension"
        }
    }
}
