package dnd.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** spec 3-1, hand-made speed sequences (km/h, 1 Hz). Values are exact in binary floating point. */
class HarshEventsTest {
    private val t0 = T.at(2026, 11, 2, 9, 0)

    private fun events(vararg speeds: Double) = detectHarshEvents(T.drive(t0, *speeds)).events

    @Test
    fun suddenStartFromRest() {
        val e = events(0.0, 0.0, 11.0, 13.0, 15.0, 17.0)
        assertEquals(1, e.size)
        assertEquals(HarshType.SUDDEN_START, e[0].type)
        assertEquals(11.0, e[0].magnitude)
        assertEquals(t0 + 2, e[0].firstEpochSec)
    }

    @Test
    fun suddenStartEdges() {
        assertEquals(HarshType.SUDDEN_START, classifyHarshSecond(5.0, 15.0))
        assertNull(classifyHarshSecond(5.0, 14.75))
        // 5.25 is above 5.0 (no 급출발) and rounds to 5 (no 급가속 band): the eTAS gap.
        assertNull(classifyHarshSecond(5.25, 25.0))
        // 5.5 rounds to 6: first 급가속 band, 12 km/h/s.
        assertEquals(HarshType.SUDDEN_ACCEL, classifyHarshSecond(5.5, 17.5))
        assertNull(classifyHarshSecond(5.5, 17.25))
    }

    @Test
    fun accelerationBandEdges() {
        // band 6-10 needs 12
        assertEquals(HarshType.SUDDEN_ACCEL, classifyHarshSecond(10.25, 22.25))
        assertNull(classifyHarshSecond(10.25, 22.0))
        // 10.5 rounds to 11: band 11-20 needs 10
        assertEquals(HarshType.SUDDEN_ACCEL, classifyHarshSecond(10.5, 20.5))
        assertNull(classifyHarshSecond(10.5, 20.25))
        assertEquals(HarshType.SUDDEN_ACCEL, classifyHarshSecond(20.0, 30.0))
        assertNull(classifyHarshSecond(20.0, 29.75))
        // 20.5 rounds to 21: third band needs 8 ("21km/h는 셋째 속도대")
        assertEquals(HarshType.SUDDEN_ACCEL, classifyHarshSecond(20.5, 28.5))
        assertNull(classifyHarshSecond(20.5, 28.25))
        assertEquals(HarshType.SUDDEN_ACCEL, classifyHarshSecond(21.0, 29.0))
        assertEquals(HarshType.SUDDEN_ACCEL, classifyHarshSecond(100.0, 108.0))
        assertNull(classifyHarshSecond(100.0, 107.75))
    }

    @Test
    fun decelerationBandEdges() {
        // 30 or less needs 14
        assertEquals(HarshType.SUDDEN_DECEL, classifyHarshSecond(30.0, 16.0))
        assertNull(classifyHarshSecond(30.0, 16.25))
        // 30.5 rounds to 31: needs 15
        assertNull(classifyHarshSecond(30.5, 16.5))
        assertEquals(HarshType.SUDDEN_DECEL, classifyHarshSecond(30.5, 15.5))
        assertEquals(HarshType.SUDDEN_DECEL, classifyHarshSecond(60.0, 45.0))
        assertNull(classifyHarshSecond(60.0, 45.25))
        // this second's speed: 6.0 or more for 급감속, 5.0 or less for 급정지, nothing between
        assertEquals(HarshType.SUDDEN_DECEL, classifyHarshSecond(20.0, 6.0))
        assertNull(classifyHarshSecond(20.0, 5.5))
        assertEquals(HarshType.SUDDEN_STOP, classifyHarshSecond(19.0, 5.0))
        assertNull(classifyHarshSecond(18.75, 5.0))
    }

    @Test
    fun eachTypeOnceCountAndMagnitudeSum() {
        // +12 from 0 급출발 | +10 from 16 급가속 (band 11-20) | -15 from 30 to 15 급감속 (band <=30 needs 14)
        // | -14 to 0 급정지; quiet seconds between them
        val s = detectHarshEvents(T.drive(t0, 0.0, 12.0, 14.0, 16.0, 26.0, 28.0, 30.0, 15.0, 14.0, 0.0, 0.0))
        assertEquals(
            listOf(HarshType.SUDDEN_START, HarshType.SUDDEN_ACCEL, HarshType.SUDDEN_DECEL, HarshType.SUDDEN_STOP),
            s.events.map { it.type },
        )
        assertEquals(listOf(12.0, 10.0, 15.0, 14.0), s.events.map { it.magnitude })
        assertEquals(4, s.count)
        assertEquals(51.0, s.magnitudeSum)
    }

    @Test
    fun startFollowedByAccelIsOneStart() {
        // +10 from 0 (급출발), +12 from 10 (급가속): one run, one 급출발 of magnitude 12
        val e = events(0.0, 10.0, 22.0, 26.0, 30.0, 30.0)
        assertEquals(1, e.size)
        assertEquals(HarshType.SUDDEN_START, e[0].type)
        assertEquals(12.0, e[0].magnitude)
        assertEquals(t0 + 1, e[0].firstEpochSec)
        assertEquals(t0 + 2, e[0].lastEpochSec)
    }

    @Test
    fun decelFollowedByStopIsOneStop() {
        // -16, -16, -14 (급감속 x3), then -14 to 0 (급정지): one 급정지 of magnitude 16
        val e = events(76.0, 60.0, 44.0, 28.0, 14.0, 0.0, 0.0)
        assertEquals(1, e.size)
        assertEquals(HarshType.SUDDEN_STOP, e[0].type)
        assertEquals(16.0, e[0].magnitude)
    }

    @Test
    fun aQuietSecondSplitsEvents() {
        // 급출발 (+10), quiet (+2), 급가속 (+12 from 12, band 11-20 needs 10)
        val e = events(0.0, 10.0, 12.0, 24.0, 25.0, 26.0)
        assertEquals(listOf(HarshType.SUDDEN_START, HarshType.SUDDEN_ACCEL), e.map { it.type })
        assertEquals(listOf(10.0, 12.0), e.map { it.magnitude })
    }

    @Test
    fun directionChangeSplitsEvents() {
        val e = events(40.0, 50.0, 35.0, 35.0, 35.0, 35.0)
        assertEquals(listOf(HarshType.SUDDEN_ACCEL, HarshType.SUDDEN_DECEL), e.map { it.type })
    }

    @Test
    fun noDeltaAcrossAMissingSample() {
        // t+2 is missing: 0 -> 15 over two seconds is not a Δv
        val gap = T.timed(t0 to 0.0, t0 + 1 to 0.0, t0 + 3 to 15.0, t0 + 4 to 16.0, t0 + 5 to 17.0, t0 + 6 to 18.0)
        assertEquals(0, detectHarshEvents(gap).count)
        // two accelerations either side of a gap are two events, not one
        val split = T.timed(t0 to 30.0, t0 + 1 to 40.0, t0 + 3 to 45.0, t0 + 4 to 55.0, t0 + 5 to 56.0, t0 + 6 to 57.0)
        assertEquals(2, detectHarshEvents(split).count)
    }
}
