package dnd.core

import kotlin.test.Test
import kotlin.test.assertEquals

/** spec 1 (rounding for display) and spec 6 */
class PhrasesAndFormatTest {
    private val t0 = T.at(2026, 11, 5, 9, 0)

    @Test
    fun durationsRoundToMinutesOnlyForDisplay() {
        assertEquals("2시간 40분", Fmt.durationText(9600L))
        assertEquals("48분", Fmt.durationText(2880L))
        assertEquals("47분", Fmt.durationText(2849L))
        assertEquals("48분", Fmt.durationText(2850L)) // 47.5 min goes up
        assertEquals("2시간", Fmt.durationText(7200L))
        assertEquals("0분", Fmt.durationText(29L))
        assertEquals("1분", Fmt.durationText(30L))
        assertEquals("1시간 10분", Fmt.durationText(4199.9))
        assertEquals("6시간 30분", Fmt.durationText(23_400L))
    }

    @Test
    fun numbersUseKoreanGroupingAndOneDecimal() {
        assertEquals("1,850", Fmt.integer(1850L))
        assertEquals("1,850", Fmt.integer(1849.5))
        assertEquals("2", Fmt.decimal1(2.0))
        assertEquals("2", Fmt.decimal1(1.95))
        assertEquals("1.9", Fmt.decimal1(1.94))
        assertEquals("1,234.6", Fmt.decimal1(1234.56))
        assertEquals(47L, Fmt.roundHalfUp(46.5))
        assertEquals(46L, Fmt.roundHalfUp(46.4999))
    }

    @Test
    fun headlineByGrade() {
        assertEquals("피로도 지수 47, 평소보다 높음", Phrases.headline(47, Grade.HIGH))
        assertEquals("피로도 지수 30, 평소 수준", Phrases.headline(30, Grade.NORMAL))
        assertEquals("피로도 지수 12, 평소보다 낮음", Phrases.headline(12, Grade.LOW))
    }

    @Test
    fun itemValueTexts() {
        val trip = T.trip(t0, t0 + 9600, dawnSec = 0, continuousSec = 9600, harshCount = 9, harshSum = 115.0, curve = 1234.4, congestionSec = 2880)
        assertEquals("연속 주행 2시간 40분", Phrases.itemValueText(FatigueItem.CONTINUOUS, trip))
        assertEquals("정체 48분", Phrases.itemValueText(FatigueItem.CONGESTION, trip))
        assertEquals("급가감속 9회", Phrases.itemValueText(FatigueItem.HARSH, trip))
        assertEquals("새벽 운행 0분", Phrases.itemValueText(FatigueItem.DAWN, trip))
        assertEquals("커브 주행 1,234m/s", Phrases.itemValueText(FatigueItem.CURVE, trip))
        assertEquals("평소의 2배", Phrases.ratioText(2.0))
        assertEquals("평소의 1.5배", Phrases.ratioText(1.46))
    }

    @Test
    fun restAdviceRules() {
        fun trip(cont: Long, dawn: Long = 0, maxTemp: Double = 15.0) =
            T.trip(t0, t0 + cont, dawnSec = dawn, continuousSec = cont, maxTempC = maxTemp)
        assertEquals(emptyList(), Phrases.restAdvice(trip(7199)))
        assertEquals(listOf("돌아가는 길에는 휴게소에서 15분 쉬어 가세요"), Phrases.restAdvice(trip(7200)))
        assertEquals(listOf("돌아가는 길에는 휴게소에서 20분 쉬어 가세요"), Phrases.restAdvice(trip(7200, maxTemp = 33.0)))
        assertEquals(listOf("다음 출발 전에 충분히 쉬어 가세요"), Phrases.restAdvice(trip(3600, dawn = 1)))
        assertEquals(
            listOf("다음 출발 전에 충분히 쉬어 가세요", "돌아가는 길에는 휴게소에서 15분 쉬어 가세요"),
            Phrases.restAdvice(trip(9000, dawn = 600)),
        )
        assertEquals("2시간 연속 운전하셨으니 가까운 휴게소에서 쉬어 가세요", Phrases.VOICE_PROMPT)
    }
}
