package dnd.app.ui.model

import dnd.core.MIN_REST_INTERVAL_SEC
import dnd.core.weekdayName
import dnd.core.demo.Scenario3Screens
import kotlin.math.roundToLong

/** Screen D, 주간 요약: three tiles and one guidance line. */
data class WeeklyUiState(
    val topBar: TopBarState,
    val departure: WeeklyTile,
    val restGap: WeeklyTile,
    val congestion: WeeklyTile,
    val advice: String?,
    val title: String = "주간 요약",
)

/**
 * A glance tile: title, one hero value, an optional attention label, one simple chart that draws
 * the reference its label names.
 */
data class WeeklyTile(
    val title: String,
    val value: String,
    val pill: String?,
    val chart: TileChart? = null,
    val pillTone: Tone = Tone.Attention,
)

sealed interface TileChart

/** Horizontal bars, longest = 1. */
data class HorizontalBars(val bars: List<TileBar>) : TileChart

/** Columns, tallest = 1, with an optional reference line at [reference] (0..1 of the tallest). */
data class ColumnBars(val bars: List<TileBar>, val reference: Float?, val referenceLabel: String?) : TileChart

data class TileBar(val label: String, val valueText: String?, val fraction: Float, val emphasized: Boolean)

/** Minutes per weekday with a dashed line at the week's mean; the flagged days carry their minutes. */
data class DayColumns(val bars: List<DayBar>, val mean: Float, val meanLabel: String) : TileChart

/**
 * One weekday: label under the bar; flagged days (above the mean) are dark with their minutes above
 * them ([minutesText]), the others grey without a number (owner, 2026-11-15: fewer texts).
 */
data class DayBar(val label: String, val minutesText: String?, val minutes: Float, val flagged: Boolean)

fun Scenario3Screens.toWeeklyUiState(): WeeklyUiState = WeeklyUiState(
    topBar = appTopBar(),
    departure = WeeklyTile(
        title = "출발 시각 편차",
        value = departureDeviationText,
        pill = if (deviationLargerThanUsual) "평소보다 큼" else null,
        // The facade has the week's spread and the usual one, not each day's departure, so the tile
        // compares the two as columns (red team I6). This week's value is the hero already, so only
        // the usual column carries its value (owner, 2026-11-15: fewer texts).
        chart = maxOf(departureDeviationSec, usualDeviationSec).takeIf { it > 0.0 }?.let { top ->
            ColumnBars(
                bars = listOf(
                    TileBar("이번 주", null, (departureDeviationSec / top).toFloat(), emphasized = deviationLargerThanUsual),
                    TileBar("평소", usualDeviationText, (usualDeviationSec / top).toFloat(), emphasized = false),
                ),
                reference = null,
                referenceLabel = null,
            )
        },
    ),
    restGap = WeeklyTile(
        title = "운행 간격",
        value = shortIntervalText,
        pill = if (shortInterval.isShort) "${MIN_REST_INTERVAL_SEC / 3600}시간보다 짧음" else null,
        // The dark column under its day says which gap the hero is (owner, 2026-11-15: no
        // "목 23:20 → 금 05:50" line).
        chart = restIntervalChart(),
    ),
    congestion = congestionTile(),
    advice = advice,
)

/** Each evening's rest until the next departure, against the 8-hour line; short rests are dark. */
private fun Scenario3Screens.restIntervalChart(): ColumnBars? {
    if (restIntervals.isEmpty()) return null
    val top = maxOf(restIntervals.maxOf { it.gapSec }, MIN_REST_INTERVAL_SEC).toFloat()
    return ColumnBars(
        bars = restIntervals.map { TileBar(weekdayName(it.date.dayOfWeek).take(1), null, it.gapSec / top, it.isShort) },
        reference = MIN_REST_INTERVAL_SEC / top,
        referenceLabel = "${MIN_REST_INTERVAL_SEC / 3600}시간",
    )
}

/**
 * Congestion per weekday. The hero names the days above the week's daily mean ("월·화"), the label
 * says so, and the chart draws that mean as a dashed line ("평균 21분"; red team W1).
 */
private fun Scenario3Screens.congestionTile(): WeeklyTile {
    val bars = congestionByWeekday.map { DayBar(it.name.take(1), if (it.aboveMean) "${it.minutes}분" else null, it.minutes.toFloat(), it.aboveMean) }
    val flagged = bars.filter { it.flagged }
    val hero = flagged.ifEmpty { listOfNotNull(bars.maxByOrNull { it.minutes }) }
    val meanMinutes = meanDailyCongestionSec / 60.0
    return WeeklyTile(
        title = "요일별 정체 시간",
        value = hero.joinToString("·") { it.label },
        pill = if (flagged.isNotEmpty()) "평균보다 긺" else null,     // 길다의 명사형은 "긺"
        chart = DayColumns(bars, meanMinutes.toFloat(), "평균 ${meanMinutes.roundToLong()}분"),
    )
}
