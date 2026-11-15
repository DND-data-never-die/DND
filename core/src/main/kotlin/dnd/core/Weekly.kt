package dnd.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.sqrt

/** spec 9: a week starts on Monday (ISO 8601). */
fun weekStartOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * spec 9: from Friday on, finishing a trip opens the weekly summary with the driver card.
 * The trip's day is its date (spec 2, the start date).
 */
fun opensWeeklySummary(trip: TripSummary): Boolean = trip.date.dayOfWeek >= DayOfWeek.FRIDAY

/** spec 9: sample standard deviation (divides by n - 1); null for fewer than two values. */
fun sampleStdDev(values: List<Double>): Double? {
    if (values.size < 2) return null
    val m = values.sum() / values.size
    return sqrt(values.sumOf { (it - m) * (it - m) } / (values.size - 1))
}

/** spec 9: the first departure of each date of the week, as seconds since local midnight. */
fun firstDepartures(trips: List<TripSummary>, weekStart: LocalDate): Map<LocalDate, Int> =
    tripsOfWeek(trips, weekStart).groupBy { it.date }.toSortedMap()
        .mapValues { (_, ts) -> DndTime.secondOfDay(ts.minOf { it.startEpochSec }) }

/** spec 9: the sample standard deviation of the week's first departure times, in seconds. */
fun departureDeviationSec(trips: List<TripSummary>, weekStart: LocalDate): Double? =
    sampleStdDev(firstDepartures(trips, weekStart).values.map { it.toDouble() })

/**
 * spec 9: the usual value is the median of the same value over the three preceding weeks.
 * SPEC GAP: a preceding week with fewer than two driving days has no value and is left out.
 */
fun usualDepartureDeviationSec(trips: List<TripSummary>, weekStart: LocalDate): Double? {
    val values = (1L..3L).mapNotNull { k -> departureDeviationSec(trips, weekStart.minusWeeks(k)) }
    return if (values.isEmpty()) null else median(values)
}

/** spec 9: 8 hours, 여객자동차 운수사업법 시행규칙 제44조의6 (business drivers, used as a reference). */
const val MIN_REST_INTERVAL_SEC = 8L * 3600L

/** spec 9: from the end of the last trip of [date] to the start of the first trip of the next date. */
data class RestInterval(val date: LocalDate, val lastEndEpochSec: Long, val nextStartEpochSec: Long) {
    val gapSec: Long get() = nextStartEpochSec - lastEndEpochSec
    val isShort: Boolean get() = gapSec < MIN_REST_INTERVAL_SEC
}

/**
 * spec 9: rest intervals between consecutive dates of the week. Trips belong to their start date,
 * so a return after midnight still ends "its" day.
 * SPEC GAP: only pairs whose two dates both fall inside the week and both have trips are listed.
 */
fun restIntervals(trips: List<TripSummary>, weekStart: LocalDate): List<RestInterval> {
    val byDate = tripsOfWeek(trips, weekStart).groupBy { it.date }
    return (0L..5L).mapNotNull { d ->
        val day = weekStart.plusDays(d)
        val today = byDate[day] ?: return@mapNotNull null
        val next = byDate[day.plusDays(1)] ?: return@mapNotNull null
        RestInterval(day, today.maxOf { it.endEpochSec }, next.minOf { it.startEpochSec })
    }
}

/** spec 9: congestion time summed per date of the week (dates with trips only). */
fun congestionByDate(trips: List<TripSummary>, weekStart: LocalDate): Map<LocalDate, Long> =
    tripsOfWeek(trips, weekStart).groupBy { it.date }.toSortedMap().mapValues { (_, ts) -> ts.sumOf { it.congestionSec } }

/** spec 9: dates whose congestion is above the mean over the days with driving. */
fun congestionFlaggedDates(byDate: Map<LocalDate, Long>): List<LocalDate> {
    if (byDate.isEmpty()) return emptyList()
    val meanSec = byDate.values.sum().toDouble() / byDate.size
    return byDate.filterValues { it > meanSec }.keys.sorted()
}

private val WEEKDAY_NAMES = mapOf(
    DayOfWeek.MONDAY to "월요일",
    DayOfWeek.TUESDAY to "화요일",
    DayOfWeek.WEDNESDAY to "수요일",
    DayOfWeek.THURSDAY to "목요일",
    DayOfWeek.FRIDAY to "금요일",
    DayOfWeek.SATURDAY to "토요일",
    DayOfWeek.SUNDAY to "일요일",
)

fun weekdayName(day: DayOfWeek): String = WEEKDAY_NAMES.getValue(day)

/**
 * spec 9 advice: "월요일과 화요일은 출발 시각을 조정해 보세요". Every weekday name ends in 일, so the
 * particles are 과 and 은. SPEC GAP: three or more days are written "월요일, 화요일과 수요일은 ...".
 */
fun departureAdvice(days: List<DayOfWeek>): String? {
    if (days.isEmpty()) return null
    val names = days.map(::weekdayName)
    val joined = if (names.size == 1) names[0] else names.dropLast(1).joinToString(", ") + "과 " + names.last()
    return "${joined}은 출발 시각을 조정해 보세요"
}

data class WeeklySummary(
    val weekStart: LocalDate,
    val firstDepartures: Map<LocalDate, Int>,
    val departureDeviationSec: Double?,
    val usualDepartureDeviationSec: Double?,
    val restIntervals: List<RestInterval>,
    val congestionByDate: Map<LocalDate, Long>,
    val meanDailyCongestionSec: Double?,
    val congestionFlaggedDates: List<LocalDate>,
) {
    val deviationLargerThanUsual: Boolean?
        get() {
            val now = departureDeviationSec ?: return null
            val usual = usualDepartureDeviationSec ?: return null
            return now > usual
        }

    val advice: String? get() = departureAdvice(congestionFlaggedDates.map { it.dayOfWeek })
}

/** spec 9: the weekly summary of the week that starts on [weekStart] from the trips given. */
fun weeklySummary(trips: List<TripSummary>, weekStart: LocalDate): WeeklySummary {
    require(weekStart.dayOfWeek == DayOfWeek.MONDAY) { "weeks start on Monday" }
    val congestion = congestionByDate(trips, weekStart)
    return WeeklySummary(
        weekStart = weekStart,
        firstDepartures = firstDepartures(trips, weekStart),
        departureDeviationSec = departureDeviationSec(trips, weekStart),
        usualDepartureDeviationSec = usualDepartureDeviationSec(trips, weekStart),
        restIntervals = restIntervals(trips, weekStart),
        congestionByDate = congestion,
        meanDailyCongestionSec = if (congestion.isEmpty()) null else congestion.values.sum().toDouble() / congestion.size,
        congestionFlaggedDates = congestionFlaggedDates(congestion),
    )
}

private fun tripsOfWeek(trips: List<TripSummary>, weekStart: LocalDate): List<TripSummary> {
    val end = weekStart.plusDays(7)
    return trips.filter { !it.date.isBefore(weekStart) && it.date.isBefore(end) }
}
