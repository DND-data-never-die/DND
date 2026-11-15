package dnd.core.demo

import dnd.core.CONGESTION_MAX_KMH
import dnd.core.GRANDEUR_WHEELBASE_M
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Builds a 1 Hz speed sequence for the synthetic demo logs, each second tagged with the road it is
 * driven on ([road]) and, at an intersection, the turn's front-wheel angle. No randomness: every
 * sample comes from the calls below. Gentle ramps change speed by 2 or 3 km/h per second, well
 * under every eTAS threshold (spec 3-1); harsh events are single explicit jumps. The steering of the
 * other seconds comes from the road's curvature at the car's position ([steeringAlong]).
 */
internal class Track(private val initialSpeed: Double = 0.0, var road: RoadProfile = RoadProfile.CITY) {
    val speeds = ArrayList<Double>()
    val roads = ArrayList<RoadProfile>()
    /** The front-wheel angle of an intersection turn, or null where the road's curvature decides. */
    val turns = ArrayList<Double?>()

    val size: Int get() = speeds.size
    val last: Double get() = speeds.lastOrNull() ?: initialSpeed

    fun add(speed: Double, turnDeg: Double? = null) {
        speeds += speed
        roads += road
        turns += turnDeg
    }

    fun hold(speed: Double, seconds: Int) = repeat(seconds) { add(speed) }

    /** Steps of [step] km/h per second; the last sample lands exactly on [target]. */
    fun rampTo(target: Double, step: Double) {
        var cur = last
        while (cur != target) {
            cur = if (target > cur) minOf(cur + step, target) else maxOf(cur - step, target)
            add(cur)
        }
    }

    /** One sample at the previous speed plus [delta]: a harsh event when it crosses a spec 3-1 threshold. */
    fun jump(delta: Double) = add(last + delta)

    /** An intersection turn: [seconds] samples at [speed] with the front wheels at [steeringDeg]. */
    fun turn(speed: Double, seconds: Int, steeringDeg: Double) = repeat(seconds) { add(speed, steeringDeg) }

    /**
     * Cruise at about [speed]: the speed drifts by up to [DRIFT_KMH] in about [DRIFT_PERIOD_SEC]-second
     * waves, starting and ending exactly at [speed] and antisymmetric about the middle (d(n-1-k) =
     * -d(k)), so the run's distance and the samples around it stay exactly as at a constant speed.
     */
    fun cruise(speed: Double, seconds: Int) {
        val d = DoubleArray(seconds)
        if (seconds > 2) {
            val n1 = seconds - 1
            val waves = maxOf(1, (n1.toDouble() / DRIFT_PERIOD_SEC).roundToInt())
            for (k in 0..n1 / 2) d[k] = DRIFT_KMH * sin(2 * PI * waves * k / n1)
            for (k in 0..n1 / 2) d[n1 - k] = -d[k]
            if (n1 % 2 == 0) d[n1 / 2] = 0.0
            d[0] = 0.0
            d[n1] = 0.0
        }
        for (k in 0 until seconds) add(speed + d[k])
    }

    /**
     * A stop-and-go cycle from standstill: depart, drive at [speed] for [driveSec], stop, stand for
     * [standSec]. With [turnDeg] the car slows to [TURN_KMH] at [TURN_RAMP] km/h per second, turns
     * for [TURN_SEC] seconds and speeds up again, inside the same [driveSec].
     */
    fun stopGo(speed: Double, driveSec: Int, standSec: Int, turnDeg: Double = 0.0) {
        rampTo(speed, 2.0)
        if (turnDeg != 0.0) {
            val first = driveSec / 2
            val ramp = ceil(maxOf(speed - TURN_KMH, 0.0) / TURN_RAMP).toInt()
            hold(speed, first - ramp)
            rampTo(TURN_KMH, TURN_RAMP)
            turn(TURN_KMH, TURN_SEC, turnDeg)
            rampTo(speed, TURN_RAMP)
            hold(speed, driveSec - first - TURN_SEC - ramp)
        } else {
            hold(speed, driveSec)
        }
        rampTo(0.0, 2.0)
        hold(0.0, standSec)
    }

    /**
     * A run of exactly [lengthSec] samples at 40 km/h or less (spec 3-3): enters at 40 from a
     * speed above 40, stops, runs [body] (which must end at standstill) and climbs back to 40.
     * The first stop is lengthened to reach the exact length.
     */
    fun lowSpeedRun(lengthSec: Int, body: Track.() -> Unit) {
        require(last > CONGESTION_MAX_KMH) { "a run is entered from above 40 km/h" }
        val start = size
        add(CONGESTION_MAX_KMH)
        rampTo(0.0, 2.0)
        val padAt = size
        body()
        require(last == 0.0) { "the run body ends at standstill" }
        rampTo(CONGESTION_MAX_KMH, 2.0)
        val pad = lengthSec - (size - start)
        require(pad >= 0) { "run content is longer than $lengthSec s" }
        val padRoad = roads[padAt]
        repeat(pad) {
            speeds.add(padAt, 0.0)
            roads.add(padAt, padRoad)
            turns.add(padAt, null)
        }
        check(size - start == lengthSec)
        check((start until size).all { speeds[it] <= CONGESTION_MAX_KMH })
    }

    fun append(other: Track) {
        speeds += other.speeds
        roads += other.roads
        turns += other.turns
    }

    companion object {
        /**
         * An intersection turn with the front wheels at 25 degrees is driven at 15 km/h:
         * a = v^2 tan(25 deg) / 2.895 m = 2.8 m/s^2 (at 30 km/h it would be 11.2, more than a car can do).
         */
        const val TURN_KMH = 15.0
        const val TURN_SEC = 4
        /** A gentle ramp (the class's 2 or 3 km/h per second). */
        const val TURN_RAMP = 3.0

        /** Cruise drift: +-3 km/h in waves of about 10 minutes; under 0.05 km/h per second. */
        const val DRIFT_KMH = 3.0
        const val DRIFT_PERIOD_SEC = 600

        /**
         * The front-wheel angle of every second: an intersection turn's own angle, otherwise the angle
         * of the road at the position the car has reached, atan(wheelbase / R) in a curve (sides
         * alternating, right first) and 0 on a tangent. The position advances by speed / 3.6 metres
         * per second, so a car standing still keeps the angle of the place where it stands.
         */
        fun steeringAlong(speeds: List<Double>, roads: List<RoadProfile>, turns: List<Double?>): List<Double> {
            var pos = 0.0
            return speeds.indices.map { i ->
                pos += speeds[i] / 3.6
                turns[i] ?: roads[i].angleAt(pos)
            }
        }
    }
}

/**
 * A road as a curvature profile along distance: curves of [radiiM] (in turn) [curveM] metres long,
 * [tangentM] metres of straight between them, sides alternating.
 */
internal data class RoadProfile(val radiiM: List<Double>, val curveM: Double, val tangentM: Double) {
    fun angleAt(positionM: Double): Double {
        val segment = curveM + tangentM
        val n = (positionM / segment).toInt()
        if (positionM - n * segment >= curveM) return 0.0
        val sign = if (n % 2 == 0) 1.0 else -1.0
        return sign * Math.toDegrees(atan(GRANDEUR_WHEELBASE_M / radiiM[n % radiiM.size]))
    }

    companion object {
        // Radii from 「도로의 구조·시설 기준에 관한 규칙」 (국토교통부령 제1588호) 제19조 (minimum radius by
        // design speed at 6 % maximum superelevation, 제21조), curve lengths above 제20조's minimum
        // (deflection 5 degrees or more). The segment lengths (1,240, 350 and 220 m) are chosen so
        // that samples 20 s and 120 s apart do not fall on one phase of the cycle.

        /** Highway, design speed 120 km/h: 710 m (the minimum), 1,000, 1,500, 2,000 m; curves 1,100 m >= 140 m. */
        val HIGHWAY = RoadProfile(listOf(710.0, 1_000.0, 1_500.0, 2_000.0), curveM = 1_100.0, tangentM = 140.0)

        /** Urban expressway, design speed 80 km/h: 280 m (the minimum); curves 230 m >= 90 m. */
        val URBAN = RoadProfile(listOf(280.0), curveM = 230.0, tangentM = 120.0)

        /** City streets, design speed 60 and 70 km/h: 140 and 200 m (their minimums); curves 150 m >= 70 m. */
        val CITY = RoadProfile(listOf(140.0, 200.0), curveM = 150.0, tangentM = 70.0)
    }
}
