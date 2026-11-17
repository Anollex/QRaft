package ro.upt.qraft.data.export

import org.junit.Assert.assertEquals
import org.junit.Test
import ro.upt.qraft.data.local.entity.ItemEntity
import ro.upt.qraft.data.local.entity.LocationEntity
import java.util.TimeZone

class InventoryCsvTest {

    private val garaj = LocationEntity(id = "g", inventoryId = "i", parentId = null, code = "grj-000-001", name = "Garaj")
    private val raft = LocationEntity(id = "r", inventoryId = "i", parentId = "g", code = "grj-000-002", name = "Raft 1")

    @Test
    fun locationPath_joinsParents() {
        val byId = listOf(garaj, raft).associateBy { it.id }
        assertEquals("Garaj / Raft 1", InventoryCsv.locationPath("r", byId))
        assertEquals("", InventoryCsv.locationPath("missing", byId))
    }

    @Test
    fun locationPath_stopsOnCycle() {
        val a = garaj.copy(parentId = "r")
        val byId = listOf(a, raft).associateBy { it.id }
        assertEquals("Garaj / Raft 1", InventoryCsv.locationPath("r", byId))
    }

    @Test
    fun escape_quotesSpecialCharacters() {
        assertEquals("simplu", InventoryCsv.escape("simplu"))
        assertEquals("\"a,b\"", InventoryCsv.escape("a,b"))
        assertEquals("\"zis \"\"x\"\"\"", InventoryCsv.escape("zis \"x\""))
        assertEquals("\"linia1\nlinia2\"", InventoryCsv.escape("linia1\nlinia2"))
    }

    @Test
    fun text_neutralizesFormulas() {
        assertEquals("'=SUM(A1)", InventoryCsv.text("=SUM(A1)"))
        assertEquals("'@cmd", InventoryCsv.text("@cmd"))
        assertEquals("Șurubelniță", InventoryCsv.text("Șurubelniță"))
    }

    @Test
    fun build_writesHeaderAndSortedRows() {
        val items = listOf(
            ItemEntity(inventoryId = "i", locationId = "r", code = "111-222-333-444", name = "Ciocan",
                category = "Unelte", quantity = 2, notes = "mâner, lemn", updatedAt = 0L),
            ItemEntity(inventoryId = "i", locationId = "g", code = "555-666-777-888", name = "Bicicletă",
                quantity = 1, updatedAt = 0L)
        )
        val csv = InventoryCsv.build(listOf(garaj, raft), items, TimeZone.getTimeZone("UTC"))
        val lines = csv.split("\r\n")

        assertEquals(InventoryCsv.HEADER.joinToString(","), lines[0])
        assertEquals("555-666-777-888,Bicicletă,,1,grj-000-001,Garaj,,1970-01-01 00:00", lines[1])
        assertEquals("111-222-333-444,Ciocan,Unelte,2,grj-000-002,Garaj / Raft 1,\"mâner, lemn\",1970-01-01 00:00", lines[2])
        assertEquals("", lines[3]) // CRLF dupa ultimul rand
    }
}
