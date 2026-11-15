package dnd.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** spec 7 and 8 */
class MaintenanceAndBriefingTest {
    private val oil = MaintenanceItem.ENGINE_OIL

    @Test
    fun grandeurIntervalTable() {
        assertEquals(ServiceInterval(15_000, 12), GrandeurOilIntervals.of(GrandeurEngine.GASOLINE_2_5, ServiceCondition.NORMAL))
        assertEquals(ServiceInterval(7_500, 6), GrandeurOilIntervals.of(GrandeurEngine.LPG_3_5, ServiceCondition.SEVERE))
        assertEquals(ServiceInterval(10_000, 12), GrandeurOilIntervals.of(GrandeurEngine.HYBRID_1_6_TURBO, ServiceCondition.NORMAL))
        assertEquals(ServiceInterval(5_000, 6), GrandeurOilIntervals.of(GrandeurEngine.HYBRID_1_6_TURBO, ServiceCondition.SEVERE))
    }

    @Test
    fun countdownByHand() {
        val c = countdown(MaintenanceRecord(oil, LocalDate.of(2025, 12, 20), 10_000.0), ServiceInterval(15_000, 12), 23_150.0, LocalDate.of(2026, 11, 12))
        assertEquals(1_850.0, c.remainingKm)
        assertEquals(LocalDate.of(2026, 12, 20), c.dueDate)
        assertEquals(38L, c.remainingDays)
        assertEquals(327L, c.elapsedDays)
        assertEquals(365L, c.intervalDays)
        assertEquals(13_150.0, c.drivenKm)
        assertEquals("엔진 오일 교체까지 1,850km 또는 38일", c.text)
        assertFalse(c.isDue)
    }

    @Test
    fun overdueByDate() {
        val c = countdown(MaintenanceRecord(oil, LocalDate.of(2025, 12, 5), 0.0), ServiceInterval(7_500, 6), 3_000.0, LocalDate.of(2026, 11, 12))
        assertEquals(LocalDate.of(2026, 6, 5), c.dueDate)
        assertEquals(-160L, c.remainingDays)
        assertEquals(160L, c.overdueDays)
        assertTrue(c.isDue)
        assertEquals("4,500km 또는 160일 지남", c.valuesText)
    }

    @Test
    fun monthArithmeticClampsToTheMonthEnd() {
        val c = countdown(MaintenanceRecord(oil, LocalDate.of(2026, 8, 31), 0.0), ServiceInterval(7_500, 6), 0.0, LocalDate.of(2027, 2, 1))
        assertEquals(LocalDate.of(2027, 2, 28), c.dueDate)
        assertEquals(27L, c.remainingDays)
    }

    @Test
    fun severeRowsUseMedianAndMean() {
        val now = T.at(2026, 11, 12, 18, 0)
        val day = 86_400L
        val recent = listOf(
            T.trip(now - day, now - day + 900, distanceKm = 4.0, stopStarts = 10, congestionSec = 300, harshCount = 1),
            T.trip(now - 2 * day, now - 2 * day + 900, distanceKm = 6.0, stopStarts = 20, congestionSec = 600, harshCount = 3),
            T.trip(now - 3 * day, now - 3 * day + 900, distanceKm = 9.0, stopStarts = 30, congestionSec = 900, harshCount = 2, subZeroSec = 120),
        )
        val current = T.trip(now, now + 900, distanceKm = 5.5, stopStarts = 25, congestionSec = 2880, harshCount = 9)
        val rows = severeConditionRows(current, recent)
        assertEquals(SevereCondition.entries, rows.map { it.condition })
        assertEquals(listOf(5.5, 25.0, 2880.0, 9.0, 0.0), rows.map { it.thisTrip })
        assertEquals(listOf(6.0, 20.0, 600.0, 2.0, 0.0), rows.map { it.recent })
        assertEquals("운행당 거리 5.5km", rows[0].thisTripText)
        assertEquals("중앙값 6km", rows[0].recentText)
        assertEquals("평균 20회", rows[1].recentText)
        assertEquals("정체 48분", rows[2].thisTripText)
        assertEquals("급가감속 9회", rows[3].thisTripText)
        assertEquals("영하 운행 0분", rows[4].thisTripText)
        assertEquals("짧은 거리를 반복적으로 주행", rows[0].condition.manualText)
    }

    private val r1 = MaintenanceRecord(oil, LocalDate.of(2025, 12, 5), 31_000.0)
    private val r2 = MaintenanceRecord(oil, LocalDate.of(2026, 11, 14), 34_700.0)
    private fun state(fuel: Double, washer: Boolean = false, oilLevel: FluidLevel = FluidLevel.NORMAL, records: List<MaintenanceRecord> = listOf(r1)) =
        VehicleState(fuel, washer, oilLevel, FluidLevel.NORMAL, records)

    private val countdown = countdown(r1, ServiceInterval(15_000, 12), 34_000.0, LocalDate.of(2026, 11, 12))

    @Test
    fun handoverLine() {
        val b = briefing(state(45.0), state(15.0, washer = true), countdown)
        assertEquals("연료 45% → 15%, 워셔액 부족", b.line)
        assertFalse(b.showsCountdown)
    }

    @Test
    fun newRecordOnly() {
        val b = briefing(state(62.0), state(62.0, records = listOf(r1, r2)), countdown)
        assertEquals("엔진 오일 교체 완료", b.line)
    }

    @Test
    fun nothingChangedShowsTheCountdown() {
        val b = briefing(state(45.2), state(44.9), countdown)
        assertTrue(b.showsCountdown)
        assertEquals("엔진 오일 교체까지 12,000km 또는 23일", b.line)
    }

    @Test
    fun recordTogetherWithOtherChanges() {
        val b = briefing(state(45.0), state(30.0, oilLevel = FluidLevel.LOW, records = listOf(r1, r2)), countdown)
        assertEquals("연료 45% → 30%, 엔진 오일 레벨 정상 → 낮음, 엔진 오일 교체 완료", b.line)
    }
}
