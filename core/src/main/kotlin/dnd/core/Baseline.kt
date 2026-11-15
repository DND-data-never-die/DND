package dnd.core

import java.time.Instant

/** spec 4: the baseline is the trips of the last 28 days. */
const val BASELINE_WINDOW_DAYS = 28L

/** spec 4: below 7 days with trips the card shows "평소 기록 수집 중". */
const val MIN_BASELINE_DAYS = 7

/**
 * spec 4: trips of the same profile that started within the 28 days before the current trip's
 * start; the current trip is not included. SPEC GAP: the window's lower end is inclusive
 * (a trip that started exactly 28 days earlier is in).
 */
fun baselineFor(history: List<TripSummary>, currentStartEpochSec: Long): List<TripSummary> {
    val from = Instant.ofEpochSecond(currentStartEpochSec).atZone(DndTime.ZONE).minusDays(BASELINE_WINDOW_DAYS).toEpochSecond()
    return history.filter { it.startEpochSec >= from && it.startEpochSec < currentStartEpochSec }
}

/**
 * spec 4: number of distinct dates (trip date = start date) with trips in the baseline.
 * SPEC GAP: "운행 기록이 쌓인 날" is counted inside the 28-day baseline window.
 */
fun baselineDays(baseline: List<TripSummary>): Int = baseline.map { it.date }.distinct().size

/** spec 4: the index is computed from 7 days on. */
fun baselineReady(baseline: List<TripSummary>): Boolean = baselineDays(baseline) >= MIN_BASELINE_DAYS

/**
 * spec 4 [파일럿], the one percentile definition of the engine (swap it here):
 * 100 x (baseline values strictly below [value]) / (number of baseline values).
 * A value of 0 gets 0 (no value is below 0); a value above every baseline value gets 100.
 */
fun percentileRank(value: Double, baseline: List<Double>): Double {
    require(baseline.isNotEmpty()) { "percentile needs at least one baseline value" }
    return 100.0 * baseline.count { it < value } / baseline.size
}

/** spec 4: median; for an even count the mean of the two middle values. */
fun median(values: List<Double>): Double {
    require(values.isNotEmpty()) { "median of nothing" }
    val s = values.sorted()
    val n = s.size
    return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2.0
}

/** spec 4: "평소의 N배" = value / median; no ratio when the median is 0. */
fun ratioToMedian(value: Double, median: Double): Double? = if (median == 0.0) null else value / median

/** spec 7: mean, used for the stop-start row of the severe-condition comparison. */
fun mean(values: List<Double>): Double {
    require(values.isNotEmpty()) { "mean of nothing" }
    return values.sum() / values.size
}
