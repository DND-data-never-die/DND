package dnd.core

/**
 * One trip cut from a signal log (spec 2).
 * [samples] are the driving samples, from the start sample up to (not including) the first P sample.
 * [endSample] is that first P sample; it is null while the trip is still open at the end of the log.
 */
data class TripSegment(
    val startEpochSec: Long,
    val endEpochSec: Long?,
    val samples: List<Sample>,
    val endSample: Sample?,
) {
    val isClosed: Boolean get() = endEpochSec != null

    fun times(): TripTimes = TripTimes(startEpochSec, checkNotNull(endEpochSec) { "trip is still open" })
}

/**
 * spec 2: a trip starts at the first non-P sample and ends at the first P sample after it.
 * A log whose first sample is already non-P starts a trip at that sample (it is the first non-P
 * sample of the log). A trip still running at the end of the log is returned open (end null).
 */
fun segmentTrips(log: List<Sample>): List<TripSegment> {
    for (i in 1 until log.size) {
        require(log[i].epochSec > log[i - 1].epochSec) { "samples must be in strictly increasing time order" }
    }
    val trips = mutableListOf<TripSegment>()
    var current: MutableList<Sample>? = null
    for (s in log) {
        val open = current
        if (open == null) {
            if (s.gear != Gear.P) current = mutableListOf(s)
        } else if (s.gear == Gear.P) {
            trips += TripSegment(open.first().epochSec, s.epochSec, open.toList(), s)
            current = null
        } else {
            open += s
        }
    }
    current?.let { trips += TripSegment(it.first().epochSec, null, it.toList(), null) }
    return trips
}

/** spec 2: a stop of 900 s or more ("15분 이상") resets continuous driving. */
const val REST_RESET_SEC = 900L

/** spec 2 */
fun restResets(gapSec: Long): Boolean = gapSec >= REST_RESET_SEC

/** Continuous driving carried into a trip from earlier trips, and the value at its end (spec 2). */
data class ContinuousDriving(val carryInSec: Long, val atEndSec: Long)

/**
 * spec 2: continuous driving adds trip times. A stop shorter than 900 s keeps adding (the stop
 * itself is not added); a stop of 900 s or more starts again from 0. The stop is the time from
 * the previous trip's end to this trip's start. [trips] must be in time order and must not overlap.
 * The first trip of the list has nothing to carry.
 */
fun continuousDriving(trips: List<TripTimes>): List<ContinuousDriving> {
    val out = ArrayList<ContinuousDriving>(trips.size)
    for ((k, t) in trips.withIndex()) {
        val carry = if (k == 0) {
            0L
        } else {
            val prev = trips[k - 1]
            val gap = t.startEpochSec - prev.endEpochSec
            require(gap >= 0) { "trips overlap" }
            if (restResets(gap)) 0L else out[k - 1].atEndSec
        }
        out += ContinuousDriving(carry, carry + t.durationSec)
    }
    return out
}

/** spec 6: the voice prompt plays once when continuous driving reaches 2 hours. */
const val VOICE_PROMPT_CONTINUOUS_SEC = 7_200L

/**
 * spec 6: the instants at which continuous driving reaches 2 hours while driving.
 * Continuous driving at instant t inside trip k is carryIn(k) + (t - start(k)).
 * SPEC GAP: a trip that ends exactly when the total reaches 2 hours gives no prompt (the car is
 * already in P at that instant).
 */
fun voicePromptInstants(trips: List<TripTimes>): List<Long> {
    val cd = continuousDriving(trips)
    return trips.indices.mapNotNull { k ->
        val carry = cd[k].carryInSec
        val t = trips[k]
        if (carry < VOICE_PROMPT_CONTINUOUS_SEC && carry + t.durationSec > VOICE_PROMPT_CONTINUOUS_SEC) {
            t.startEpochSec + (VOICE_PROMPT_CONTINUOUS_SEC - carry)
        } else {
            null
        }
    }
}
