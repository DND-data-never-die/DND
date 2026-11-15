package dnd.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class ServiceCondition { NORMAL, SEVERE }

/** Grandeur (GN7) engines of the spec 7 table. */
enum class GrandeurEngine(val label: String) {
    GASOLINE_2_5("가솔린 2.5"),
    GASOLINE_3_5("가솔린 3.5"),
    LPG_3_5("LPG 3.5"),
    HYBRID_1_6_TURBO("1.6 터보 하이브리드"),
}

data class ServiceInterval(val km: Int, val months: Long)

/** spec 7: engine oil and oil filter intervals, 2027 Grandeur (GN7) owner's manual, service schedule. */
object GrandeurOilIntervals {
    fun of(engine: GrandeurEngine, condition: ServiceCondition): ServiceInterval = when (engine) {
        GrandeurEngine.GASOLINE_2_5, GrandeurEngine.GASOLINE_3_5, GrandeurEngine.LPG_3_5 -> when (condition) {
            ServiceCondition.NORMAL -> ServiceInterval(15_000, 12)
            ServiceCondition.SEVERE -> ServiceInterval(7_500, 6)
        }
        GrandeurEngine.HYBRID_1_6_TURBO -> when (condition) {
            ServiceCondition.NORMAL -> ServiceInterval(10_000, 12)
            ServiceCondition.SEVERE -> ServiceInterval(5_000, 6)
        }
    }
}

enum class MaintenanceItem(val label: String) { ENGINE_OIL("엔진 오일") }

data class MaintenanceRecord(val item: MaintenanceItem, val date: LocalDate, val odometerKm: Double)

/**
 * spec 7: remaining distance = last service odometer + interval km - current odometer;
 * remaining days = days from [today] to (last service date + interval months).
 * Negative values mean the point has passed.
 */
data class Countdown(
    val record: MaintenanceRecord,
    val interval: ServiceInterval,
    val currentOdometerKm: Double,
    val today: LocalDate,
) {
    val remainingKm: Double get() = record.odometerKm + interval.km - currentOdometerKm
    val dueDate: LocalDate get() = record.date.plusMonths(interval.months)
    val remainingDays: Long get() = ChronoUnit.DAYS.between(today, dueDate)
    val drivenKm: Double get() = currentOdometerKm - record.odometerKm
    val elapsedDays: Long get() = ChronoUnit.DAYS.between(record.date, today)
    val intervalDays: Long get() = ChronoUnit.DAYS.between(record.date, dueDate)

    /** Days past the due date; 0 while it has not passed. */
    val overdueDays: Long get() = maxOf(0L, -remainingDays)

    /** spec 7: whichever of the two comes first is the service point, so one of them reached means due. */
    val isDue: Boolean get() = remainingKm <= 0.0 || remainingDays <= 0L

    /** "1,850km". SPEC GAP: a passed distance is written "Nkm 지남" (the spec gives no wording). */
    val remainingKmText: String
        get() = if (remainingKm >= 0.0) "${Fmt.integer(remainingKm)}km" else "${Fmt.integer(-remainingKm)}km 지남"

    /** "38일". SPEC GAP: a passed date is written "N일 지남" (the spec gives no wording). */
    val remainingDaysText: String
        get() = if (remainingDays >= 0L) "${remainingDays}일" else "${-remainingDays}일 지남"

    /** spec 7: both values together, "1,850km 또는 38일". */
    val valuesText: String get() = "$remainingKmText 또는 $remainingDaysText"

    /** "엔진 오일 교체까지 1,850km 또는 38일". */
    val text: String get() = "${record.item.label} 교체까지 $valuesText"
}

fun countdown(
    record: MaintenanceRecord,
    interval: ServiceInterval,
    currentOdometerKm: Double,
    today: LocalDate,
): Countdown = Countdown(record, interval, currentOdometerKm, today)

/** spec 7: the five severe conditions of the manual that the trip log can be compared with. */
enum class SevereCondition(val manualText: String, val valueLabel: String, val recentStatistic: RecentStatistic) {
    SHORT_TRIPS("짧은 거리를 반복적으로 주행", "운행당 거리", RecentStatistic.MEDIAN),
    STOP_AND_GO("정지와 출발의 잦은 반복", "정지·출발", RecentStatistic.MEAN),
    // SPEC GAP: spec 7 names the statistic only for the first two rows; the other rows use the
    // usual value of spec 4 (median).
    CONGESTION("교통 체증이 심한 구역 주행", "정체", RecentStatistic.MEDIAN),
    HIGH_SPEED_HARSH("잦은 고속 주행 및 급가감속", "급가감속", RecentStatistic.MEDIAN),
    COLD("소금, 부식 물질 위 또는 한랭 지역 주행", "영하 운행", RecentStatistic.MEDIAN),
}

enum class RecentStatistic(val label: String) { MEDIAN("중앙값"), MEAN("평균") }

data class SevereRow(
    val condition: SevereCondition,
    val thisTrip: Double,
    val recent: Double,
    /** "정체 48분", "급가감속 9회" */
    val thisTripText: String,
    /** "중앙값 24분", "평균 18회" */
    val recentText: String,
    val recentTripCount: Int,
)

/**
 * spec 7: the manual's phrase, this trip's value and the value over the recent 28 days' trips.
 * SPEC GAP: "최근 28일 운행" is the spec 4 baseline window (trips that started in the 28 days
 * before this trip's start, this trip excluded).
 */
fun severeConditionRows(current: TripSummary, history: List<TripSummary>): List<SevereRow> {
    val recent = baselineFor(history, current.startEpochSec)
    require(recent.isNotEmpty()) { "no trips in the last 28 days" }
    return SevereCondition.entries.map { c ->
        val pick: (TripSummary) -> Double = when (c) {
            SevereCondition.SHORT_TRIPS -> { t -> t.distanceKm }
            SevereCondition.STOP_AND_GO -> { t -> t.stopStartCount.toDouble() }
            SevereCondition.CONGESTION -> { t -> t.congestionSec.toDouble() }
            SevereCondition.HIGH_SPEED_HARSH -> { t -> t.harshCount.toDouble() }
            SevereCondition.COLD -> { t -> t.subZeroSec.toDouble() }
        }
        val values = recent.map(pick)
        val stat = if (c.recentStatistic == RecentStatistic.MEAN) mean(values) else median(values)
        // SPEC GAP: distances and counts are written to one decimal place, as an integer when whole
        // (the spec 6 ratio format); times as durations (spec 1).
        val fmt: (Double) -> String = when (c) {
            SevereCondition.SHORT_TRIPS -> { v -> "${Fmt.decimal1(v)}km" }
            SevereCondition.STOP_AND_GO, SevereCondition.HIGH_SPEED_HARSH -> { v -> "${Fmt.decimal1(v)}회" }
            SevereCondition.CONGESTION, SevereCondition.COLD -> { v -> Fmt.durationText(v) }
        }
        val mine = pick(current)
        SevereRow(
            condition = c,
            thisTrip = mine,
            recent = stat,
            thisTripText = "${c.valueLabel} ${fmt(mine)}",
            recentText = "${c.recentStatistic.label} ${fmt(stat)}",
            recentTripCount = recent.size,
        )
    }
}
