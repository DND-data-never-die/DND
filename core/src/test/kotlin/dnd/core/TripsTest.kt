package dnd.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** spec 2 */
class TripsTest {
    private val t0 = T.at(2026, 11, 4, 7, 0)

    @Test
    fun tripStartsAtFirstNonPSampleAndEndsAtFirstPSample() {
        val gears = listOf(Gear.P, Gear.P, Gear.D, Gear.D, Gear.N, Gear.P, Gear.P, Gear.R, Gear.D)
        val log = gears.mapIndexed { i, g -> T.sample(t0 + i, 0.0, gear = g) }
        val trips = segmentTrips(log)
        assertEquals(2, trips.size)
        assertEquals(t0 + 2, trips[0].startEpochSec)
        assertEquals(t0 + 5, trips[0].endEpochSec)
        assertEquals(listOf(t0 + 2, t0 + 3, t0 + 4), trips[0].samples.map { it.epochSec })
        assertEquals(t0 + 5, trips[0].endSample?.epochSec)
        assertEquals(3L, trips[0].times().durationSec)
        assertEquals(t0 + 7, trips[1].startEpochSec)
        assertFalse(trips[1].isClosed)
        assertNull(trips[1].endEpochSec)
    }

    @Test
    fun aLogThatStartsInDriveStartsATripAtItsFirstSample() {
        val log = listOf(Gear.D, Gear.D, Gear.P, Gear.P).mapIndexed { i, g -> T.sample(t0 + i, 0.0, gear = g) }
        val trips = segmentTrips(log)
        assertEquals(1, trips.size)
        assertEquals(TripTimes(t0, t0 + 2), trips[0].times())
    }

    @Test
    fun restOf899SecondsKeepsAdding() {
        val a = TripTimes(t0, t0 + 3600)
        val b = TripTimes(t0 + 3600 + 899, t0 + 3600 + 899 + 1800)
        val cd = continuousDriving(listOf(a, b))
        assertEquals(ContinuousDriving(0, 3600), cd[0])
        assertEquals(ContinuousDriving(3600, 5400), cd[1])
        assertFalse(restResets(899))
    }

    @Test
    fun restOf900SecondsResets() {
        val a = TripTimes(t0, t0 + 3600)
        val b = TripTimes(t0 + 3600 + 900, t0 + 3600 + 900 + 1800)
        val cd = continuousDriving(listOf(a, b))
        assertEquals(ContinuousDriving(0, 1800), cd[1])
        assertTrue(restResets(900))
    }

    @Test
    fun continuousDrivingChainsOverSeveralShortStops() {
        val trips = listOf(
            TripTimes(t0, t0 + 1000),
            TripTimes(t0 + 1100, t0 + 2100), // 100 s stop
            TripTimes(t0 + 2200, t0 + 2500), // 100 s stop
            TripTimes(t0 + 3400, t0 + 3500), // 900 s stop: reset
        )
        assertEquals(listOf(1000L, 2000L, 2300L, 100L), continuousDriving(trips).map { it.atEndSec })
    }

    @Test
    fun voicePromptWhenContinuousDrivingReachesTwoHours() {
        // 1.5 h, 10 min stop, then 1 h: 2 h is reached 30 min into the second trip
        val a = TripTimes(t0, t0 + 5400)
        val b = TripTimes(t0 + 6000, t0 + 9600)
        assertEquals(listOf(t0 + 7800), voicePromptInstants(listOf(a, b)))
        // with a 15 min stop the second trip starts from 0 and stays under 2 h
        val c = TripTimes(t0 + 6300, t0 + 9900)
        assertEquals(emptyList(), voicePromptInstants(listOf(a, c)))
        // one long trip: once, at start + 2 h
        assertEquals(listOf(t0 + 7200), voicePromptInstants(listOf(TripTimes(t0, t0 + 9600))))
        // a trip that ends exactly at 2 h gives no prompt; the next one (short stop) starts above 2 h
        assertEquals(emptyList(), voicePromptInstants(listOf(TripTimes(t0, t0 + 7200), TripTimes(t0 + 7300, t0 + 9000))))
    }
}
