package dnd.core.demo

import dnd.core.DndTime
import dnd.core.FluidLevel
import dnd.core.MaintenanceItem
import dnd.core.MaintenanceRecord
import dnd.core.TripSummary
import dnd.core.TripTimes
import dnd.core.VehicleState
import dnd.core.continuousDriving
import dnd.core.dawnSeconds
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Scenario 2 (기획서 4, 상황 2): a city driver. The evening trip of 2026-11-12 (Thursday) ends the
 * card; the 41 trips of the 28 days before it are given as trip summaries (spec 10).
 *
 * How the table was made (offline, once; frozen here): the Python random module (seed 41) drew weekday
 * commute and Saturday errand start times from 2026-10-16 to 2026-11-12, dropped extras down to
 * 41, drew 20 distances below 6 km, one of exactly 6.0 km and 20 above (median 6.0), stop-starts
 * near 2.7 per km adjusted to sum to 738 (mean 18.0), durations at 17-24 km/h, congestion at
 * 15-45 % of the trip, 0-3 harsh events. Two November morning trips got sub-zero minutes.
 *
 * Example values Claude chose (not from the application): 3,590 km driven since the last change
 * on 2025-12-05 (10.5 km a day in the table, about 3,590 km over 342 days), so 11,410 km remain
 * under the normal interval and 3,910 km under the severe one.
 */
internal object Scenario2Data {
    /** Scenario "today". */
    val DATE: LocalDate = LocalDate.of(2026, 11, 12)

    val LAST_OIL_CHANGE = MaintenanceRecord(MaintenanceItem.ENGINE_OIL, LocalDate.of(2025, 12, 5), 31_110.0)
    const val DRIVEN_SINCE_CHANGE_KM = 3_590.0
    val END_ODOMETER_KM: Double = LAST_OIL_CHANGE.odometerKm + DRIVEN_SINCE_CHANGE_KM

    private data class Seed(
        val date: LocalDate,
        val start: LocalTime,
        val durationSec: Long,
        val distanceKm: Double,
        val stopStarts: Int,
        val congestionSec: Long,
        val harshCount: Int,
        val harshSum: Double,
        val subZeroSec: Long,
        val maxTempC: Double,
    )

    private fun seed(
        date: String, start: String, durationSec: Long, distanceKm: Double, stopStarts: Int, congestionSec: Long,
        harshCount: Int, harshSum: Double, subZeroSec: Long, maxTempC: Double,
    ) = Seed(LocalDate.parse(date), LocalTime.parse(start), durationSec, distanceKm, stopStarts, congestionSec, harshCount, harshSum, subZeroSec, maxTempC)

    // date, start, duration s, distance km, stop-starts, congestion s, harsh count, harsh sum, sub-zero s, max temp C
    private val SEEDS = listOf(
        seed("2026-10-16", "08:18:00", 1002, 5.3, 14, 225, 1, 14.6, 0, 11.5),
        seed("2026-10-16", "18:32:40", 518, 2.5, 4, 206, 3, 39.2, 0, 15.9),
        seed("2026-10-17", "11:31:30", 2462, 13.6, 36, 738, 1, 12.3, 0, 10.1),
        seed("2026-10-19", "08:10:40", 1620, 7.7, 18, 324, 2, 21.3, 0, 10.4),
        seed("2026-10-19", "18:36:19", 799, 4.6, 13, 126, 1, 12.5, 0, 15.1),
        seed("2026-10-20", "08:24:43", 2107, 12.1, 31, 472, 3, 33.9, 0, 10.3),
        seed("2026-10-20", "18:29:21", 2538, 14.4, 37, 813, 3, 30.0, 0, 13.5),
        seed("2026-10-21", "08:23:52", 2332, 14.0, 34, 1004, 1, 8.6, 0, 8.9),
        seed("2026-10-21", "18:28:53", 565, 3.0, 5, 221, 2, 28.9, 0, 15.0),
        seed("2026-10-22", "08:18:05", 582, 3.6, 6, 152, 0, 0.0, 0, 8.1),
        seed("2026-10-22", "18:49:21", 423, 2.3, 6, 108, 1, 9.6, 0, 13.3),
        seed("2026-10-23", "08:05:19", 490, 2.4, 4, 197, 1, 10.5, 0, 10.4),
        seed("2026-10-23", "18:27:00", 1869, 10.0, 24, 289, 0, 0.0, 0, 15.3),
        seed("2026-10-24", "12:30:19", 1829, 12.1, 32, 756, 3, 27.3, 0, 14.6),
        seed("2026-10-26", "08:33:38", 1097, 6.3, 18, 472, 1, 10.7, 0, 7.5),
        seed("2026-10-26", "18:11:15", 402, 2.6, 4, 117, 2, 24.6, 0, 14.0),
        seed("2026-10-27", "08:19:59", 382, 2.3, 7, 128, 3, 37.6, 0, 7.5),
        seed("2026-10-28", "08:10:06", 952, 6.0, 18, 358, 2, 22.8, 0, 7.6),
        seed("2026-10-28", "18:31:46", 2503, 13.2, 32, 1071, 1, 10.3, 0, 11.9),
        seed("2026-10-29", "08:10:43", 1901, 12.1, 33, 853, 1, 9.5, 0, 9.0),
        seed("2026-10-30", "08:27:20", 432, 2.5, 7, 158, 1, 12.7, 0, 6.9),
        seed("2026-10-30", "18:49:51", 352, 2.3, 4, 97, 3, 32.9, 0, 11.8),
        seed("2026-10-31", "11:08:07", 774, 4.1, 8, 202, 0, 0.0, 0, 7.5),
        seed("2026-11-02", "08:09:05", 495, 3.0, 9, 150, 2, 28.8, 0, 7.5),
        seed("2026-11-02", "18:12:15", 1795, 11.9, 34, 662, 3, 36.0, 0, 12.8),
        seed("2026-11-03", "08:19:45", 872, 5.6, 13, 361, 0, 0.0, 0, 7.8),
        seed("2026-11-03", "18:29:14", 1215, 8.0, 19, 537, 0, 0.0, 0, 10.7),
        seed("2026-11-04", "08:33:42", 529, 3.2, 7, 123, 1, 11.0, 240, 1.2),
        seed("2026-11-04", "18:24:44", 1375, 6.9, 15, 533, 3, 37.9, 0, 12.8),
        seed("2026-11-05", "08:07:28", 2306, 13.3, 32, 612, 1, 9.8, 0, 7.2),
        seed("2026-11-05", "18:34:35", 614, 3.6, 9, 108, 2, 22.6, 0, 12.1),
        seed("2026-11-06", "08:29:59", 2518, 12.9, 35, 1090, 3, 32.2, 0, 5.1),
        seed("2026-11-06", "18:42:26", 749, 4.3, 10, 148, 3, 33.0, 0, 12.2),
        seed("2026-11-07", "11:17:04", 605, 3.8, 9, 264, 0, 0.0, 0, 5.0),
        seed("2026-11-09", "08:23:57", 2340, 12.2, 32, 471, 2, 20.3, 0, 4.4),
        seed("2026-11-09", "18:11:15", 879, 5.1, 14, 378, 1, 12.7, 0, 9.5),
        seed("2026-11-10", "08:09:13", 376, 2.5, 4, 98, 0, 0.0, 420, 0.6),
        seed("2026-11-10", "18:16:44", 1999, 10.6, 27, 868, 3, 32.2, 0, 9.6),
        seed("2026-11-11", "08:09:54", 1699, 9.6, 25, 403, 1, 11.8, 0, 5.0),
        seed("2026-11-11", "18:22:08", 2073, 11.2, 29, 609, 1, 13.6, 0, 10.7),
        seed("2026-11-12", "08:06:49", 1317, 7.0, 20, 569, 0, 0.0, 0, 4.9),
        // the card's trip: Thursday evening
        seed("2026-11-12", "18:20:00", 1410, 6.4, 21, 620, 1, 10.5, 0, 9.8),
    )

    /** All 42 trips in time order; the last one is the card's trip. */
    fun trips(): List<TripSummary> {
        val times = SEEDS.map { s ->
            val start = DndTime.epochSec(s.date, s.start)
            TripTimes(start, start + s.durationSec)
        }
        val cd = continuousDriving(times)
        val endOdo = DoubleArray(SEEDS.size)
        endOdo[SEEDS.lastIndex] = END_ODOMETER_KM
        for (k in SEEDS.lastIndex - 1 downTo 0) endOdo[k] = endOdo[k + 1] - SEEDS[k + 1].distanceKm
        return SEEDS.mapIndexed { k, s ->
            TripSummary(
                startEpochSec = times[k].startEpochSec,
                endEpochSec = times[k].endEpochSec,
                distanceKm = s.distanceKm,
                continuousSec = cd[k].atEndSec,
                dawnSec = dawnSeconds(times[k].startEpochSec, times[k].endEpochSec),
                maxOutsideTempC = s.maxTempC,
                subZeroSec = s.subZeroSec,
                harshCount = s.harshCount,
                harshMagnitudeSum = s.harshSum,
                stopStartCount = s.stopStarts,
                congestionSec = s.congestionSec,
                curveLoad = 0.0,
                endOdometerKm = endOdo[k],
                fatigueIndex = null,
            )
        }
    }

    // ---- 출발 전 브리핑 after the weekend oil change (기획서 4, 상황 2) ----

    /** The driver finished driving on Saturday after the service, then entered the record. */
    val SERVICE_RECORD = MaintenanceRecord(MaintenanceItem.ENGINE_OIL, LocalDate.of(2026, 11, 14), 34_762.0)
    val AFTER_SERVICE_LAST_DRIVE_END: LocalDateTime = LocalDateTime.of(2026, 11, 14, 11, 40)
    val AFTER_SERVICE_NOW: LocalDateTime = LocalDateTime.of(2026, 11, 15, 8, 30)
    val AFTER_SERVICE_LAST = VehicleState(48.0, false, FluidLevel.NORMAL, FluidLevel.NORMAL, listOf(LAST_OIL_CHANGE))
    val AFTER_SERVICE_NOW_STATE = VehicleState(48.0, false, FluidLevel.NORMAL, FluidLevel.NORMAL, listOf(LAST_OIL_CHANGE, SERVICE_RECORD))
    const val AFTER_SERVICE_ODOMETER_KM = 34_762.0

    // ---- handover example (기획서 3, 핵심 기능 2): another family member drove in between ----

    val HANDOVER_LAST_DRIVE_END: LocalDateTime = LocalDateTime.of(2026, 11, 16, 19, 5)
    val HANDOVER_NOW: LocalDateTime = LocalDateTime.of(2026, 11, 18, 7, 45)
    val HANDOVER_LAST = VehicleState(45.0, false, FluidLevel.NORMAL, FluidLevel.NORMAL, listOf(LAST_OIL_CHANGE, SERVICE_RECORD))
    val HANDOVER_NOW_STATE = VehicleState(15.0, true, FluidLevel.NORMAL, FluidLevel.NORMAL, listOf(LAST_OIL_CHANGE, SERVICE_RECORD))
    const val HANDOVER_ODOMETER_KM = 34_951.0
}
