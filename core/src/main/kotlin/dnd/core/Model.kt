package dnd.core

import java.time.LocalDate

/** spec 1: gear selection P, R, N, D. */
enum class Gear { P, R, N, D }

/** spec 1: engine-oil and brake-fluid level states ("LOW, NORMAL 등"). */
enum class FluidLevel { LOW, NORMAL }

/**
 * One 1 Hz sample (spec 1). Speed is in km/h (the car reports m/s; the collector multiplies by 3.6).
 * SPEC GAP: the spec does not say how a reverse (negative) speed is stored; the engine takes the
 * speed magnitude, so the collector must store |speed|.
 */
data class Sample(
    val epochSec: Long,
    val speedKmh: Double,
    val gear: Gear,
    /** Front-wheel steering angle in degrees. */
    val steeringDeg: Double,
    val odometerKm: Double,
    val outsideTempC: Double,
    val fuelPercent: Double,
    val engineOil: FluidLevel,
    val brakeFluid: FluidLevel,
    val washerLow: Boolean,
)

/** Start and end instants of a closed trip (spec 2). */
data class TripTimes(val startEpochSec: Long, val endEpochSec: Long) {
    init {
        require(endEpochSec > startEpochSec) { "trip must end after it starts" }
    }

    val durationSec: Long get() = endEpochSec - startEpochSec
}

/**
 * Everything the cards need from one finished trip. Values are kept in seconds and unrounded;
 * rounding happens only when formatting (spec 1).
 */
data class TripSummary(
    val startEpochSec: Long,
    val endEpochSec: Long,
    val distanceKm: Double,
    /** spec 2: continuous driving time at this trip's end. */
    val continuousSec: Long,
    /** spec 3-5: time inside 02:00-05:00. */
    val dawnSec: Long,
    /** spec 3-5: highest outside temperature during the trip. */
    val maxOutsideTempC: Double,
    /** spec 3-5: time with outside temperature below 0 degrees. */
    val subZeroSec: Long,
    /** spec 3-1: number of harsh events (card value). */
    val harshCount: Int,
    /** spec 3-1: sum of harsh event magnitudes in km/h/s (fatigue factor value). */
    val harshMagnitudeSum: Double,
    /** spec 3-2 */
    val stopStartCount: Int,
    /** spec 3-3 */
    val congestionSec: Long,
    /** spec 3-4: sum of |lateral acceleration| per second, m/s. */
    val curveLoad: Double,
    val endOdometerKm: Double,
    /** spec 5: fatigue index computed and stored when the trip ended; null while the baseline was collecting. */
    val fatigueIndex: Double?,
) {
    init {
        require(endEpochSec > startEpochSec) { "trip must end after it starts" }
    }

    val times: TripTimes get() = TripTimes(startEpochSec, endEpochSec)
    val durationSec: Long get() = endEpochSec - startEpochSec

    /** spec 2: a trip belongs to the date of its start. */
    val date: LocalDate get() = DndTime.date(startEpochSec)

    /** spec 3-5 */
    val isHeat: Boolean get() = isHeatTrip(maxOutsideTempC)
}
