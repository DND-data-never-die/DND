package dnd.core

/**
 * spec 5: the five items of the fatigue index, in the spec's table order, with their weights
 * (Friedrichs & Yang 2010, Table 3, Spearman coefficients 0.51 : 0.22 : 0.32 = 48.6 : 21.0 : 30.5,
 * rounded to steps of 5; the 30 is split equally, OECD/JRC 2008).
 */
enum class FatigueItem(val label: String, val weight: Double) {
    DAWN("새벽 운행", 50.0),
    CONTINUOUS("연속 주행", 20.0),
    HARSH("급가감속", 10.0),
    CURVE("커브 주행", 10.0),
    CONGESTION("정체", 10.0);

    /** spec 5 table "값" column; times stay in seconds (spec 1). */
    fun valueIn(trip: TripSummary): Double = when (this) {
        DAWN -> trip.dawnSec.toDouble()
        CONTINUOUS -> trip.continuousSec.toDouble()
        HARSH -> trip.harshMagnitudeSum
        CURVE -> trip.curveLoad
        CONGESTION -> trip.congestionSec.toDouble()
    }
}

/** One row of the breakdown: score = weight x percentile / 100 (spec 5). */
data class ItemScore(
    val item: FatigueItem,
    val value: Double,
    val percentile: Double,
    val weight: Double,
    val score: Double,
)

data class FatigueResult(val items: List<ItemScore>, val index: Double) {
    /** spec 5: shown as an integer, 0.5 goes up. */
    val indexRounded: Long get() = Fmt.roundHalfUp(index)

    fun item(item: FatigueItem): ItemScore = items.first { it.item == item }
}

/** spec 5: index = sum of weight x percentile / 100 over the five items. */
fun fatigueIndex(current: TripSummary, baseline: List<TripSummary>): FatigueResult {
    require(baseline.isNotEmpty()) { "the index needs a baseline" }
    val items = FatigueItem.entries.map { item ->
        val value = item.valueIn(current)
        val pct = percentileRank(value, baseline.map { item.valueIn(it) })
        ItemScore(item, value, pct, item.weight, item.weight * pct / 100.0)
    }
    return FatigueResult(items, items.sumOf { it.score })
}

/**
 * spec 10 / spec 5: the stored index of each baseline trip, computed with the other trips of the
 * same set as its baseline (leave-one-out).
 */
fun leaveOneOutIndices(trips: List<TripSummary>): List<Double> {
    require(trips.size >= 2) { "leave-one-out needs at least two trips" }
    return trips.indices.map { j -> fatigueIndex(trips[j], trips.filterIndexed { k, _ -> k != j }).index }
}

enum class Grade { LOW, NORMAL, HIGH }

/** The ceil(n/3)-th and ceil(2n/3)-th smallest baseline indices (1-based). */
data class GradeBounds(val low: Double, val high: Double)

/**
 * spec 5 [파일럿]: nearest-rank sample quantiles (Hyndman & Fan 1996, Type 1) of the baseline
 * trips' stored indices.
 */
fun gradeBounds(baselineIndices: List<Double>): GradeBounds {
    require(baselineIndices.isNotEmpty()) { "grade bounds need stored indices" }
    val s = baselineIndices.sorted()
    val n = s.size
    val lowRank = (n + 2) / 3 // ceil(n / 3)
    val highRank = (2 * n + 2) / 3 // ceil(2n / 3)
    return GradeBounds(s[lowRank - 1], s[highRank - 1])
}

/** spec 5: at or below the low bound 낮음, at or below the high bound 보통, above it 높음; unrounded. */
fun grade(index: Double, bounds: GradeBounds): Grade = when {
    index <= bounds.low -> Grade.LOW
    index <= bounds.high -> Grade.NORMAL
    else -> Grade.HIGH
}

/**
 * spec 5: the item with the largest score; on equal scores the larger weight.
 * SPEC GAP: equal scores and equal weights (harsh, curve, congestion) go to the earlier item in
 * the spec's table order.
 */
fun largestItem(result: FatigueResult): FatigueItem {
    var best = result.items.first()
    for (r in result.items.drop(1)) {
        if (r.score > best.score || (r.score == best.score && r.weight > best.weight)) best = r
    }
    return best.item
}
