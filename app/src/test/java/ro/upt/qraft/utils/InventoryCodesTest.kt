package ro.upt.qraft.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class InventoryCodesTest {

    @Test
    fun prefix_takesFirstThreeConsonants() {
        assertEquals("lbr", InventoryCodes.locationPrefix("Laborator"))
        assertEquals("css", InventoryCodes.locationPrefix("Casa Străbunicii"))
    }

    @Test
    fun prefix_ignoresDiacriticsAndNonLetters() {
        assertEquals("spr", InventoryCodes.locationPrefix("Șopron 2 - Mobilă"))
        assertEquals("tvm", InventoryCodes.locationPrefix("Țeava 1, ampră"))
    }

    @Test
    fun prefix_padsWhenNotEnoughConsonants() {
        assertEquals("rxx", InventoryCodes.locationPrefix("Rai"))
        assertEquals("xxx", InventoryCodes.locationPrefix("Aia"))
    }

    @Test
    fun locationCode_hasTwoGroupsOfThreeDigits() {
        assertEquals("lbr-000-007", InventoryCodes.locationCode("lbr", 7))
        assertEquals("lbr-001-042", InventoryCodes.locationCode("lbr", 1042))
    }

    @Test
    fun locationNumber_parsesValidCodesOnly() {
        assertEquals(1042, InventoryCodes.locationNumber("lbr-001-042"))
        assertNull(InventoryCodes.locationNumber("LAB-L-0007"))
    }

    @Test
    fun firstFreeLocationNumber_fillsGaps() {
        assertEquals(1, InventoryCodes.firstFreeLocationNumber(emptyList()))
        assertEquals(2, InventoryCodes.firstFreeLocationNumber(listOf("lbr-000-001", "lbr-000-003")))
        assertEquals(3, InventoryCodes.firstFreeLocationNumber(listOf("lbr-000-001", "lbr-000-002")))
    }

    @Test
    fun itemCode_isFourGroupsOfThreeDigits() {
        val pattern = Regex("^\\d{3}-\\d{3}-\\d{3}-\\d{3}$")
        val random = Random(42)
        repeat(1000) {
            assertTrue(pattern.matches(InventoryCodes.randomItemCode(random)))
        }
    }
}
