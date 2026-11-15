package dnd.core.demo

import dnd.core.DndTime
import dnd.core.FluidLevel
import dnd.core.Gear
import dnd.core.MaintenanceItem
import dnd.core.MaintenanceRecord
import dnd.core.Sample
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToLong

/**
 * Scenario 1 (기획서 4, 상황 1): the 1 Hz signal log of a Daegu-Seoul business trip on 2026-11-12,
 * 13:00:00 to 17:10:00. Synthetic and deterministic (spec 10).
 * - Leg 1: 13:00:00 (first sample, gear D) to 14:15:00 (first P sample): city start, highway.
 * - Rest stop in P: 14:15:00 to 14:29:59 (900 s, so continuous driving restarts from 0, spec 2).
 * - Leg 2: 14:30:00 (first non-P sample) to 17:10:00 (first P sample): highway, a jam near Seoul,
 *   an urban expressway, city traffic, arrival.
 * Leg 2 carries 9 harsh events (4 on the highway, 2 in the jam, 3 in the city; magnitudes
 * 16, 9, 16 (two merged seconds 16 and 15), 10, 12, 15, 11, 11, 15) and two congestion runs of
 * 1,680 s and 1,200 s (48 min together).
 */
object Scenario1Log {
    /** Scenario "today". */
    val DATE: LocalDate = LocalDate.of(2026, 11, 12)

    val LOG_START: Long = DndTime.epochSec(DATE, LocalTime.of(13, 0))
    val LEG1_END: Long = LOG_START + 4_500 // 14:15:00
    val LEG2_START: Long = LOG_START + 5_400 // 14:30:00
    val LEG2_END: Long = LOG_START + 15_000 // 17:10:00

    /** Odometer at 17:10:00, 13,150 km after the last oil change. */
    const val END_ODOMETER_KM = 42_380.0
    val LAST_OIL_CHANGE = MaintenanceRecord(MaintenanceItem.ENGINE_OIL, LocalDate.of(2025, 12, 20), 29_230.0)

    private const val LEG1_SEC = 4_500
    private const val LEG2_SEC = 9_600
    private const val REST_SEC = 900
    /**
     * The road ([RoadProfile]): city streets, the highway and the urban expressway each have curves
     * along distance, and the steering of every second is the angle of the road where the car is
     * ([Track.steeringAlong]); intersection turns at 15 km/h come on top. Front wheels on the
     * highway 0.23, 0.17, 0.11 and 0.08 degrees, on the urban expressway 0.59, in city curves 1.18
     * and 0.83. The baseline's curve loads are on the same scale ([Scenario1Data.CURVE_SCALE]).
     */
    private const val CITY_TURN_DEG = 25.0

    private fun leg1(): Track {
        val head = Track(road = RoadProfile.CITY).apply {
            hold(0.0, 5)
            jump(11.0) // 급출발 11
            rampTo(24.0, 2.0)
            hold(24.0, 40)
            rampTo(0.0, 2.0)
            hold(0.0, 25)
            stopGo(20.0, 50, 30, turnDeg = CITY_TURN_DEG)
            stopGo(30.0, 60, 20)
            stopGo(18.0, 40, 35, turnDeg = -CITY_TURN_DEG)
            stopGo(25.0, 45, 15)
            rampTo(40.0, 2.0)
            road = RoadProfile.HIGHWAY
            rampTo(100.0, 3.0)
            cruise(100.0, 900)
            jump(-16.0) // 급감속 16
            rampTo(100.0, 2.0)
            cruise(100.0, 1_200)
            jump(9.0) // 급가속 9
            rampTo(100.0, 1.0)
        }
        val tail = Track(100.0, RoadProfile.HIGHWAY).apply {
            rampTo(0.0, 2.0) // into the rest area
            hold(0.0, 20)
        }
        return Track(road = RoadProfile.HIGHWAY).apply {
            append(head)
            cruise(100.0, LEG1_SEC - head.size - tail.size)
            append(tail)
            check(size == LEG1_SEC)
        }
    }

    private fun leg2(): Track {
        val head = Track(road = RoadProfile.HIGHWAY).apply {
            hold(0.0, 10)
            rampTo(40.0, 2.0)
            rampTo(100.0, 3.0)
            cruise(100.0, 1_500)
            jump(-16.0) // 급감속 16
            rampTo(100.0, 2.0)
            cruise(100.0, 1_200)
            jump(9.0) // 급가속 9
            rampTo(100.0, 1.0)
            cruise(100.0, 1_200)
            jump(-16.0) // 급감속: -16 then -15, one event of magnitude 16
            jump(-15.0)
            rampTo(100.0, 2.0)
            cruise(100.0, 900)
            jump(10.0) // 급가속 10
            rampTo(100.0, 1.0)
        }
        val tail = Track(100.0, RoadProfile.HIGHWAY).apply {
            rampTo(42.0, 2.0)
            lowSpeedRun(1_680) { // jam near Seoul: 28 min
                stopGo(15.0, 90, 40)
                jump(12.0) // 급출발 12
                rampTo(20.0, 2.0)
                hold(20.0, 80)
                rampTo(0.0, 2.0)
                hold(0.0, 45)
                stopGo(22.0, 120, 30)
                stopGo(10.0, 60, 50)
                rampTo(18.0, 2.0)
                hold(18.0, 100)
                jump(-15.0) // 급정지 15 (to 3 km/h)
                add(0.0)
                hold(0.0, 40)
                stopGo(25.0, 110, 35)
                stopGo(12.0, 70, 45)
                stopGo(20.0, 90, 30)
                stopGo(16.0, 80, 40)
                stopGo(24.0, 100, 30)
            }
            road = RoadProfile.URBAN
            rampTo(80.0, 2.0)
            hold(80.0, 600)
            rampTo(42.0, 2.0)
            road = RoadProfile.CITY
            lowSpeedRun(1_200) { // city: 20 min
                stopGo(30.0, 60, 40, turnDeg = CITY_TURN_DEG)
                jump(11.0) // 급출발 11
                rampTo(25.0, 2.0)
                hold(25.0, 40)
                rampTo(15.0, 2.0)
                turn(15.0, 6, -CITY_TURN_DEG)
                rampTo(0.0, 2.0)
                hold(0.0, 30)
                rampTo(15.0, 2.0)
                hold(15.0, 10)
                jump(11.0) // 급가속 11 (from 15 km/h, band 11-20)
                rampTo(30.0, 2.0)
                hold(30.0, 40)
                rampTo(0.0, 2.0)
                hold(0.0, 30)
                stopGo(20.0, 50, 35, turnDeg = CITY_TURN_DEG)
                rampTo(18.0, 2.0)
                hold(18.0, 30)
                jump(-15.0) // 급정지 15
                add(0.0)
                hold(0.0, 35)
                stopGo(28.0, 70, 40, turnDeg = -CITY_TURN_DEG)
                stopGo(22.0, 60, 30)
            }
            rampTo(50.0, 2.0)
            hold(50.0, 120)
            rampTo(0.0, 2.0) // arrival
            hold(0.0, 10)
        }
        return Track(road = RoadProfile.HIGHWAY).apply {
            append(head)
            cruise(100.0, LEG2_SEC - head.size - tail.size)
            append(tail)
            check(size == LEG2_SEC)
        }
    }

    /** The full log, 15,001 samples from 13:00:00 to 17:10:00 inclusive. */
    fun signalLog(): List<Sample> {
        val all = Track()
        val gears = ArrayList<Gear>()
        fun put(t: Track, gear: Gear) {
            all.append(t)
            repeat(t.size) { gears += gear }
        }
        put(leg1(), Gear.D)
        put(Track(road = RoadProfile.HIGHWAY).apply { hold(0.0, REST_SEC) }, Gear.P)
        put(leg2(), Gear.D)
        put(Track(road = RoadProfile.CITY).apply { hold(0.0, 1) }, Gear.P)
        val speeds = all.speeds
        val steering = Track.steeringAlong(all.speeds, all.roads, all.turns).toMutableList()
        check(speeds.size == 15_001)

        // A parked car's wheels are straight (owner, 2026-11-15): every second in P, and the standstill
        // next to it or at the log's start (the parking place, which is not on the road's curve).
        val parked = BooleanArray(speeds.size) { gears[it] == Gear.P }
        if (speeds[0] == 0.0) parked[0] = true
        for (i in 1 until speeds.size) if (speeds[i] == 0.0 && parked[i - 1]) parked[i] = true
        for (i in speeds.size - 2 downTo 0) if (speeds[i] == 0.0 && parked[i + 1]) parked[i] = true
        for (i in speeds.indices) if (parked[i]) steering[i] = 0.0

        // Odometer in 0.1 km steps from the integrated speed, fixed so the last sample reads END_ODOMETER_KM.
        val cum = DoubleArray(speeds.size)
        for (i in 1 until speeds.size) cum[i] = cum[i - 1] + speeds[i] / 3_600.0
        val endTenths = (END_ODOMETER_KM * 10).roundToLong()
        return speeds.indices.map { i ->
            val odoTenths = endTenths - ((cum.last() - cum[i]) * 10).roundToLong()
            Sample(
                epochSec = LOG_START + i,
                speedKmh = speeds[i],
                gear = gears[i],
                steeringDeg = steering[i],
                odometerKm = odoTenths / 10.0,
                outsideTempC = ((150.0 - 30.0 * i / 15_000.0)).roundToLong() / 10.0, // 15.0 -> 12.0 degrees
                fuelPercent = (780.0 - 0.9 * cum[i]).roundToLong() / 10.0,
                engineOil = FluidLevel.NORMAL,
                brakeFluid = FluidLevel.NORMAL,
                washerLow = false,
            )
        }
    }
}
