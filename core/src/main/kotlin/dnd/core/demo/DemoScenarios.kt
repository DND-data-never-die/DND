package dnd.core.demo

import dnd.core.Briefing
import dnd.core.Comparison
import dnd.core.Countdown
import dnd.core.DndTime
import dnd.core.DriverAssessment
import dnd.core.FatigueItem
import dnd.core.Fmt
import dnd.core.Gear
import dnd.core.Grade
import dnd.core.GradeBounds
import dnd.core.GrandeurEngine
import dnd.core.GrandeurOilIntervals
import dnd.core.HarshEvent
import dnd.core.MaintenanceRecord
import dnd.core.Phrases
import dnd.core.RestInterval
import dnd.core.Sample
import dnd.core.ServiceCondition
import dnd.core.ServiceInterval
import dnd.core.SevereRow
import dnd.core.TripSummary
import dnd.core.assessDriver
import dnd.core.baselineDays
import dnd.core.baselineFor
import dnd.core.briefing
import dnd.core.countdown
import dnd.core.detectHarshEvents
import dnd.core.median
import dnd.core.mean
import dnd.core.severeConditionRows
import dnd.core.voicePromptInstants
import dnd.core.weekdayName
import dnd.core.weeklySummary
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

// ---------------------------------------------------------------- UI data

/** One row of 상세 정보 (spec 5 breakdown). */
data class EvidenceRow(
    val item: FatigueItem,
    /** "연속 주행" */
    val label: String,
    val value: Double,
    /** "2시간 40분", "9회, 크기 합 115", "951m/s", "48분" */
    val valueText: String,
    val percentile: Double,
    val percentileRounded: Long,
    val weight: Double,
    /** "50", "20", "10" */
    val weightText: String,
    val score: Double,
    /** One decimal place: "28.0", "6.1" */
    val scoreText: String,
)

data class DriverCardUi(
    val index: Double,
    val indexRounded: Long,
    val grade: Grade,
    /** "평소보다 높음" */
    val gradeText: String,
    /** "피로도 지수 42, 평소보다 높음" */
    val headline: String,
    val gradeBounds: GradeBounds,
    val gradeBoundsRounded: Pair<Long, Long>,
    val largestItem: FatigueItem,
    /** "연속 주행 2시간 40분" */
    val largestText: String,
    /** "정체 48분" + "평소의 2배" */
    val comparison: Comparison?,
    /** "돌아가는 길에는 휴게소에서 15분 쉬어 가세요" */
    val restAdvice: List<String>,
    val evidence: List<EvidenceRow>,
    val baselineTripCount: Int,
    val baselineDays: Int,
)

data class VehicleCardUi(
    val engine: GrandeurEngine,
    val condition: ServiceCondition,
    val interval: ServiceInterval,
    val countdown: Countdown,
    /** "엔진 오일 교체까지" */
    val label: String,
    /** "1,850km" */
    val remainingKmText: String,
    /** "38일" */
    val remainingDaysText: String,
    /** "1,850km 또는 38일" */
    val valuesText: String,
    /** "엔진 오일 교체까지 1,850km 또는 38일" */
    val text: String,
    val drivenKm: Double,
    val elapsedDays: Long,
    val intervalDays: Long,
    val overdueDays: Long,
    val severeRows: List<SevereRow>,
)

data class BriefingUi(
    val lastDriveEnd: LocalDateTime,
    val now: LocalDateTime,
    val briefing: Briefing,
    /** "엔진 오일 교체 완료" */
    val line: String,
    val countdownNormal: Countdown,
    val countdownSevere: Countdown,
)

data class SpeedPoint(val epochSec: Long, val speedKmh: Double, val gear: Gear)

data class ReplayTimeline(
    val logStartEpochSec: Long,
    val logEndEpochSec: Long,
    /** Driving legs as [start, end) epoch seconds. */
    val legs: List<Pair<Long, Long>>,
    /** The rest stop in P, [start, end). */
    val restStop: Pair<Long, Long>,
    val voicePromptEpochSec: Long,
    /** "2시간 연속 운전하셨으니 가까운 휴게소에서 쉬어 가세요" */
    val voicePromptText: String,
    /** Every 1 Hz sample of the log. */
    val samples: List<Sample>,
) {
    /** The speed every [stepSec] seconds from the log start (a display choice of the caller). */
    fun speedSeries(stepSec: Int): List<SpeedPoint> {
        require(stepSec >= 1)
        return samples.filter { (it.epochSec - logStartEpochSec) % stepSec == 0L }.map { SpeedPoint(it.epochSec, it.speedKmh, it.gear) }
    }
}

data class Scenario1Screens(
    val today: LocalDate,
    val trip: TripSummary,
    val firstLeg: TripSummary,
    val baseline: List<TripSummary>,
    val harshEvents: List<HarshEvent>,
    val driverCard: DriverCardUi,
    val vehicleNormal: VehicleCardUi,
    val vehicleSevere: VehicleCardUi,
    val replay: ReplayTimeline,
    /** The speed series at [SPEED_SERIES_STEP_SEC] for the replay chart. */
    val speedSeries: List<SpeedPoint>,
)

data class Scenario2Screens(
    val today: LocalDate,
    val trip: TripSummary,
    val recentTrips: List<TripSummary>,
    val recentTripCount: Int,
    val medianDistanceKm: Double,
    val meanStopStarts: Double,
    val vehicleNormal: VehicleCardUi,
    val vehicleSevere: VehicleCardUi,
    val afterServiceBriefing: BriefingUi,
    val handoverBriefing: BriefingUi,
)

data class WeekdayCongestion(
    val day: DayOfWeek,
    /** "월요일" */
    val name: String,
    val seconds: Long,
    val minutes: Long,
    val aboveMean: Boolean,
)

data class Scenario3Screens(
    val weekStart: LocalDate,
    val summaryOpensAtEpochSec: Long,
    val departureDeviationSec: Double,
    /** "1시간 10분" */
    val departureDeviationText: String,
    val usualDeviationSec: Double,
    val usualDeviationText: String,
    val deviationLargerThanUsual: Boolean,
    val restIntervals: List<RestInterval>,
    /** The Thursday-Friday interval. */
    val shortInterval: RestInterval,
    /** "6시간 30분" */
    val shortIntervalText: String,
    /** "목요일 23:20" */
    val shortIntervalFromText: String,
    /** "금요일 05:50" */
    val shortIntervalToText: String,
    val congestionByWeekday: List<WeekdayCongestion>,
    val meanDailyCongestionSec: Double,
    /** "월요일과 화요일은 출발 시각을 조정해 보세요" */
    val advice: String,
)

// ---------------------------------------------------------------- facade

/** One function per scenario of the application (기획서 4). All inputs are fixed; no clock is read. */
object DemoScenarios {
    /** Display choice for the replay chart, not a definition. */
    const val SPEED_SERIES_STEP_SEC = 60

    val ENGINE = GrandeurEngine.GASOLINE_2_5

    fun scenario1(): Scenario1Screens {
        val built = Scenario1Data.build()
        val trip = built.leg2
        val assessment = assessDriver(trip, built.baseline)
        check(assessment is DriverAssessment.Ready) { "scenario 1 has a full baseline" }
        val stored = trip.copy(fatigueIndex = assessment.result.index)
        val legs = built.segments.map { it.startEpochSec to it.endEpochSec!! }
        val replay = ReplayTimeline(
            logStartEpochSec = built.log.first().epochSec,
            logEndEpochSec = built.log.last().epochSec,
            legs = legs,
            restStop = legs[0].second to legs[1].first,
            voicePromptEpochSec = voicePromptInstants(built.baseline.map { it.times } + trip.times).single(),
            voicePromptText = Phrases.VOICE_PROMPT,
            samples = built.log,
        )
        return Scenario1Screens(
            today = Scenario1Log.DATE,
            trip = stored,
            firstLeg = built.leg1,
            baseline = built.baseline,
            harshEvents = detectHarshEvents(built.segments[1].samples).events,
            driverCard = driverCard(assessment),
            vehicleNormal = vehicleCard(Scenario1Log.LAST_OIL_CHANGE, ServiceCondition.NORMAL, stored, built.baseline, Scenario1Log.DATE),
            vehicleSevere = vehicleCard(Scenario1Log.LAST_OIL_CHANGE, ServiceCondition.SEVERE, stored, built.baseline, Scenario1Log.DATE),
            replay = replay,
            speedSeries = replay.speedSeries(SPEED_SERIES_STEP_SEC),
        )
    }

    fun scenario2(): Scenario2Screens {
        val all = Scenario2Data.trips()
        val trip = all.last()
        val history = all.dropLast(1)
        val recent = baselineFor(history, trip.startEpochSec)
        val today = Scenario2Data.DATE
        val record = Scenario2Data.LAST_OIL_CHANGE
        fun briefingUi(last: LocalDateTime, now: LocalDateTime, lastState: dnd.core.VehicleState, nowState: dnd.core.VehicleState, odo: Double): BriefingUi {
            val latest = nowState.records.maxBy { it.date }
            val normal = countdown(latest, GrandeurOilIntervals.of(ENGINE, ServiceCondition.NORMAL), odo, now.toLocalDate())
            val severe = countdown(latest, GrandeurOilIntervals.of(ENGINE, ServiceCondition.SEVERE), odo, now.toLocalDate())
            val b = briefing(lastState, nowState, normal)
            return BriefingUi(last, now, b, b.line, normal, severe)
        }
        return Scenario2Screens(
            today = today,
            trip = trip,
            recentTrips = recent,
            recentTripCount = recent.size,
            medianDistanceKm = median(recent.map { it.distanceKm }),
            meanStopStarts = mean(recent.map { it.stopStartCount.toDouble() }),
            vehicleNormal = vehicleCard(record, ServiceCondition.NORMAL, trip, history, today),
            vehicleSevere = vehicleCard(record, ServiceCondition.SEVERE, trip, history, today),
            afterServiceBriefing = briefingUi(
                Scenario2Data.AFTER_SERVICE_LAST_DRIVE_END, Scenario2Data.AFTER_SERVICE_NOW,
                Scenario2Data.AFTER_SERVICE_LAST, Scenario2Data.AFTER_SERVICE_NOW_STATE, Scenario2Data.AFTER_SERVICE_ODOMETER_KM,
            ),
            handoverBriefing = briefingUi(
                Scenario2Data.HANDOVER_LAST_DRIVE_END, Scenario2Data.HANDOVER_NOW,
                Scenario2Data.HANDOVER_LAST, Scenario2Data.HANDOVER_NOW_STATE, Scenario2Data.HANDOVER_ODOMETER_KM,
            ),
        )
    }

    fun scenario3(): Scenario3Screens {
        val trips = Scenario3Data.trips()
        val week = Scenario3Data.WEEK_START
        val w = weeklySummary(trips, week)
        val dev = checkNotNull(w.departureDeviationSec)
        val usual = checkNotNull(w.usualDepartureDeviationSec)
        val short = w.restIntervals.single { it.isShort }
        val mean = checkNotNull(w.meanDailyCongestionSec)
        fun dayTime(epochSec: Long): String {
            val t = DndTime.dateTime(epochSec)
            return "${weekdayName(t.dayOfWeek)} %02d:%02d".format(DndTime.LOCALE, t.hour, t.minute)
        }
        return Scenario3Screens(
            weekStart = week,
            summaryOpensAtEpochSec = trips.last().endEpochSec,
            departureDeviationSec = dev,
            departureDeviationText = Fmt.durationText(dev),
            usualDeviationSec = usual,
            usualDeviationText = Fmt.durationText(usual),
            deviationLargerThanUsual = checkNotNull(w.deviationLargerThanUsual),
            restIntervals = w.restIntervals,
            shortInterval = short,
            shortIntervalText = Fmt.durationText(short.gapSec),
            shortIntervalFromText = dayTime(short.lastEndEpochSec),
            shortIntervalToText = dayTime(short.nextStartEpochSec),
            congestionByWeekday = w.congestionByDate.map { (date, sec) ->
                WeekdayCongestion(date.dayOfWeek, weekdayName(date.dayOfWeek), sec, Fmt.minutes(sec), date in w.congestionFlaggedDates)
            },
            meanDailyCongestionSec = mean,
            advice = checkNotNull(w.advice),
        )
    }

    // ------------------------------------------------------------ helpers

    private fun driverCard(a: DriverAssessment.Ready): DriverCardUi = DriverCardUi(
        index = a.result.index,
        indexRounded = a.result.indexRounded,
        grade = a.grade,
        gradeText = Phrases.gradeText(a.grade),
        headline = a.headline,
        gradeBounds = a.bounds,
        gradeBoundsRounded = Fmt.roundHalfUp(a.bounds.low) to Fmt.roundHalfUp(a.bounds.high),
        largestItem = a.largest,
        largestText = a.largestText,
        comparison = a.comparison,
        restAdvice = a.restAdvice,
        evidence = a.result.items.map { s ->
            EvidenceRow(
                item = s.item,
                label = s.item.label,
                value = s.value,
                valueText = evidenceValueText(s.item, a.trip),
                percentile = s.percentile,
                percentileRounded = Fmt.roundHalfUp(s.percentile),
                weight = s.weight,
                weightText = BigDecimal.valueOf(s.weight).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(),
                score = s.score,
                scoreText = BigDecimal.valueOf(s.score).setScale(1, RoundingMode.HALF_UP).toPlainString(),
            )
        },
        baselineTripCount = a.baseline.size,
        baselineDays = baselineDays(a.baseline),
    )

    /** 상세 정보 value column: the item value with its unit; 급가감속 shows the count and the magnitude sum. */
    private fun evidenceValueText(item: FatigueItem, trip: TripSummary): String = when (item) {
        FatigueItem.DAWN -> Fmt.durationText(trip.dawnSec)
        FatigueItem.CONTINUOUS -> Fmt.durationText(trip.continuousSec)
        FatigueItem.HARSH -> "${trip.harshCount}회, 크기 합 ${Fmt.decimal1(trip.harshMagnitudeSum)}"
        FatigueItem.CURVE -> "${Fmt.integer(trip.curveLoad)}m/s"
        FatigueItem.CONGESTION -> Fmt.durationText(trip.congestionSec)
    }

    private fun vehicleCard(
        record: MaintenanceRecord,
        condition: ServiceCondition,
        trip: TripSummary,
        history: List<TripSummary>,
        today: LocalDate,
    ): VehicleCardUi {
        val interval = GrandeurOilIntervals.of(ENGINE, condition)
        val c = countdown(record, interval, trip.endOdometerKm, today)
        return VehicleCardUi(
            engine = ENGINE,
            condition = condition,
            interval = interval,
            countdown = c,
            label = "${record.item.label} 교체까지",
            remainingKmText = c.remainingKmText,
            remainingDaysText = c.remainingDaysText,
            valuesText = c.valuesText,
            text = c.text,
            drivenKm = c.drivenKm,
            elapsedDays = c.elapsedDays,
            intervalDays = c.intervalDays,
            overdueDays = c.overdueDays,
            severeRows = severeConditionRows(trip, history),
        )
    }
}
