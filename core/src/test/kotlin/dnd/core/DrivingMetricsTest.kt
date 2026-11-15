package dnd.core

import java.time.LocalDate
import kotlin.math.tan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** spec 3-2 to 3-5 with hand-made sequences. */
class DrivingMetricsTest {
    private val t0 = T.at(2026, 11, 3, 8, 0)

    // ---------- 3-2 ----------

    @Test
    fun firstDepartureIsNotAStopStart() {
        // departs at index 2 (not counted), stops at 4-5, goes at 6, stops at 7, goes at 8
        assertEquals(2, stopStartCount(T.drive(t0, 0.0, 0.0, 5.0, 10.0, 0.0, 0.0, 8.0, 0.0, 6.0, 6.0)))
        assertEquals(listOf(6, 8), stopStartIndices(T.drive(t0, 0.0, 0.0, 5.0, 10.0, 0.0, 0.0, 8.0, 0.0, 6.0, 6.0)))
    }

    @Test
    fun aTripThatStartsMovingCountsItsFirstStop() {
        assertEquals(1, stopStartCount(T.drive(t0, 10.0, 5.0, 0.0, 0.0, 5.0, 9.0)))
    }

    @Test
    fun noStopStartWithoutARestart() {
        assertEquals(0, stopStartCount(T.drive(t0, 0.0, 10.0, 20.0, 10.0, 0.0, 0.0)))
    }

    // ---------- 3-3 ----------

    @Test
    fun lowSpeedRunWithOneStopStartIsNotCongestion() {
        // run = indices 0..6 (7 s, all <= 40), one stop-start (index 4)
        val s = T.drive(t0, 30.0, 20.0, 0.0, 0.0, 10.0, 20.0, 30.0, 45.0, 50.0)
        val runs = lowSpeedRuns(s)
        assertEquals(listOf(LowSpeedRun(0, 6, 1)), runs)
        assertEquals(0L, congestionSeconds(s))
    }

    @Test
    fun lowSpeedRunWithTwoStopStartsIsCongestion() {
        // run = indices 0..5 (6 s), stop-starts at 2 and 4; 41 km/h ends the run
        val s = T.drive(t0, 30.0, 0.0, 10.0, 0.0, 10.0, 30.0, 41.0, 50.0)
        assertEquals(listOf(LowSpeedRun(0, 5, 2)), lowSpeedRuns(s))
        assertEquals(6L, congestionSeconds(s))
    }

    @Test
    fun fortyIsInsideTheRunAndFortyOneIsOutside() {
        val s = T.drive(t0, 45.0, 40.0, 0.0, 5.0, 0.0, 5.0, 40.0, 40.25, 45.0)
        assertEquals(listOf(LowSpeedRun(1, 6, 2)), lowSpeedRuns(s))
        assertEquals(6L, congestionSeconds(s))
    }

    @Test
    fun theTripsFirstDepartureDoesNotCountTowardCongestion() {
        // departure at index 2 is the first one; only index 4 counts
        val s = T.drive(t0, 0.0, 0.0, 5.0, 0.0, 5.0, 50.0, 60.0)
        assertEquals(listOf(LowSpeedRun(0, 4, 1)), lowSpeedRuns(s))
        assertEquals(0L, congestionSeconds(s))
    }

    @Test
    fun aMissingSampleEndsTheRun() {
        val joined = T.drive(t0, 30.0, 0.0, 10.0, 0.0, 10.0, 30.0, 50.0)
        assertEquals(6L, congestionSeconds(joined))
        val gapped = T.timed(t0 to 30.0, t0 + 1 to 0.0, t0 + 2 to 10.0, t0 + 4 to 0.0, t0 + 5 to 10.0, t0 + 6 to 30.0, t0 + 7 to 50.0)
        assertEquals(listOf(LowSpeedRun(0, 2, 1), LowSpeedRun(3, 5, 1)), lowSpeedRuns(gapped))
        assertEquals(0L, congestionSeconds(gapped))
    }

    // ---------- 3-4 ----------

    @Test
    fun lateralAccelerationUsesRadiansAndTheWheelbase() {
        // 36 km/h = 10 m/s, 45 degrees -> tan = 1, a = 100 / L
        assertEquals(100.0 / 2.895, lateralAccel(36.0, 45.0, 2.895), 1e-9)
        // 72 km/h = 20 m/s, 1 degree, L = 3.0
        assertEquals(400.0 * tan(Math.PI / 180.0) / 3.0, lateralAccel(72.0, 1.0, 3.0), 1e-12)
    }

    @Test
    fun curveLoadSumsAbsoluteValuesPerSecond() {
        val s = listOf(
            T.sample(t0, 36.0, steeringDeg = 45.0),
            T.sample(t0 + 1, 36.0, steeringDeg = -45.0),
            T.sample(t0 + 2, 0.0, steeringDeg = 30.0),
            T.sample(t0 + 3, 36.0, steeringDeg = 0.0),
            T.sample(t0 + 4, 36.0, steeringDeg = 45.0),
            T.sample(t0 + 5, 36.0, steeringDeg = 0.0),
        )
        assertEquals(3 * 100.0 / 2.0, curveLoad(s, 2.0), 1e-9)
    }

    // ---------- 3-5 ----------

    @Test
    fun dawnTimeAcrossMidnightAndTheTripDate() {
        val start = T.at(2026, 11, 9, 23, 30)
        val end = T.at(2026, 11, 10, 3, 0)
        assertEquals(3600L, dawnSeconds(start, end))
        val trip = T.trip(start, end)
        assertEquals(LocalDate.of(2026, 11, 9), trip.date)
    }

    @Test
    fun dawnWindowIsHalfOpenAndCoversEveryDate() {
        assertEquals(3L * 3600L, dawnSeconds(T.at(2026, 11, 10, 1, 0), T.at(2026, 11, 10, 6, 0)))
        assertEquals(0L, dawnSeconds(T.at(2026, 11, 10, 5, 0), T.at(2026, 11, 10, 6, 0)))
        assertEquals(0L, dawnSeconds(T.at(2026, 11, 10, 1, 0), T.at(2026, 11, 10, 2, 0)))
        assertEquals(1L, dawnSeconds(T.at(2026, 11, 10, 1, 0), T.at(2026, 11, 10, 2, 0, 1)))
        // two nights: 3 h on the 10th and 2 h on the 11th
        assertEquals(5L * 3600L, dawnSeconds(T.at(2026, 11, 10, 1, 0), T.at(2026, 11, 11, 4, 0)))
    }

    @Test
    fun heatAndSubZero() {
        assertTrue(isHeatTrip(33.0))
        assertFalse(isHeatTrip(32.9))
        val s = listOf(1.0, 0.0, -0.5, -1.0, 0.0, 2.0).mapIndexed { i, c -> T.sample(t0 + i, 30.0, tempC = c) }
        assertEquals(2L, subZeroSeconds(s))
    }
}
