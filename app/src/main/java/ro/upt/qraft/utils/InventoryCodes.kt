package ro.upt.qraft.utils

import java.text.Normalizer
import java.util.Locale
import kotlin.random.Random

/**
 * Reguli de numerotare:
 *  - locatie: <primele 3 consoane din numele inventarului>-<3 cifre>-<3 cifre>, ex. "lbr-000-007"
 *  - item:    4 grupe de 3 cifre, unic in cadrul inventarului, ex. "482-907-115-630"
 */
object InventoryCodes {

    private const val CONSONANTS = "bcdfghjklmnpqrstvwxyz"
    private val LOCATION_CODE = Regex("^[a-z]{3}-(\\d{3})-(\\d{3})$")

    /** "Laborator" -> "lbr", "Șură" -> "sr" + completare -> "srx" (diacriticele sunt eliminate). */
    fun locationPrefix(inventoryName: String): String {
        val letters = Normalizer.normalize(inventoryName, Normalizer.Form.NFD)
            .lowercase(Locale.ROOT)
            .filter { it in 'a'..'z' }
        val consonants = letters.filter { it in CONSONANTS }
        return if (consonants.length >= 3) consonants.take(3) else consonants.padEnd(3, 'x')
    }

    fun locationCode(prefix: String, number: Int): String =
        String.format(Locale.US, "%s-%03d-%03d", prefix, number / 1000, number % 1000)

    /** Numarul din codul unei locatii ("lbr-001-042" -> 1042), sau null daca nu respecta formatul. */
    fun locationNumber(code: String): Int? =
        LOCATION_CODE.find(code)?.let { (it.groupValues[1] + it.groupValues[2]).toIntOrNull() }

    /** Primul numar liber (>= 1), astfel incat codurile sa ramana compacte. */
    fun firstFreeLocationNumber(existingCodes: Collection<String>): Int {
        val used = existingCodes.mapNotNull { locationNumber(it) }.toSet()
        var n = 1
        while (n in used) n++
        return n
    }

    fun randomItemCode(random: Random = Random): String =
        List(4) { random.nextInt(0, 1000) }.joinToString("-") { String.format(Locale.US, "%03d", it) }
}
