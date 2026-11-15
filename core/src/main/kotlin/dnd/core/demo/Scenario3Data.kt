package dnd.core.demo

import dnd.core.DndTime
import dnd.core.TripSummary
import dnd.core.TripTimes
import dnd.core.continuousDriving
import dnd.core.dawnSeconds
import java.time.LocalDate
import java.time.LocalTime

/**
 * Scenario 3 (기획서 4, 상황 3): a commuter driving about 70 minutes a day on weekdays, 28 days of
 * trip summaries from 2026-10-17 to 2026-11-13 (spec 10). Written by hand:
 * - Weeks of 10-19, 10-26 and 11-02: departures 07:25-07:55 (sample SDs about 7, 11 and 8 min).
 * - Week of 11-09: departures Mon 08:25, Tue 07:20, Wed 08:51, Thu 07:30, Fri 05:50
 *   (sample SD 69.9 min, shown as 1시간 10분); Thursday's last trip 22:45-23:20, Friday's first
 *   05:50, so the interval is 6시간 30분; congestion Mon 38, Tue 33, Wed 12, Thu 14, Fri 6 min
 *   (mean 20.6), so Monday and Tuesday are above the mean.
 * Distance = (congestion x 12 + other time x 40 km/h) / 3600; stop-starts = 3 + congestion / 150.
 */
internal object Scenario3Data {
    val WEEK_START: LocalDate = LocalDate.of(2026, 11, 9)

    private data class Seed(val date: LocalDate, val start: LocalTime, val durationSec: Long, val congestionSec: Long)

    private fun seed(date: String, start: String, durationSec: Long, congestionSec: Long) =
        Seed(LocalDate.parse(date), LocalTime.parse(start), durationSec, congestionSec)

    private val SEEDS = listOf(
        // week of 2026-10-19
        seed("2026-10-19", "07:35", 2160, 720), seed("2026-10-19", "18:20", 2040, 540),
        seed("2026-10-20", "07:42", 2100, 660), seed("2026-10-20", "18:35", 2220, 780),
        seed("2026-10-21", "07:30", 1980, 480), seed("2026-10-21", "18:10", 2100, 600),
        seed("2026-10-22", "07:48", 2280, 840), seed("2026-10-22", "18:40", 1920, 420),
        seed("2026-10-23", "07:38", 2040, 540), seed("2026-10-23", "18:15", 2160, 660),
        // week of 2026-10-26
        seed("2026-10-26", "07:40", 2220, 780), seed("2026-10-26", "18:30", 2100, 600),
        seed("2026-10-27", "07:25", 1980, 420), seed("2026-10-27", "18:15", 2040, 540),
        seed("2026-10-28", "07:55", 2340, 900), seed("2026-10-28", "18:45", 1980, 480),
        seed("2026-10-29", "07:33", 2100, 600), seed("2026-10-29", "18:25", 2160, 660),
        seed("2026-10-30", "07:45", 2040, 540), seed("2026-10-30", "18:05", 2100, 600),
        // week of 2026-11-02
        seed("2026-11-02", "07:37", 2160, 660), seed("2026-11-02", "18:22", 2100, 600),
        seed("2026-11-03", "07:50", 2280, 780), seed("2026-11-03", "18:38", 1980, 480),
        seed("2026-11-04", "07:28", 2040, 540), seed("2026-11-04", "18:12", 2160, 720),
        seed("2026-11-05", "07:41", 2100, 600), seed("2026-11-05", "18:30", 2040, 540),
        seed("2026-11-06", "07:35", 1980, 480), seed("2026-11-06", "18:18", 2100, 600),
        // this week, 2026-11-09 to 2026-11-13
        seed("2026-11-09", "08:25", 2520, 1500), seed("2026-11-09", "18:30", 2280, 780),
        seed("2026-11-10", "07:20", 2400, 1380), seed("2026-11-10", "18:20", 2160, 600),
        seed("2026-11-11", "08:51", 1860, 300), seed("2026-11-11", "18:40", 1980, 420),
        seed("2026-11-12", "07:30", 2040, 540), seed("2026-11-12", "22:45", 2100, 300),
        seed("2026-11-13", "05:50", 1920, 0), seed("2026-11-13", "17:30", 1980, 360),
    )

    fun trips(): List<TripSummary> {
        val times = SEEDS.map { s ->
            val start = DndTime.epochSec(s.date, s.start)
            TripTimes(start, start + s.durationSec)
        }
        val cd = continuousDriving(times)
        var odo = 51_200.0
        return SEEDS.mapIndexed { k, s ->
            val distance = Math.round((s.congestionSec * 12.0 + (s.durationSec - s.congestionSec) * 40.0) / 360.0) / 10.0
            odo += distance
            TripSummary(
                startEpochSec = times[k].startEpochSec,
                endEpochSec = times[k].endEpochSec,
                distanceKm = distance,
                continuousSec = cd[k].atEndSec,
                dawnSec = dawnSeconds(times[k].startEpochSec, times[k].endEpochSec),
                maxOutsideTempC = 12.0,
                subZeroSec = 0,
                harshCount = 1,
                harshMagnitudeSum = 10.0,
                stopStartCount = 3 + (s.congestionSec / 150).toInt(),
                congestionSec = s.congestionSec,
                curveLoad = 600.0 * Scenario1Data.CURVE_SCALE,
                endOdometerKm = odo,
                fatigueIndex = null,
            )
        }
    }
}
