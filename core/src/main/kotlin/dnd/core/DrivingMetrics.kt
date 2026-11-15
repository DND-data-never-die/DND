package dnd.core

import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.tan

// ---------- spec 3-2: stop-start count ----------

/**
 * spec 3-2: indices i where the speed became 0 and then rose above 0 again (samples[i - 1] is 0,
 * samples[i] is above 0, and the car had moved earlier in the trip). The first departure of the
 * trip is not counted. SPEC GAP: the count follows the sample order; it does not need Δv, so a
 * missing sample does not break it.
 */
fun stopStartIndices(samples: List<Sample>): List<Int> {
    val out = mutableListOf<Int>()
    var movedBefore = false
    var stopped = false
    for ((i, s) in samples.withIndex()) {
        if (s.speedKmh > 0.0) {
            if (stopped) out += i
            movedBefore = true
            stopped = false
        } else if (movedBefore) {
            stopped = true
        }
    }
    return out
}

/** spec 3-2 */
fun stopStartCount(samples: List<Sample>): Int = stopStartIndices(samples).size

// ---------- spec 3-3: congestion time ----------

/** spec 3-3: 40 km/h, the speed Bitkina et al. (2019) used to separate congested sections. */
const val CONGESTION_MAX_KMH = 40.0

/** spec 3-3: "가다 서다를 반복" = two or more stop-starts. */
const val CONGESTION_MIN_STOP_STARTS = 2

/** A run of consecutive samples at 40 km/h or less, with the stop-starts inside it. */
data class LowSpeedRun(val fromIndex: Int, val toIndex: Int, val stopStarts: Int) {
    /** Each 1 Hz sample stands for one second. */
    val lengthSec: Long get() = (toIndex - fromIndex + 1).toLong()
    val isCongestion: Boolean get() = stopStarts >= CONGESTION_MIN_STOP_STARTS
}

/**
 * spec 3-3: maximal runs of samples at 40 km/h or less. A stop-start (spec 3-2) is inside a run
 * when both of its samples are. SPEC GAP: a missing sample (two samples more than 1 s apart)
 * ends a run, as it does for Δv in spec 3.
 */
fun lowSpeedRuns(samples: List<Sample>): List<LowSpeedRun> {
    val starts = stopStartIndices(samples).toSet()
    val runs = mutableListOf<LowSpeedRun>()
    var from = -1
    var count = 0
    for ((i, s) in samples.withIndex()) {
        val low = s.speedKmh <= CONGESTION_MAX_KMH
        val continues = from >= 0 && low && s.epochSec - samples[i - 1].epochSec == 1L
        if (continues) {
            if (i in starts) count++
        } else {
            if (from >= 0) runs += LowSpeedRun(from, i - 1, count)
            from = if (low) i else -1
            count = 0
        }
    }
    if (from >= 0) runs += LowSpeedRun(from, samples.lastIndex, count)
    return runs
}

/** spec 3-3: total length of the low-speed runs that hold two or more stop-starts, in seconds. */
fun congestionSeconds(samples: List<Sample>): Long = lowSpeedRuns(samples).filter { it.isCongestion }.sumOf { it.lengthSec }

// ---------- spec 3-4: curve load ----------

/** spec 3-4: Grandeur wheelbase 2,895 mm (2027 Grandeur owner's manual, dimensions). */
const val GRANDEUR_WHEELBASE_M = 2.895

/** spec 3-4: a = v^2 * tan(delta) / L, v in m/s, delta the front-wheel angle (degrees converted to radians). */
fun lateralAccel(speedKmh: Double, steeringDeg: Double, wheelbaseM: Double): Double {
    require(wheelbaseM > 0.0) { "wheelbase must be positive" }
    val v = speedKmh / 3.6 // spec 1: km/h = m/s x 3.6
    return v * v * tan(Math.toRadians(steeringDeg)) / wheelbaseM
}

/** spec 3-4: curve load = sum over seconds of |a|, in m/s. */
fun curveLoad(samples: List<Sample>, wheelbaseM: Double): Double =
    samples.sumOf { abs(lateralAccel(it.speedKmh, it.steeringDeg, wheelbaseM)) }

// ---------- spec 3-5: time and temperature ----------

/** spec 3-5: dawn window 02:00-05:00 (Connor et al. 2002). */
val DAWN_START: LocalTime = LocalTime.of(2, 0)
val DAWN_END: LocalTime = LocalTime.of(5, 0)

/**
 * spec 3-5: the part of [startEpochSec, endEpochSec) inside 02:00-05:00 of every local date the
 * trip touches, in seconds.
 */
fun dawnSeconds(startEpochSec: Long, endEpochSec: Long): Long {
    require(endEpochSec >= startEpochSec) { "end before start" }
    var total = 0L
    var date = DndTime.date(startEpochSec)
    val lastDate = DndTime.date(endEpochSec)
    while (!date.isAfter(lastDate)) {
        val w0 = DndTime.epochSec(date, DAWN_START)
        val w1 = DndTime.epochSec(date, DAWN_END)
        total += maxOf(0L, minOf(endEpochSec, w1) - maxOf(startEpochSec, w0))
        date = date.plusDays(1)
    }
    return total
}

/** spec 3-5: heat trip when the highest outside temperature is 33 degrees or more. */
const val HEAT_MIN_C = 33.0

fun isHeatTrip(maxOutsideTempC: Double): Boolean = maxOutsideTempC >= HEAT_MIN_C

/** spec 3-5: seconds with the outside temperature below 0 degrees (one sample = one second). */
fun subZeroSeconds(samples: List<Sample>): Long = samples.count { it.outsideTempC < 0.0 }.toLong()

// ---------- one trip ----------

/**
 * SPEC GAP: the spec does not define per-trip distance; it is the odometer at the end (the first
 * P sample) minus the odometer at the start sample.
 */
fun tripDistanceKm(trip: TripSegment): Double {
    val end = trip.endSample ?: trip.samples.last()
    return end.odometerKm - trip.samples.first().odometerKm
}

/** Per-trip values from a closed trip of the signal log (spec 2, 3). */
fun summarizeTrip(
    trip: TripSegment,
    continuous: ContinuousDriving,
    wheelbaseM: Double,
    fatigueIndex: Double?,
): TripSummary {
    val times = trip.times()
    val harsh = detectHarshEvents(trip.samples)
    return TripSummary(
        startEpochSec = times.startEpochSec,
        endEpochSec = times.endEpochSec,
        distanceKm = tripDistanceKm(trip),
        continuousSec = continuous.atEndSec,
        dawnSec = dawnSeconds(times.startEpochSec, times.endEpochSec),
        maxOutsideTempC = trip.samples.maxOf { it.outsideTempC },
        subZeroSec = subZeroSeconds(trip.samples),
        harshCount = harsh.count,
        harshMagnitudeSum = harsh.magnitudeSum,
        stopStartCount = stopStartCount(trip.samples),
        congestionSec = congestionSeconds(trip.samples),
        curveLoad = curveLoad(trip.samples, wheelbaseM),
        endOdometerKm = (trip.endSample ?: trip.samples.last()).odometerKm,
        fatigueIndex = fatigueIndex,
    )
}
