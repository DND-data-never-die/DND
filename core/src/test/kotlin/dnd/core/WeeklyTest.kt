package dnd.core

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** spec 9 */
class WeeklyTest {
    private val mon = LocalDate.of(2026, 11, 9)

    @Test
    fun sampleStandardDeviationDividesByNMinusOne() {
        // mean 5, squared deviations 32, n - 1 = 7 (the population value would be 2.0)
        assertEquals(sqrt(32.0 / 7.0), sampleStdDev(listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0))!!, 1e-12)
        assertNull(sampleStdDev(listOf(3.0)))
    }

    @Test
    fun weekStartsOnMonday() {
        assertEquals(mon, weekStartOf(LocalDate.of(2026, 11, 15)))
        assertEquals(mon, weekStartOf(mon))
        assertEquals(LocalDate.of(2026, 11, 16), weekStartOf(LocalDate.of(2026, 11, 16)))
    }

    @Test
    fun deviationOfFirstDepartures() {
        val trips = listOf(
            T.trip(T.at(mon, 7, 0), T.at(mon, 7, 40)),
            T.trip(T.at(mon, 18, 0), T.at(mon, 18, 40)), // not the first of the day
            T.trip(T.at(mon.plusDays(1), 8, 0), T.at(mon.plusDays(1), 8, 40)),
            T.trip(T.at(mon.plusDays(2), 9, 0), T.at(mon.plusDays(2), 9, 40)),
        )
        // 7:00, 8:00, 9:00 -> sample SD = 3600 s
        assertEquals(3600.0, departureDeviationSec(trips, mon)!!, 1e-9)
    }

    @Test
    fun usualDeviationIsTheMedianOfThePrecedingThreeWeeks() {
        fun week(start: LocalDate, minutesApart: Int) = listOf(
            T.trip(T.at(start, 7, 0), T.at(start, 7, 30)),
            T.trip(T.at(start.plusDays(1), 7, minutesApart), T.at(start.plusDays(1), 8, 0 + minutesApart)),
        )
        // two departures d apart: SD = d / sqrt(2)
        val trips = week(mon.minusWeeks(1), 10) + week(mon.minusWeeks(2), 30) + week(mon.minusWeeks(3), 20) + week(mon.minusWeeks(4), 50)
        assertEquals(20 * 60 / sqrt(2.0), usualDepartureDeviationSec(trips, mon)!!, 1e-9)
    }

    @Test
    fun restIntervalWhenTheReturnIsAfterMidnight() {
        val tue = mon.plusDays(1)
        val trips = listOf(
            T.trip(T.at(mon, 8, 0), T.at(mon, 8, 40)),
            T.trip(T.at(mon, 23, 30), T.at(tue, 0, 45)), // belongs to Monday
            T.trip(T.at(tue, 7, 30), T.at(tue, 8, 10)),
            T.trip(T.at(tue, 18, 0), T.at(tue, 18, 40)),
            T.trip(T.at(mon.plusDays(2), 7, 0), T.at(mon.plusDays(2), 7, 40)),
        )
        val r = restIntervals(trips, mon)
        assertEquals(2, r.size)
        assertEquals(mon, r[0].date)
        assertEquals(6L * 3600 + 45 * 60, r[0].gapSec) // Tue 00:45 -> Tue 07:30
        assertTrue(r[0].isShort)
        assertEquals(12L * 3600 + 20 * 60, r[1].gapSec) // Tue 18:40 -> Wed 07:00
        assertFalse(r[1].isShort)
    }

    @Test
    fun eightHoursExactlyIsNotShort() {
        assertFalse(RestInterval(mon, 0L, 8L * 3600).isShort)
        assertTrue(RestInterval(mon, 0L, 8L * 3600 - 1).isShort)
    }

    @Test
    fun congestionAboveTheMeanOfDrivingDays() {
        val byDay = mapOf(mon to 600L, mon.plusDays(1) to 300L, mon.plusDays(2) to 300L)
        // mean 400: only Monday is above
        assertEquals(listOf(mon), congestionFlaggedDates(byDay))
        assertEquals(emptyList(), congestionFlaggedDates(mapOf(mon to 300L, mon.plusDays(1) to 300L)))
    }

    @Test
    fun summaryOpensFromFriday() {
        assertFalse(opensWeeklySummary(T.trip(T.at(mon.plusDays(3), 18, 0), T.at(mon.plusDays(3), 18, 40))))
        assertTrue(opensWeeklySummary(T.trip(T.at(mon.plusDays(4), 18, 0), T.at(mon.plusDays(4), 18, 40))))
        assertTrue(opensWeeklySummary(T.trip(T.at(mon.plusDays(6), 9, 0), T.at(mon.plusDays(6), 9, 40))))
    }

    @Test
    fun adviceText() {
        assertEquals("월요일은 출발 시각을 조정해 보세요", departureAdvice(listOf(DayOfWeek.MONDAY)))
        assertEquals("월요일과 화요일은 출발 시각을 조정해 보세요", departureAdvice(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)))
        assertEquals("월요일, 화요일과 금요일은 출발 시각을 조정해 보세요", departureAdvice(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.FRIDAY)))
        assertNull(departureAdvice(emptyList()))
    }
}
