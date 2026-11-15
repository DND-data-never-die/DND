package dnd.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

/** Rounding and Korean formatting. Rounding happens only here (spec 1). */
object Fmt {
    /** spec 5: round to an integer, 0.5 goes up. */
    fun roundHalfUp(x: Double): Long = BigDecimal.valueOf(x).setScale(0, RoundingMode.HALF_UP).longValueExact()

    /** One decimal place, 0.05 goes up (spec 6 writes ratios to one decimal place). */
    fun roundHalfUp1(x: Double): BigDecimal = BigDecimal.valueOf(x).setScale(1, RoundingMode.HALF_UP)

    /** spec 1: seconds are rounded to minutes only for display; 30 s goes up. */
    fun minutes(sec: Long): Long = BigDecimal.valueOf(sec).divide(SIXTY, 0, RoundingMode.HALF_UP).longValueExact()

    fun minutes(sec: Double): Long = BigDecimal.valueOf(sec).divide(SIXTY, 0, RoundingMode.HALF_UP).longValueExact()

    private val SIXTY = BigDecimal.valueOf(60)

    /**
     * "2시간 40분", "48분", "0분". SPEC GAP: the spec shows no whole-hour example; a whole number of
     * hours is written "2시간".
     */
    fun durationText(sec: Long): String = durationTextFromMinutes(minutes(sec))

    fun durationText(sec: Double): String = durationTextFromMinutes(minutes(sec))

    fun durationTextFromMinutes(totalMinutes: Long): String {
        require(totalMinutes >= 0) { "durations are not negative" }
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h == 0L -> "${m}분"
            m == 0L -> "${h}시간"
            else -> "${h}시간 ${m}분"
        }
    }

    /** "1,850" (Locale.KOREA grouping). */
    fun integer(n: Long): String = NumberFormat.getIntegerInstance(DndTime.LOCALE).format(n)

    /** Rounded half up to an integer, then grouped: 1849.6 -> "1,850". */
    fun integer(x: Double): String = integer(roundHalfUp(x))

    /**
     * spec 6: one decimal place, written as an integer when the rounded value is whole
     * ("2", "1.5"). Grouped with Locale.KOREA.
     */
    fun decimal1(x: Double): String {
        val format = DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(DndTime.LOCALE))
        return format.format(roundHalfUp1(x))
    }
}
