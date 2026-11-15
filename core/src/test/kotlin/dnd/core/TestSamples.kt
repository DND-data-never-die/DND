package dnd.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Hand-made inputs for the micro-tests. Nothing here uses the demo generators. */
object T {
    fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int, s: Int = 0): Long =
        DndTime.epochSec(LocalDateTime.of(y, mo, d, h, mi, s))

    fun at(date: LocalDate, h: Int, mi: Int, s: Int = 0): Long = DndTime.epochSec(date, LocalTime.of(h, mi, s))

    fun sample(
        t: Long,
        speed: Double,
        gear: Gear = Gear.D,
        steeringDeg: Double = 0.0,
        tempC: Double = 10.0,
        odometerKm: Double = 0.0,
    ) = Sample(t, speed, gear, steeringDeg, odometerKm, tempC, 50.0, FluidLevel.NORMAL, FluidLevel.NORMAL, false)

    /** Consecutive 1 Hz samples starting at [t0]. */
    fun drive(t0: Long, vararg speeds: Double): List<Sample> = speeds.mapIndexed { i, v -> sample(t0 + i, v) }

    /** Samples at explicit times (to leave gaps). */
    fun timed(vararg pairs: Pair<Long, Double>): List<Sample> = pairs.map { (t, v) -> sample(t, v) }

    fun trip(
        start: Long,
        end: Long,
        dawnSec: Long = 0,
        continuousSec: Long = end - start,
        harshSum: Double = 0.0,
        harshCount: Int = 0,
        curve: Double = 0.0,
        congestionSec: Long = 0,
        distanceKm: Double = 10.0,
        stopStarts: Int = 0,
        subZeroSec: Long = 0,
        maxTempC: Double = 15.0,
        index: Double? = null,
    ) = TripSummary(
        startEpochSec = start,
        endEpochSec = end,
        distanceKm = distanceKm,
        continuousSec = continuousSec,
        dawnSec = dawnSec,
        maxOutsideTempC = maxTempC,
        subZeroSec = subZeroSec,
        harshCount = harshCount,
        harshMagnitudeSum = harshSum,
        stopStartCount = stopStarts,
        congestionSec = congestionSec,
        curveLoad = curve,
        endOdometerKm = 0.0,
        fatigueIndex = index,
    )
}
