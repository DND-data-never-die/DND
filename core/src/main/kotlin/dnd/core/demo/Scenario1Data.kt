package dnd.core.demo

import dnd.core.DndTime
import dnd.core.GRANDEUR_WHEELBASE_M
import dnd.core.TripSegment
import dnd.core.TripSummary
import dnd.core.TripTimes
import dnd.core.continuousDriving
import dnd.core.dawnSeconds
import dnd.core.leaveOneOutIndices
import dnd.core.segmentTrips
import dnd.core.summarizeTrip
import java.time.LocalDate
import java.time.LocalTime

/**
 * Scenario 1 baseline: 36 commute trips (a morning and an evening trip on the 18 weekdays from
 * 2026-10-19 to 2026-11-11) given as trip summaries, plus the first leg of 2026-11-12 computed
 * from the signal log (spec 10).
 *
 * How the table was made (offline, once; frozen here): a rank search
 * (Python random module with seeds 0-199, swap hill-climbing) looked for duration, harsh,
 * curve and congestion ranks of the 37 baseline trips whose leave-one-out indices (spec 10) have
 * nearest-rank bounds of 21 and 34, with Spearman correlation of at least 0.55 between trip
 * duration and the other values and congestion under 90 % of the trip time. Seed 82 hit 21.0 and
 * 34.0 exactly under the weights of that day (46 / 28 / 26). The weights became 50 / 20 / 30 on
 * 2026-11-15; the table was kept, and with them the bounds are 19.4 and 30.0 and the card index is
 * 41.9. Values were then laid on monotone ladders around the leg values measured from
 * [Scenario1Log] (leg 1: harsh sum 36, curve 537.8, congestion 499 s; leg 2: harsh sum 115,
 * curve 950.7, congestion 2,880 s), with the congestion median at 1,440 s, and the 36 rows were
 * spread over the commute slots with the Python random module, seed 20261112. The curve column is
 * on the scale of that log; [CURVE_SCALE] carries it to the present road model.
 * Distance = (congestion x 12 + other time x 45 km/h) / 3600, stop-starts = 3 + congestion / 150,
 * event count = harsh sum / 12.5 (each event 8 to 25 km/h/s).
 */
internal object Scenario1Data {
    /**
     * The scale of the curve loads (2026-11-15): the log's steering follows the road's curvature along
     * distance and its city turns are driven at 15 km/h (Scenario1Log, Track), which multiplied leg
     * 2's curve load by 4.812 (950.7 when the table was laid, now 4,574.7). Every seeded curve load is
     * multiplied by the same factor, so every rank, percentile, 부담 and index stays as it was;
     * scenario 3's placeholder uses the same scale.
     */
    const val CURVE_SCALE = 4.812

    private data class Seed(
        val date: LocalDate,
        val start: LocalTime,
        val durationSec: Long,
        val distanceKm: Double,
        val harshCount: Int,
        val harshSum: Double,
        val stopStarts: Int,
        val congestionSec: Long,
        val curve: Double,
        val maxTempC: Double,
    )

    private fun seed(
        date: String, start: String, durationSec: Long, distanceKm: Double, harshCount: Int, harshSum: Double,
        stopStarts: Int, congestionSec: Long, curve: Double, maxTempC: Double,
    ) = Seed(LocalDate.parse(date), LocalTime.parse(start), durationSec, distanceKm, harshCount, harshSum, stopStarts, congestionSec, curve, maxTempC)

    // date, start, duration s, distance km, harsh count, harsh sum km/h/s, stop-starts, congestion s, curve m/s, max temp C
    private val SEEDS = listOf(
        seed("2026-10-19", "07:55:11", 2904, 15.6, 8, 97.5, 18, 2254, 818.7, 11.1),
        seed("2026-10-19", "17:57:26", 3400, 31.7, 6, 70.0, 11, 1183, 1000.4, 17.3),
        seed("2026-10-20", "07:53:59", 2202, 12.8, 1, 17.5, 14, 1603, 1227.0, 11.3),
        seed("2026-10-20", "18:48:48", 2141, 23.8, 1, 8.5, 5, 319, 968.0, 16.1),
        seed("2026-10-21", "07:48:51", 1645, 12.9, 2, 22.0, 9, 841, 428.5, 12.8),
        seed("2026-10-21", "18:00:39", 2637, 16.8, 6, 81.0, 15, 1766, 650.1, 16.7),
        seed("2026-10-22", "08:03:15", 3545, 17.2, 13, 161.4, 23, 2958, 1421.2, 11.4),
        seed("2026-10-22", "18:21:23", 2782, 26.3, 10, 129.6, 9, 927, 565.9, 16.4),
        seed("2026-10-23", "07:50:01", 3110, 28.8, 13, 166.7, 10, 1098, 1032.8, 12.6),
        seed("2026-10-23", "18:07:37", 2347, 11.7, 5, 61.5, 16, 1928, 790.6, 17.4),
        seed("2026-10-26", "07:35:15", 3461, 18.1, 10, 119.0, 21, 2742, 1453.6, 10.2),
        seed("2026-10-26", "19:03:20", 2492, 12.0, 2, 26.5, 17, 2091, 734.4, 15.6),
        seed("2026-10-27", "08:07:14", 1584, 17.9, 3, 38.8, 4, 207, 622.0, 11.2),
        seed("2026-10-27", "18:54:50", 1790, 10.0, 1, 13.0, 12, 1354, 466.8, 17.6),
        seed("2026-10-28", "07:37:52", 2080, 25.1, 3, 41.7, 4, 96, 1065.1, 11.0),
        seed("2026-10-28", "18:09:31", 1500, 17.4, 4, 44.5, 4, 152, 902.9, 17.4),
        seed("2026-10-29", "07:54:08", 2988, 24.1, 6, 75.5, 13, 1440, 846.7, 11.5),
        seed("2026-10-29", "18:04:53", 3255, 22.3, 12, 156.1, 16, 2010, 1388.9, 16.2),
        seed("2026-10-30", "07:39:26", 3049, 21.2, 11, 140.2, 15, 1847, 931.0, 10.7),
        seed("2026-10-30", "18:07:39", 3339, 17.3, 4, 50.2, 21, 2661, 1194.6, 15.8),
        seed("2026-11-02", "07:43:51", 1935, 14.9, 2, 31.0, 10, 1012, 678.2, 9.6),
        seed("2026-11-02", "18:55:05", 2698, 13.8, 11, 134.9, 17, 2172, 762.5, 16.2),
        seed("2026-11-03", "07:46:47", 3690, 23.2, 9, 108.5, 20, 2498, 1162.2, 11.6),
        seed("2026-11-03", "19:02:15", 1851, 17.0, 4, 47.3, 7, 670, 390.2, 14.2),
        seed("2026-11-04", "07:41:51", 2286, 21.6, 7, 86.5, 8, 756, 352.0, 8.8),
        seed("2026-11-04", "19:04:14", 3812, 19.0, 14, 172.0, 24, 3127, 1486.0, 15.7),
        seed("2026-11-05", "07:44:54", 1996, 13.3, 4, 55.8, 11, 1269, 505.0, 11.1),
        seed("2026-11-05", "17:53:17", 3896, 27.3, 5, 58.7, 19, 2335, 1324.1, 14.1),
        seed("2026-11-06", "07:58:06", 1729, 19.2, 4, 53.0, 5, 263, 706.3, 10.7),
        seed("2026-11-06", "18:32:15", 2843, 13.4, 8, 103.0, 19, 2416, 594.0, 14.7),
        seed("2026-11-09", "07:59:05", 3606, 21.4, 12, 145.5, 20, 2579, 1291.8, 8.2),
        seed("2026-11-09", "18:47:22", 2553, 26.6, 5, 67.2, 7, 585, 874.8, 12.9),
        seed("2026-11-10", "08:08:50", 3194, 36.0, 5, 64.3, 6, 430, 1356.5, 8.4),
        seed("2026-11-10", "18:21:48", 3957, 34.0, 12, 150.8, 14, 1684, 1097.5, 15.3),
        seed("2026-11-11", "08:10:57", 3751, 32.9, 7, 92.0, 13, 1521, 1259.4, 9.6),
        seed("2026-11-11", "17:55:54", 2431, 27.0, 10, 124.3, 5, 374, 1129.9, 15.4),
    )

    class Built(
        val log: List<dnd.core.Sample>,
        val segments: List<TripSegment>,
        /** The 37 baseline trips (36 seeded + leg 1) with their stored leave-one-out indices. */
        val baseline: List<TripSummary>,
        val leg1: TripSummary,
        /** Leg 2 without a stored index; the card computes it. */
        val leg2: TripSummary,
    )

    fun build(): Built {
        val log = Scenario1Log.signalLog()
        val segments = segmentTrips(log)
        check(segments.size == 2 && segments.all { it.isClosed })
        val seedTimes = SEEDS.map { s ->
            val start = DndTime.epochSec(s.date, s.start)
            TripTimes(start, start + s.durationSec)
        }
        val allTimes = seedTimes + segments.map { it.times() }
        val cd = continuousDriving(allTimes)

        // Odometer readings of the seeded trips, counted back from the first leg's start.
        val endOdo = DoubleArray(SEEDS.size)
        var nextStartOdo = segments[0].samples.first().odometerKm
        for (k in SEEDS.indices.reversed()) {
            endOdo[k] = nextStartOdo
            nextStartOdo = endOdo[k] - SEEDS[k].distanceKm
        }
        val seeded = SEEDS.mapIndexed { k, s ->
            val t = seedTimes[k]
            TripSummary(
                startEpochSec = t.startEpochSec,
                endEpochSec = t.endEpochSec,
                distanceKm = s.distanceKm,
                continuousSec = cd[k].atEndSec,
                dawnSec = dawnSeconds(t.startEpochSec, t.endEpochSec),
                maxOutsideTempC = s.maxTempC,
                subZeroSec = 0,
                harshCount = s.harshCount,
                harshMagnitudeSum = s.harshSum,
                stopStartCount = s.stopStarts,
                congestionSec = s.congestionSec,
                curveLoad = s.curve * CURVE_SCALE,
                endOdometerKm = endOdo[k],
                fatigueIndex = null,
            )
        }
        val leg1 = summarizeTrip(segments[0], cd[SEEDS.size], GRANDEUR_WHEELBASE_M, null)
        val leg2 = summarizeTrip(segments[1], cd[SEEDS.size + 1], GRANDEUR_WHEELBASE_M, null)
        val unindexed = seeded + leg1
        val stored = leaveOneOutIndices(unindexed) // spec 10
        val baseline = unindexed.mapIndexed { j, t -> t.copy(fatigueIndex = stored[j]) }
        return Built(log, segments, baseline, baseline.last(), leg2)
    }
}
