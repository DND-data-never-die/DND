package dnd.app

import dnd.core.SevereCondition
import java.io.File
import java.util.Locale

/**
 * The text budget of UI round 3 (addendum B), counted on what a screen actually draws:
 * - characters = Hangul syllables + digits + Latin letters (spaces and punctuation not counted);
 *   one element should hold at most 10 (B1, Kim & Park 2012); a pair drawn as two elements
 *   counts as two;
 * - sentence characters = the JAMA count (a number or a unit counts as one letter, punctuation not
 *   counted); a sentence should hold at most 30 (B8, JAMA v3.0 Annex 2 4(4));
 * - units per card (max): declared per screen by the capture (a value with its label is one unit;
 *   buttons are not counted); at most 4 per card, at most 6 rows in a detail list (B2, B4);
 * - manual quotations: elements that are the owner's manual's phrases (kept whole, rule 6);
 * - reading time = characters / 9.16 per second (Rhiu et al. 2017, Korean silent reading) and
 *   characters / 6.4 per second (with the older readers' 0.70 factor of Kim, Kim & Lee 2015).
 * Each capture appends one row to build/outputs/roborazzi/text_counts.tsv.
 */
object TextCounts {
    private val file = File("build/outputs/roborazzi/text_counts.tsv")
    private const val ELEMENT_LIMIT = 10
    private val quotations = SevereCondition.entries.map { it.manualText }.toSet()

    fun characters(s: String): Int = s.count { it in '가'..'힣' || it in '0'..'9' || it in 'A'..'Z' || it in 'a'..'z' }

    fun sentenceCharacters(s: String): Int {
        var n = 0
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c in '0'..'9' -> {
                    n++
                    while (i + 1 < s.length && (s[i + 1] in '0'..'9' || s[i + 1] == ',' || s[i + 1] == '.')) i++
                }
                c in 'A'..'Z' || c in 'a'..'z' -> {
                    n++
                    while (i + 1 < s.length && (s[i + 1] in 'A'..'Z' || s[i + 1] in 'a'..'z')) i++
                }
                c in '가'..'힣' -> n++
            }
            i++
        }
        return n
    }

    @Synchronized
    fun record(screen: String, texts: List<String>, unitsPerCard: String) {
        val elements = texts.map { it.replace('\n', ' ').trim() }.filter { characters(it) > 0 }
        val total = elements.sumOf { characters(it) }
        val longest = elements.maxByOrNull { characters(it) } ?: ""
        val over = elements.filter { characters(it) > ELEMENT_LIMIT }
        val sentence = elements.maxOfOrNull { sentenceCharacters(it) } ?: 0
        val quoted = elements.count { it in quotations }
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeText(
                listOf(
                    "screen", "elements", "characters", "longest", "longest_text", "over_10", "over_10_texts",
                    "sentence_chars_max", "units_per_card_max", "manual_quotations", "read_s_9.16", "read_s_6.4",
                ).joinToString("\t") + "\n",
            )
        }
        val row = listOf(
            screen,
            elements.size.toString(),
            total.toString(),
            characters(longest).toString(),
            longest,
            over.size.toString(),
            over.joinToString(" | "),
            sentence.toString(),
            unitsPerCard,
            quoted.toString(),
            String.format(Locale.ROOT, "%.1f", total / 9.16),
            String.format(Locale.ROOT, "%.1f", total / 6.4),
        )
        file.appendText(row.joinToString("\t") + "\n")
        println("TEXT " + row.joinToString("  "))
    }
}
