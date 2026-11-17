package ro.upt.qraft.data.export

import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Exportul itemilor unui inventar ca CSV (RFC 4180: separator virgula, CRLF, campuri citate la nevoie).
 * Pur Kotlin, fara dependinte Android, ca sa poata fi testat unitar.
 */
object InventoryCsv {

    val HEADER = listOf(
        "Cod item", "Nume", "Categorie", "Cantitate",
        "Cod locație", "Locație", "Note", "Ultima modificare"
    )

    /** BOM-ul UTF-8 face ca Excel sa afiseze corect diacriticele. */
    val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    fun build(
        locations: List<LocationEntity>,
        items: List<ItemEntity>,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val byId = locations.associateBy { it.id }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { this.timeZone = timeZone }

        val rows = items
            .map { item -> item to locationPath(item.locationId, byId) }
            .sortedWith(compareBy({ it.second.lowercase() }, { it.first.name.lowercase() }))
            .map { (item, path) ->
                listOf(
                    item.code,
                    text(item.name),
                    text(item.category),
                    item.quantity.toString(),
                    byId[item.locationId]?.code.orEmpty(),
                    text(path),
                    text(item.notes),
                    dateFormat.format(Date(item.updatedAt))
                )
            }

        return buildString {
            (listOf(HEADER) + rows).forEach { row ->
                row.joinTo(this, separator = ",") { escape(it) }
                append("\r\n")
            }
        }
    }

    /** Calea completa a locatiei, ex. "Garaj / Raft 1 / Sertar 2". Protejat impotriva ciclurilor. */
    fun locationPath(locationId: String, byId: Map<String, LocationEntity>): String {
        val names = ArrayDeque<String>()
        val visited = mutableSetOf<String>()
        var current = byId[locationId]
        while (current != null && visited.add(current.id)) {
            names.addFirst(current.name)
            current = current.parentId?.let(byId::get)
        }
        return names.joinToString(" / ")
    }

    fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value

    /**
     * Textul introdus de utilizatori (inclusiv de alti membri ai inventarului) nu trebuie interpretat
     * ca formula de Excel/Sheets ("CSV injection"), deci il prefixam cu apostrof.
     */
    fun text(value: String): String =
        if (value.isNotEmpty() && value[0] in "=+-@\t\r") "'$value" else value
}
