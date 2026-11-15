package dnd.app.ui.model

import dnd.core.Countdown
import dnd.core.DndTime
import dnd.core.FatigueItem
import dnd.core.Fmt
import dnd.core.Grade
import dnd.core.RecentStatistic
import dnd.core.SevereCondition
import dnd.core.SevereRow
import dnd.core.ServiceInterval
import dnd.core.demo.DriverCardUi
import dnd.core.demo.Scenario1Screens
import dnd.core.demo.Scenario2Screens
import dnd.core.demo.VehicleCardUi
import java.util.Locale

/* Copy the engine does not supply: the facade has no driver profile. */
internal const val DriverName = "운전자 1"
private const val LargestFactorNote = "가장 큰 요인"
private const val TimeLeadIn = "또는"
private const val DistanceStillLeft = "거리로는"
private const val TimeStillLeft = "기간으로는"
private const val DistanceAlsoPassed = "거리로도"
private const val TimeAlsoPassed = "기간으로도"
private const val Left = "남음"
/**
 * "교체 후 13,150 / 15,000km", "교체 후 327 / 365일": Hyundai writes 교체 for the oil (그랜저 2027
 * manual); the pictogram and the unit tell the two bars apart.
 */
private const val DistanceLead = "교체 후"
private const val TimeLead = "교체 후"
/** The manual's "짧은 거리를 반복적으로 주행" row compares one trip's distance (red team V1). */
private const val ShortTripsItem = "운행 거리"

/** The severe-condition rows Screen A shows for each scenario, in this order (screen spec and brief). */
private val Scenario1SevereRows = listOf(SevereCondition.CONGESTION, SevereCondition.HIGH_SPEED_HARSH)
private val Scenario2SevereRows = listOf(SevereCondition.SHORT_TRIPS, SevereCondition.STOP_AND_GO)

internal fun oneDecimal(x: Double, unit: String): String = String.format(Locale.ROOT, "%.1f%s", x, unit)

internal fun clockText(epochSec: Long): String {
    val t = DndTime.dateTime(epochSec)
    return String.format(Locale.ROOT, "%02d:%02d", t.hour, t.minute)
}

internal fun appTopBar() = TopBarState(driverName = DriverName)

/** Screen A for scenario 1. With [severeIntervalOn] the vehicle card uses the severe-condition countdown. */
fun Scenario1Screens.toTripEndUiState(selfCheck: Int? = null, severeIntervalOn: Boolean = false): TripEndUiState =
    TripEndUiState(
        topBar = appTopBar(),
        driver = driverCard.toDriverCardState(),
        vehicle = (if (severeIntervalOn) vehicleSevere else vehicleNormal).toVehicleCardState(Scenario1SevereRows, severeIntervalOn, vehicleSevere.interval),
        selfCheck = SelfCheckState(selected = selfCheck),
    )

/** The vehicle card's detail screen: every severe condition the engine returns that occurred in this trip. */
fun Scenario1Screens.toVehicleDetailUiState(severeIntervalOn: Boolean = false) =
    vehicleDetail(if (severeIntervalOn) vehicleSevere else vehicleNormal, severeIntervalOn, vehicleSevere.interval)

fun Scenario2Screens.toVehicleDetailUiState(severeIntervalOn: Boolean = false) =
    vehicleDetail(if (severeIntervalOn) vehicleSevere else vehicleNormal, severeIntervalOn, vehicleSevere.interval)

private fun vehicleDetail(v: VehicleCardUi, severeIntervalOn: Boolean, severeInterval: ServiceInterval): VehicleDetailUiState {
    val d = v.countdown.record.date
    return VehicleDetailUiState(
        topBar = appTopBar(),
        vehicle = v.toVehicleCardState(SevereCondition.entries, severeIntervalOn, severeInterval),
        // The bars already say the distance and the interval ("13,150 / 15,000km", "327 / 365일"), so
        // the last change keeps only its date (owner, 2026-11-15: fewer supplementary texts).
        facts = listOf(
            VehicleFact(VehicleEvidenceKind.ServiceRecord, "지난 교체", String.format(Locale.ROOT, "%d.%02d.%02d", d.year, d.monthValue, d.dayOfMonth)),
        ),
    )
}

/** "15,000km 또는 12개월": months as the manual writes them (red team V7). */
private fun ServiceInterval.text() = "${Fmt.integer(km.toLong())}km 또는 ${months}개월"

/** Screen A for scenario 2: the facade has no driver card for this trip, so only the vehicle card is shown. */
fun Scenario2Screens.toTripEndUiState(selfCheck: Int? = null, severeIntervalOn: Boolean = false): TripEndUiState =
    TripEndUiState(
        topBar = appTopBar(),
        driver = null,
        vehicle = (if (severeIntervalOn) vehicleSevere else vehicleNormal).toVehicleCardState(Scenario2SevereRows, severeIntervalOn, vehicleSevere.interval),
        selfCheck = SelfCheckState(selected = selfCheck),
    )

/**
 * "Whichever comes first" (the manual): time is named first when its used share (elapsed ÷
 * interval days) is at least the distance's (driven ÷ interval km).
 */
internal fun Countdown.timeFirst(): Boolean = elapsedDays.toDouble() / intervalDays >= drivenKm / interval.km

/** "엔진 오일 교체까지 7,311km 또는 177일", the limit reached first named first. */
internal fun Countdown.firstLimitText(): String {
    val (first, second) = if (timeFirst()) remainingDaysText to remainingKmText else remainingKmText to remainingDaysText
    return "${record.item.label} 교체까지 $first $TimeLeadIn $second"
}

internal fun Grade.segment(): Int = when (this) {
    Grade.LOW -> 0
    Grade.NORMAL -> 1
    Grade.HIGH -> 2
}

internal fun DriverCardUi.gradeScale(showBounds: Boolean = false) = GradeScale(
    value = index.toFloat(),
    lowUpper = gradeBounds.low.toFloat(),
    midUpper = gradeBounds.high.toFloat(),
    activeSegment = grade.segment(),
    boundLabels = if (showBounds) gradeBoundsRounded.first.toString() to gradeBoundsRounded.second.toString() else null,
)

private fun DriverCardUi.toDriverCardState() = DriverCardState(
    index = indexRounded.toString(),
    levelPill = gradeText,
    levelTone = when (grade) {
        Grade.HIGH -> Tone.Attention
        Grade.NORMAL -> Tone.Neutral
        Grade.LOW -> Tone.Good
    },
    grade = gradeScale(),
    largestFactor = FactLine(largestItem.toFactorKind(), largestText, LargestFactorNote),
    comparison = comparison?.let { FactLine(it.item.toFactorKind(), it.valueText, it.ratioText) },
    guidance = restAdvice.firstOrNull(),
)

/**
 * The manual's rule is "whichever comes first", so the hero is the limit reached first: the one
 * whose used share (used ÷ interval) is larger (UI round 3 brief, section 7; red team S1). The
 * other limit follows as "또는 …". A passed limit is the hero (time when both have passed).
 */
private fun VehicleCardUi.toVehicleCardState(
    rows: List<SevereCondition>,
    severeIntervalOn: Boolean,
    severeInterval: ServiceInterval,
): VehicleCardState {
    val kmPassed = countdown.remainingKm < 0.0
    val daysPassed = countdown.remainingDays < 0L
    val timeFirst = countdown.timeFirst()
    val (hero, second) = when {
        daysPassed && kmPassed -> remainingDaysText to "$DistanceAlsoPassed $remainingKmText"
        daysPassed -> remainingDaysText to "$DistanceStillLeft $remainingKmText $Left"
        kmPassed -> remainingKmText to "$TimeStillLeft $remainingDaysText $Left"
        timeFirst -> remainingDaysText to "$TimeLeadIn $remainingKmText"
        else -> remainingKmText to "$TimeLeadIn $remainingDaysText"
    }
    val overdue = daysPassed || kmPassed
    // A condition that did not occur in this trip is not shown (owner, 2026-11-15: no statements
    // that something is absent).
    val shown = rows.mapNotNull { c -> severeRows.firstOrNull { it.condition == c } }.filter { it.thisTrip != 0.0 }
    return VehicleCardState(
        dueHero = hero,
        dueHeroOverdue = overdue,
        dueSecond = second,
        distanceProgress = ProgressState(
            current = drivenKm.toFloat(),
            total = interval.km.toFloat(),
            lead = DistanceLead,
            valueLabel = "${Fmt.integer(drivenKm)} / ${Fmt.integer(interval.km.toLong())}km",
            overdue = kmPassed,
        ),
        timeProgress = ProgressState(
            current = elapsedDays.toFloat(),
            total = intervalDays.toFloat(),
            lead = TimeLead,
            valueLabel = "$elapsedDays / ${intervalDays}일",
            overdue = daysPassed,
        ),
        severeRows = shown.map { it.toLine() },
        severeIntervalOn = severeIntervalOn,
        severeIntervalText = severeInterval.text(),
        dueCaption = if (overdue) "${countdown.record.item.label} 교체 시기" else label,
    )
}

internal fun FatigueItem.toFactorKind(): FactorKind = when (this) {
    FatigueItem.DAWN -> FactorKind.Dawn
    FatigueItem.CONTINUOUS -> FactorKind.ContinuousDriving
    FatigueItem.HARSH -> FactorKind.HarshEvents
    FatigueItem.CURVE -> FactorKind.Curves
    FatigueItem.CONGESTION -> FactorKind.Congestion
}

/**
 * One row: the manual's phrase, the item's short name, this trip's value and the usual value. The
 * column header says "평소" once for the whole column (red team V2), so the engine's "중앙값"/"평균"
 * prefix is not repeated per row (the median/mean choice is in docs/formula.md).
 */
private fun SevereRow.toLine(): SevereConditionLine {
    val item = if (condition == SevereCondition.SHORT_TRIPS) ShortTripsItem else condition.valueLabel
    val kind = when (condition) {
        SevereCondition.SHORT_TRIPS -> SevereKind.ShortTrips
        SevereCondition.STOP_AND_GO -> SevereKind.StopAndGo
        SevereCondition.CONGESTION -> SevereKind.Congestion
        SevereCondition.HIGH_SPEED_HARSH -> SevereKind.HighSpeedHarsh
        SevereCondition.COLD -> SevereKind.Cold
    }
    // One format per column (red team M5): distances always with one decimal; counts as whole
    // numbers, the usual stop-start mean rounded half up ("13회"; round 4); durations as the engine
    // writes them.
    val value = if (condition == SevereCondition.SHORT_TRIPS) oneDecimal(thisTrip, "km") else thisTripText.removePrefix("${condition.valueLabel} ")
    val usual = when {
        condition == SevereCondition.SHORT_TRIPS -> oneDecimal(recent, "km")
        condition.recentStatistic == RecentStatistic.MEAN -> "${Fmt.roundHalfUp(recent)}회"
        else -> recentText.removePrefix("${condition.recentStatistic.label} ")
    }
    return SevereConditionLine(
        kind = kind,
        phrase = condition.manualText,
        item = item,
        value = value,
        usual = usual,
    )
}
