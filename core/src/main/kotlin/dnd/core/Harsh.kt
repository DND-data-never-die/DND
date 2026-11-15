package dnd.core

/** spec 3-1: the four eTAS events (taxi criteria). */
enum class HarshType(val label: String, val isAcceleration: Boolean) {
    SUDDEN_START("급출발", true),
    SUDDEN_ACCEL("급가속", true),
    SUDDEN_DECEL("급감속", false),
    SUDDEN_STOP("급정지", false),
}

/**
 * One counted event. [firstEpochSec]..[lastEpochSec] are the event seconds merged into it (the
 * time of the later sample of each Δv pair). [magnitude] is the largest |Δv| among them, km/h/s.
 */
data class HarshEvent(
    val type: HarshType,
    val firstEpochSec: Long,
    val lastEpochSec: Long,
    val magnitude: Double,
)

data class HarshSummary(val events: List<HarshEvent>) {
    /** spec 3-1: the card shows the sum of the four counts. */
    val count: Int get() = events.size

    /** spec 3-1: the fatigue factor value is the sum of event magnitudes. */
    val magnitudeSum: Double get() = events.sumOf { it.magnitude }

    fun count(type: HarshType): Int = events.count { it.type == type }
}

/** spec 3-1 table, eTAS taxi criteria. Speeds in km/h, Δv thresholds in km/h per second. */
object HarshCriteria {
    /** 급출발: from a speed of 5.0 km/h or less (previous second), accelerating 10 km/h/s or more. */
    const val START_MAX_PREV_KMH = 5.0
    const val START_MIN_DV = 10.0

    /** 급가속 bands on the previous second's speed rounded to an integer: 6-10, 11-20, 21 and above. */
    fun accelThreshold(bandKmh: Long): Double? = when {
        bandKmh in 6L..10L -> 12.0
        bandKmh in 11L..20L -> 10.0
        bandKmh >= 21L -> 8.0
        else -> null
    }

    /** 급감속 bands on the previous second's speed rounded to an integer: 30 or less -> 14, above 30 -> 15. */
    fun decelThreshold(bandKmh: Long): Double = if (bandKmh <= 30L) 14.0 else 15.0

    /** 급감속 needs this second's speed at 6.0 km/h or more. */
    const val DECEL_MIN_CUR_KMH = 6.0

    /** 급정지: decelerating 14 km/h/s or more to a speed (this second) of 5.0 km/h or less. */
    const val STOP_MIN_DECEL = 14.0
    const val STOP_MAX_CUR_KMH = 5.0
}

/**
 * spec 3-1: classify one second from the previous and current speed. Bands use the previous
 * second's speed rounded half up to an integer; 급출발 uses the previous speed unrounded; 급감속 and
 * 급정지 test this second's speed unrounded. Speeds in (5.0, 6.0) after a hard deceleration and a
 * previous speed in (5.0, 5.5) before a hard acceleration give no event: the eTAS bands have these gaps.
 */
fun classifyHarshSecond(prevKmh: Double, curKmh: Double): HarshType? {
    val dv = curKmh - prevKmh
    val band = Fmt.roundHalfUp(prevKmh)
    if (dv > 0) {
        if (prevKmh <= HarshCriteria.START_MAX_PREV_KMH) {
            return if (dv >= HarshCriteria.START_MIN_DV) HarshType.SUDDEN_START else null
        }
        val threshold = HarshCriteria.accelThreshold(band) ?: return null
        return if (dv >= threshold) HarshType.SUDDEN_ACCEL else null
    }
    if (dv < 0) {
        val decel = -dv
        if (curKmh <= HarshCriteria.STOP_MAX_CUR_KMH) {
            return if (decel >= HarshCriteria.STOP_MIN_DECEL) HarshType.SUDDEN_STOP else null
        }
        if (curKmh >= HarshCriteria.DECEL_MIN_CUR_KMH && decel >= HarshCriteria.decelThreshold(band)) {
            return HarshType.SUDDEN_DECEL
        }
    }
    return null
}

/**
 * spec 3-1 counting: Δv exists only between two samples exactly 1 s apart (spec 3). Consecutive
 * event seconds of the same direction are one event: a run that contains 급출발 is 급출발, a run that
 * contains 급정지 is 급정지, otherwise 급가속 or 급감속. The magnitude is the largest |Δv| of the run.
 */
fun detectHarshEvents(samples: List<Sample>): HarshSummary {
    data class Second(val epochSec: Long, val type: HarshType, val absDv: Double)

    val seconds = mutableListOf<Second>()
    for (i in 1 until samples.size) {
        val prev = samples[i - 1]
        val cur = samples[i]
        if (cur.epochSec - prev.epochSec != 1L) continue
        val type = classifyHarshSecond(prev.speedKmh, cur.speedKmh) ?: continue
        seconds += Second(cur.epochSec, type, kotlin.math.abs(cur.speedKmh - prev.speedKmh))
    }
    val events = mutableListOf<HarshEvent>()
    var run = mutableListOf<Second>()
    fun flush() {
        if (run.isEmpty()) return
        val accel = run.first().type.isAcceleration
        val type = if (accel) {
            if (run.any { it.type == HarshType.SUDDEN_START }) HarshType.SUDDEN_START else HarshType.SUDDEN_ACCEL
        } else {
            if (run.any { it.type == HarshType.SUDDEN_STOP }) HarshType.SUDDEN_STOP else HarshType.SUDDEN_DECEL
        }
        events += HarshEvent(type, run.first().epochSec, run.last().epochSec, run.maxOf { it.absDv })
        run = mutableListOf()
    }
    for (s in seconds) {
        val last = run.lastOrNull()
        if (last != null && (s.epochSec - last.epochSec != 1L || s.type.isAcceleration != last.type.isAcceleration)) flush()
        run += s
    }
    flush()
    return HarshSummary(events)
}
