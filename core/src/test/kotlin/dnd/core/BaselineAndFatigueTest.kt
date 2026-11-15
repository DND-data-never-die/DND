package dnd.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** spec 4 and 5 */
class BaselineAndFatigueTest {
    private val now = T.at(2026, 11, 12, 14, 30)

    @Test
    fun windowIsThe28DaysBeforeTheStartWithoutTheCurrentTrip() {
        val day = 86_400L
        val inEdge = T.trip(now - 28 * day, now - 28 * day + 600)
        val outEdge = T.trip(now - 28 * day - 1, now - 28 * day + 600)
        val recent = T.trip(now - 3600, now - 1800)
        val current = T.trip(now, now + 600)
        val b = baselineFor(listOf(outEdge, inEdge, recent, current), now)
        assertEquals(listOf(inEdge, recent), b)
    }

    @Test
    fun sevenDistinctDaysAreNeeded() {
        val day = 86_400L
        val six = (1..6).flatMap { d -> listOf(T.trip(now - d * day, now - d * day + 600), T.trip(now - d * day + 3600, now - d * day + 4200)) }
        assertEquals(6, baselineDays(six))
        assertFalse(baselineReady(six))
        val seven = six + T.trip(now - 7 * day, now - 7 * day + 600)
        assertTrue(baselineReady(seven))
    }

    @Test
    fun percentileCountsOnlyStrictlyLowerValues() {
        val base = listOf(0.0, 0.0, 5.0, 5.0, 10.0)
        assertEquals(0.0, percentileRank(0.0, base))
        assertEquals(40.0, percentileRank(5.0, base))
        assertEquals(80.0, percentileRank(7.0, base))
        assertEquals(80.0, percentileRank(10.0, base))
        assertEquals(100.0, percentileRank(11.0, base))
        // a zero value gets 0 even when every baseline value is 0
        assertEquals(0.0, percentileRank(0.0, listOf(0.0, 0.0, 0.0)))
    }

    @Test
    fun medianOddEvenAndZero() {
        assertEquals(3.0, median(listOf(5.0, 1.0, 3.0)))
        assertEquals(2.5, median(listOf(3.0, 1.0, 2.0, 10.0)))
        assertEquals(0.0, median(listOf(0.0, 0.0, 0.0, 5.0)))
        assertNull(ratioToMedian(4.0, 0.0))
        assertEquals(2.0, ratioToMedian(5.0, 2.5))
    }

    @Test
    fun nearestRankGradeBounds() {
        val seven = listOf(70.0, 10.0, 50.0, 30.0, 20.0, 60.0, 40.0)
        assertEquals(GradeBounds(30.0, 50.0), gradeBounds(seven)) // ranks ceil(7/3)=3, ceil(14/3)=5
        val six = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        assertEquals(GradeBounds(2.0, 4.0), gradeBounds(six)) // ranks 2 and 4
        assertEquals(GradeBounds(9.0, 9.0), gradeBounds(listOf(9.0)))
        val b = GradeBounds(30.0, 50.0)
        assertEquals(Grade.LOW, grade(30.0, b))
        assertEquals(Grade.NORMAL, grade(30.000001, b))
        assertEquals(Grade.NORMAL, grade(50.0, b))
        assertEquals(Grade.HIGH, grade(50.000001, b))
    }

    private val day = 86_400L
    private fun base(d: Int, dawn: Long, cont: Long, harsh: Double, curve: Double, cong: Long) =
        T.trip(now - d * day, now - d * day + cont, dawnSec = dawn, continuousSec = cont, harshSum = harsh, curve = curve, congestionSec = cong)

    private val fourTrips = listOf(
        base(1, 0, 1000, 10.0, 100.0, 0),
        base(2, 0, 2000, 20.0, 200.0, 100),
        base(3, 600, 3000, 30.0, 300.0, 200),
        base(4, 0, 4000, 40.0, 400.0, 300),
    )
    private val current = T.trip(now, now + 2500, dawnSec = 300, continuousSec = 2500, harshSum = 40.0, curve = 50.0, congestionSec = 300)

    @Test
    fun indexBreakdownByHand() {
        val r = fatigueIndex(current, fourTrips)
        // percentiles: dawn 3/4, continuous 2/4, harsh 3/4 (40 is not below 40), curve 0/4, congestion 3/4
        assertEquals(listOf(75.0, 50.0, 75.0, 0.0, 75.0), r.items.map { it.percentile })
        assertEquals(listOf(50.0, 20.0, 10.0, 10.0, 10.0), r.items.map { it.weight })
        // 50*0.75 + 20*0.5 + 10*0.75 + 0 + 10*0.75 = 37.5 + 10 + 7.5 + 7.5 = 62.5
        assertEquals(62.5, r.index, 1e-9)
        assertEquals(63L, r.indexRounded) // 0.5 goes up
        assertEquals(FatigueItem.DAWN, largestItem(r))
        assertEquals(100.0, FatigueItem.entries.sumOf { it.weight }, 1e-12)
    }

    @Test
    fun largestItemTieRules() {
        fun row(item: FatigueItem, score: Double) = ItemScore(item, 0.0, 0.0, item.weight, score)
        // equal scores, different weights: the larger weight
        val a = FatigueResult(listOf(row(FatigueItem.DAWN, 14.0), row(FatigueItem.CONTINUOUS, 14.0), row(FatigueItem.HARSH, 1.0), row(FatigueItem.CURVE, 1.0), row(FatigueItem.CONGESTION, 1.0)), 31.0)
        assertEquals(FatigueItem.DAWN, largestItem(a))
        val b = FatigueResult(listOf(row(FatigueItem.DAWN, 0.0), row(FatigueItem.CONTINUOUS, 5.0), row(FatigueItem.HARSH, 5.0), row(FatigueItem.CURVE, 1.0), row(FatigueItem.CONGESTION, 1.0)), 12.0)
        assertEquals(FatigueItem.CONTINUOUS, largestItem(b))
        // equal scores and weights: table order (SPEC GAP)
        val c = FatigueResult(listOf(row(FatigueItem.DAWN, 0.0), row(FatigueItem.CONTINUOUS, 0.0), row(FatigueItem.HARSH, 3.0), row(FatigueItem.CURVE, 2.0), row(FatigueItem.CONGESTION, 3.0)), 8.0)
        assertEquals(FatigueItem.HARSH, largestItem(c))
    }

    @Test
    fun comparisonPicksTheLargestRatioOutsideTheLargestItem() {
        // medians: dawn 0 (no ratio), continuous 2500 (1.0), harsh 25 (1.6), curve 250 (0.2), congestion 150 (2.0)
        val c = comparisonLine(current, fourTrips, FatigueItem.DAWN)!!
        assertEquals(FatigueItem.CONGESTION, c.item)
        assertEquals(2.0, c.ratio)
        assertEquals("정체 5분, 평소의 2배", c.text)
        // without congestion in the race, harsh wins with 1.6
        val d = comparisonLine(current.copy(congestionSec = 0), fourTrips, FatigueItem.DAWN)!!
        assertEquals(FatigueItem.HARSH, d.item)
        assertEquals("평소의 1.6배", d.ratioText)
    }

    @Test
    fun comparisonIsHiddenAtOneOrBelow() {
        val low = current.copy(continuousSec = 2500, harshMagnitudeSum = 25.0, curveLoad = 250.0, congestionSec = 150)
        assertNull(comparisonLine(low, fourTrips, FatigueItem.DAWN))
    }

    @Test
    fun leaveOneOutUsesTheOtherTrips() {
        val idx = leaveOneOutIndices(fourTrips)
        // trip 4 against trips 1-3: dawn 0 (value 0), continuous 3/3, harsh 3/3, curve 3/3, congestion 3/3
        assertEquals(20.0 + 30.0, idx[3], 1e-9)
        // trip 1 against trips 2-4: everything lowest -> 0
        assertEquals(0.0, idx[0], 1e-12)
        // trip 3: dawn 3/3 (600 > 0,0,0), continuous 2/3, harsh 2/3, curve 2/3, congestion 2/3
        assertEquals(50.0 + 20.0 * 2 / 3 + 30.0 * 2 / 3, idx[2], 1e-9)
    }

    @Test
    fun assessDriverWithSevenDaysAndNoStoredIndicesUsesLeaveOneOut() {
        // seven trips on seven days, continuous 1000..7000 s, everything else 0; none stored
        val seven = (1..7).map { d -> base(d, 0, 1000L * d, 0.0, 0.0, 0) }
        val loo = leaveOneOutIndices(seven)
        // trip with continuous 1000*d has d-1 lower among the other 6: 20 * (d - 1) / 6
        loo.forEachIndexed { i, v -> assertEquals(20.0 * i / 6, v, 1e-9) }
        val cur = T.trip(now, now + 5500, continuousSec = 5500)
        val a = assertIs<DriverAssessment.Ready>(assessDriver(cur, seven))
        // nearest rank over 7 values: 3rd and 5th smallest -> 20*2/6 and 20*4/6
        assertEquals(20.0 * 2 / 6, a.bounds.low, 1e-9)
        assertEquals(20.0 * 4 / 6, a.bounds.high, 1e-9)
        // 5500 s is above 5 of 7: 20 * 5/7 = 14.29 > 13.33 -> 높음
        assertEquals(20.0 * 5 / 7, a.result.index, 1e-9)
        assertEquals(Grade.HIGH, a.grade)
        assertEquals("피로도 지수 14, 평소보다 높음", a.headline)
    }

    @Test
    fun assessDriverCollectsBelowSevenDays() {
        val a = assertIs<DriverAssessment.Collecting>(assessDriver(current, fourTrips))
        assertEquals(4, a.baselineDays)
        assertEquals("평소 기록 수집 중", a.text)
    }
}
