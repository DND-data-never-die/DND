package dnd.core.demo

import dnd.core.DndTime
import dnd.core.FatigueItem
import dnd.core.Grade
import dnd.core.HarshType
import dnd.core.Phrases
import dnd.core.SevereCondition
import java.time.DayOfWeek
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The numbers of the application text (기획서 v11, section 4) and figure 3, recomputed by the
 * engine from the demo data. These tests check consistency with the targets; the definitions
 * themselves are checked by the hand-made micro-tests.
 */
class ScenarioTest {
    private fun at(h: Int, m: Int, s: Int = 0) = DndTime.epochSec(LocalDateTime.of(2026, 11, 12, h, m, s))

    private val s1 = DemoScenarios.scenario1()
    private val s2 = DemoScenarios.scenario2()
    private val s3 = DemoScenarios.scenario3()

    // ---------------------------------------------------------------- scenario 1

    /**
     * The road has curves along distance (「도로의 구조·시설 기준에 관한 규칙」 제19조·제20조: radii 710 to
     * 2,000 m on the highway, 280 m on the urban expressway, 140 and 200 m in the city) and the
     * steering follows the position. The legs' curve loads, divided by the baseline's scale, stay
     * between the same baseline rows (leg 1 between 505.0 and 565.9, leg 2 between 931.0 and 968.0),
     * so the curve percentile 54 and every 부담 stay.
     */
    @Test
    fun scenario1RoadCurves() {
        assertEquals(2_487.4, s1.firstLeg.curveLoad, 0.1)
        assertEquals(4_574.7, s1.trip.curveLoad, 0.1)
        // The curves, the slower turns and the cruise drift keep the distance the vehicle detail shows.
        assertEquals(187.9, s1.trip.distanceKm, 1e-9)
        assertTrue(s1.firstLeg.curveLoad / Scenario1Data.CURVE_SCALE in 505.0..565.9)
        assertTrue(s1.trip.curveLoad / Scenario1Data.CURVE_SCALE in 931.0..968.0)
        // The highway cruise drifts around 100 km/h by at most 3 km/h.
        assertTrue(s1.replay.samples.any { it.speedKmh in 97.0..103.0 && it.speedKmh != 100.0 })
        val radii = listOf(710.0, 1_000.0, 1_500.0, 2_000.0).map { Math.toDegrees(kotlin.math.atan(2.895 / it)) }
        val highway = s1.replay.samples.filter { it.speedKmh > 90.0 && it.steeringDeg != 0.0 }
        assertTrue(highway.all { s -> radii.any { kotlin.math.abs(kotlin.math.abs(s.steeringDeg) - it) < 1e-9 } })
        assertTrue(highway.any { it.steeringDeg < 0.0 } && highway.any { it.steeringDeg > 0.0 })
        // No second of the log asks more lateral acceleration than an intersection turn at 15 km/h.
        val maxA = s1.replay.samples.maxOf { kotlin.math.abs(dnd.core.lateralAccel(it.speedKmh, it.steeringDeg, 2.895)) }
        assertEquals(2.80, maxA, 0.01)
        // At one frame per 20 s, most frames in D show a non-zero angle at one decimal.
        val frames = s1.replay.samples.filter { (it.epochSec - s1.replay.logStartEpochSec) % 20 == 0L && it.gear == dnd.core.Gear.D }
        val moving = frames.count { kotlin.math.abs(it.steeringDeg) >= 0.05 }
        assertTrue(moving * 2 > frames.size)
        // A parked car's wheels are straight: in P, and standing at 13:00, 14:30 and before 17:10.
        assertTrue(s1.replay.samples.filter { it.gear == dnd.core.Gear.P }.all { it.steeringDeg == 0.0 })
        listOf(at(13, 0), at(14, 15) - 1, at(14, 15), at(14, 30), at(17, 10) - 1, at(17, 10)).forEach { t ->
            val s = s1.replay.samples.first { it.epochSec == t }
            assertEquals(0.0, s.speedKmh, 0.0)
            assertEquals(0.0, s.steeringDeg, 0.0)
        }
    }

    @Test
    fun scenario1TripsFromTheSignalLog() {
        assertEquals(at(14, 30), s1.trip.startEpochSec)
        assertEquals(at(17, 10), s1.trip.endEpochSec)
        assertEquals(at(13, 0), s1.firstLeg.startEpochSec)
        assertEquals(at(14, 15), s1.firstLeg.endEpochSec)
        assertEquals(9_600L, s1.trip.continuousSec) // the 900 s rest resets (spec 2)
        assertEquals(2_880L, s1.trip.congestionSec)
        assertEquals(9, s1.trip.harshCount)
        assertEquals(0L, s1.trip.dawnSec)
        assertEquals(
            listOf(16.0, 9.0, 16.0, 10.0, 12.0, 15.0, 11.0, 11.0, 15.0),
            s1.harshEvents.map { it.magnitude },
        )
        assertEquals(2, s1.harshEvents.count { it.type == HarshType.SUDDEN_START })
        assertEquals(3, s1.harshEvents.count { it.type == HarshType.SUDDEN_ACCEL })
        assertEquals(2, s1.harshEvents.count { it.type == HarshType.SUDDEN_DECEL })
        assertEquals(2, s1.harshEvents.count { it.type == HarshType.SUDDEN_STOP })
        assertEquals(115.0, s1.trip.harshMagnitudeSum)
    }

    @Test
    fun scenario1DriverCard() {
        val d = s1.driverCard
        assertEquals(37, d.baselineTripCount)
        assertTrue(d.baselineDays >= 7)
        assertTrue(d.index >= 41.5 && d.index < 42.5, "index ${d.index}")
        assertEquals(42L, d.indexRounded)
        assertEquals(Grade.HIGH, d.grade)
        assertEquals("평소보다 높음", d.gradeText)
        assertEquals("피로도 지수 42, 평소보다 높음", d.headline)
        assertEquals(listOf(0L, 100L, 70L, 54L, 95L), d.evidence.map { it.percentileRounded })
        assertEquals(FatigueItem.CONTINUOUS, d.largestItem)
        assertEquals("연속 주행 2시간 40분", d.largestText)
        val c = assertNotNull(d.comparison)
        assertEquals(FatigueItem.CONGESTION, c.item)
        assertEquals("정체 48분", c.valueText)
        assertEquals("평소의 2배", c.ratioText)
        assertEquals("정체 48분, 평소의 2배", c.text)
        assertEquals(listOf("돌아가는 길에는 휴게소에서 15분 쉬어 가세요"), d.restAdvice)
        assertEquals(19L to 30L, d.gradeBoundsRounded)
        assertTrue(d.index > d.gradeBounds.high)
    }

    @Test
    fun scenario1Evidence() {
        val e = s1.driverCard.evidence
        assertEquals(listOf("새벽 운행", "연속 주행", "급가감속", "커브 주행", "정체"), e.map { it.label })
        assertEquals(listOf("0분", "2시간 40분", "9회, 크기 합 115"), e.take(3).map { it.valueText })
        assertEquals("48분", e[4].valueText)
        assertEquals(listOf("50", "20", "10", "10", "10"), e.map { it.weightText })
        assertEquals(listOf("0.0", "20.0", "7.0", "5.4", "9.5"), e.map { it.scoreText })
    }

    @Test
    fun scenario1VehicleCard() {
        val v = s1.vehicleNormal
        assertEquals("엔진 오일 교체까지", v.label)
        assertEquals("1,850km", v.remainingKmText)
        assertEquals("38일", v.remainingDaysText)
        assertEquals("1,850km 또는 38일", v.valuesText)
        assertEquals(13_150.0, v.drivenKm, 1e-9)
        assertEquals(15_000, v.interval.km)
        assertEquals(327L, v.elapsedDays)
        assertEquals(365L, v.intervalDays)
        val rows = v.severeRows.associateBy { it.condition }
        assertEquals("정체 48분", rows.getValue(SevereCondition.CONGESTION).thisTripText)
        assertEquals("급가감속 9회", rows.getValue(SevereCondition.HIGH_SPEED_HARSH).thisTripText)
        assertEquals(37, rows.getValue(SevereCondition.CONGESTION).recentTripCount)
        assertEquals(7_500, s1.vehicleSevere.interval.km)
    }

    @Test
    fun scenario1Replay() {
        val r = s1.replay
        assertEquals(at(13, 0), r.logStartEpochSec)
        assertEquals(at(17, 10), r.logEndEpochSec)
        assertEquals(listOf(at(13, 0) to at(14, 15), at(14, 30) to at(17, 10)), r.legs)
        assertEquals(at(14, 15) to at(14, 30), r.restStop)
        assertEquals(at(16, 30), r.voicePromptEpochSec)
        assertEquals("2시간 연속 운전하셨으니 가까운 휴게소에서 쉬어 가세요", r.voicePromptText)
        assertEquals(15_001, r.samples.size)
        assertEquals(251, s1.speedSeries.size)
        assertEquals(at(13, 0), s1.speedSeries.first().epochSec)
    }

    @Test
    fun scenario1IsDeterministic() {
        val again = DemoScenarios.scenario1()
        assertEquals(s1.driverCard, again.driverCard)
        assertEquals(s1.trip, again.trip)
        assertEquals(s1.baseline, again.baseline)
    }

    // ---------------------------------------------------------------- scenario 2

    @Test
    fun scenario2VehicleCard() {
        assertEquals(41, s2.recentTripCount)
        assertEquals(6.0, s2.medianDistanceKm)
        assertEquals(18.0, s2.meanStopStarts, 1e-12)
        val rows = s2.vehicleNormal.severeRows.associateBy { it.condition }
        assertEquals("중앙값 6km", rows.getValue(SevereCondition.SHORT_TRIPS).recentText)
        assertEquals("평균 18회", rows.getValue(SevereCondition.STOP_AND_GO).recentText)
        assertEquals("짧은 거리를 반복적으로 주행", SevereCondition.SHORT_TRIPS.manualText)
        assertEquals("정지와 출발의 잦은 반복", SevereCondition.STOP_AND_GO.manualText)
        val n = s2.vehicleNormal.countdown
        assertEquals(23L, n.remainingDays)
        assertEquals(11_410.0, n.remainingKm, 1e-9)
        assertEquals("23일", s2.vehicleNormal.remainingDaysText)
        val sev = s2.vehicleSevere.countdown
        assertTrue(sev.remainingDays < 0)
        assertEquals(160L, s2.vehicleSevere.overdueDays)
        assertEquals(3_910.0, sev.remainingKm, 1e-9)
    }

    @Test
    fun scenario2Briefings() {
        assertEquals("엔진 오일 교체 완료", s2.afterServiceBriefing.line)
        assertEquals("엔진 오일 교체까지 15,000km 또는 364일", s2.afterServiceBriefing.countdownNormal.text)
        assertEquals("연료 45% → 15%, 워셔액 부족", s2.handoverBriefing.line)
    }

    // ---------------------------------------------------------------- scenario 3

    @Test
    fun scenario3WeeklySummary() {
        assertEquals("1시간 10분", s3.departureDeviationText)
        assertTrue(s3.deviationLargerThanUsual)
        assertTrue(s3.departureDeviationSec > s3.usualDeviationSec)
        assertEquals("6시간 30분", s3.shortIntervalText)
        assertEquals(23_400L, s3.shortInterval.gapSec)
        assertEquals("목요일 23:20", s3.shortIntervalFromText)
        assertEquals("금요일 05:50", s3.shortIntervalToText)
        assertEquals(1, s3.restIntervals.count { it.isShort })
        val above = s3.congestionByWeekday.filter { it.aboveMean }.map { it.day }
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), above)
        assertEquals(5, s3.congestionByWeekday.size)
        assertEquals("월요일과 화요일은 출발 시각을 조정해 보세요", s3.advice)
        assertEquals(Phrases.VOICE_PROMPT, s1.replay.voicePromptText)
    }
}
