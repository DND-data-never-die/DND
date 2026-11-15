package dnd.core

/** spec 6: card phrases and the rules that pick them. */
object Phrases {
    /** spec 4 */
    const val COLLECTING = "평소 기록 수집 중"

    /** spec 6: in-drive voice prompt. */
    const val VOICE_PROMPT = "2시간 연속 운전하셨으니 가까운 휴게소에서 쉬어 가세요"

    /** spec 6: rest advice after dawn driving. */
    const val DAWN_REST = "다음 출발 전에 충분히 쉬어 가세요"

    /** spec 6: rest advice after 2 h or more of continuous driving (spec 2 source: 15 min after 2 h). */
    const val REST_ADVICE_MIN_CONTINUOUS_SEC = 7_200L
    const val REST_MINUTES = 15
    const val HEAT_REST_MINUTES = 20

    fun continuousRest(heat: Boolean): String =
        "돌아가는 길에는 휴게소에서 ${if (heat) HEAT_REST_MINUTES else REST_MINUTES}분 쉬어 가세요"

    /** spec 6: 높음 "평소보다 높음", 보통 "평소 수준", 낮음 "평소보다 낮음". */
    fun gradeText(grade: Grade): String = when (grade) {
        Grade.HIGH -> "평소보다 높음"
        Grade.NORMAL -> "평소 수준"
        Grade.LOW -> "평소보다 낮음"
    }

    /** spec 6: "피로도 지수 42, 평소보다 높음". */
    fun headline(indexRounded: Long, grade: Grade): String = "피로도 지수 $indexRounded, ${gradeText(grade)}"

    /**
     * spec 6: an item and its value, e.g. "연속 주행 2시간 40분", "정체 48분", "급가감속 9회".
     * 급가감속 shows the event count, as the card does (spec 3-1).
     * SPEC GAP: the spec gives no display format for 커브 주행; it is written as the rounded sum
     * with its unit, e.g. "커브 주행 1,234m/s".
     */
    fun itemValueText(item: FatigueItem, trip: TripSummary): String = when (item) {
        FatigueItem.DAWN -> "${item.label} ${Fmt.durationText(trip.dawnSec)}"
        FatigueItem.CONTINUOUS -> "${item.label} ${Fmt.durationText(trip.continuousSec)}"
        FatigueItem.HARSH -> "${item.label} ${trip.harshCount}회"
        FatigueItem.CURVE -> "${item.label} ${Fmt.integer(trip.curveLoad)}m/s"
        FatigueItem.CONGESTION -> "${item.label} ${Fmt.durationText(trip.congestionSec)}"
    }

    /** spec 6: "평소의 2배", one decimal place, an integer when whole. */
    fun ratioText(ratio: Double): String = "평소의 ${Fmt.decimal1(ratio)}배"

    /**
     * spec 6 rest advice: "돌아가는 길에는 ..." when continuous driving is 2 h or more (20 min on a
     * heat trip); "다음 출발 전에 ..." when there was dawn driving. When both apply, the dawn phrase
     * comes first.
     */
    fun restAdvice(trip: TripSummary): List<String> {
        val out = mutableListOf<String>()
        if (trip.dawnSec > 0L) out += DAWN_REST
        if (trip.continuousSec >= REST_ADVICE_MIN_CONTINUOUS_SEC) out += continuousRest(trip.isHeat)
        return out
    }
}

/** spec 6 "평소와 비교" line. */
data class Comparison(
    val item: FatigueItem,
    val value: Double,
    val median: Double,
    val ratio: Double,
    val valueText: String,
    val ratioText: String,
) {
    /** "정체 48분, 평소의 2배" */
    val text: String get() = "$valueText, $ratioText"
}

/**
 * spec 6: among the items other than the largest one, the item with the largest value / median
 * (spec 4; items whose median is 0 have no ratio). Shown only when that ratio, unrounded, is
 * above 1. The ratio uses the item value of spec 5 (for 급가감속 the magnitude sum).
 * SPEC GAP: equal ratios go to the earlier item in the spec 5 table order.
 * SPEC GAP: when 급가감속 is picked, the text shows the event count ("급가감속 9회") next to the
 * magnitude-sum ratio.
 */
fun comparisonLine(current: TripSummary, baseline: List<TripSummary>, largest: FatigueItem): Comparison? {
    var best: Comparison? = null
    for (item in FatigueItem.entries) {
        if (item == largest) continue
        val med = median(baseline.map { item.valueIn(it) })
        val value = item.valueIn(current)
        val ratio = ratioToMedian(value, med) ?: continue
        if (best == null || ratio > best.ratio) {
            best = Comparison(item, value, med, ratio, Phrases.itemValueText(item, current), Phrases.ratioText(ratio))
        }
    }
    return best?.takeIf { it.ratio > 1.0 }
}

/** Everything the driver card computes for one finished trip. */
sealed interface DriverAssessment {
    val trip: TripSummary
    val baseline: List<TripSummary>
    val restAdvice: List<String>

    /** spec 4: fewer than 7 days with trips. */
    data class Collecting(
        override val trip: TripSummary,
        override val baseline: List<TripSummary>,
        override val restAdvice: List<String>,
        val baselineDays: Int,
    ) : DriverAssessment {
        val text: String get() = Phrases.COLLECTING
    }

    data class Ready(
        override val trip: TripSummary,
        override val baseline: List<TripSummary>,
        override val restAdvice: List<String>,
        val result: FatigueResult,
        val bounds: GradeBounds,
        val grade: Grade,
        val largest: FatigueItem,
        val largestText: String,
        val comparison: Comparison?,
    ) : DriverAssessment {
        val headline: String get() = Phrases.headline(result.indexRounded, grade)
    }
}

/**
 * spec 5: the indices the grade bounds are taken from: each baseline trip's stored index.
 * SPEC GAP: a baseline trip without a stored index (it ended while the baseline was still
 * collecting) gets its leave-one-out index inside the current baseline, the method spec 10 uses.
 */
fun baselineIndices(baseline: List<TripSummary>): List<Double> {
    if (baseline.all { it.fatigueIndex != null }) return baseline.map { it.fatigueIndex!! }
    val loo = leaveOneOutIndices(baseline)
    return baseline.mapIndexed { j, t -> t.fatigueIndex ?: loo[j] }
}

/**
 * spec 4-6: the driver card of [current]. [history] holds earlier trip summaries of the same
 * profile with their stored indices (spec 5).
 */
fun assessDriver(current: TripSummary, history: List<TripSummary>): DriverAssessment {
    val baseline = baselineFor(history, current.startEpochSec)
    val rest = Phrases.restAdvice(current)
    if (!baselineReady(baseline)) return DriverAssessment.Collecting(current, baseline, rest, baselineDays(baseline))
    val result = fatigueIndex(current, baseline)
    val bounds = gradeBounds(baselineIndices(baseline))
    val largest = largestItem(result)
    return DriverAssessment.Ready(
        trip = current,
        baseline = baseline,
        restAdvice = rest,
        result = result,
        bounds = bounds,
        grade = grade(result.index, bounds),
        largest = largest,
        largestText = Phrases.itemValueText(largest, current),
        comparison = comparisonLine(current, baseline, largest),
    )
}
