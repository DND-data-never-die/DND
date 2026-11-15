package dnd.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/**
 * The one zone and locale of the engine. Every entry point takes explicit times (epoch seconds or
 * LocalDate values); nothing reads the machine clock, zone or locale.
 */
object DndTime {
    /** spec 1: dates and times are Asia/Seoul. */
    val ZONE: ZoneId = ZoneId.of("Asia/Seoul")

    /** Number and duration formatting locale. */
    val LOCALE: Locale = Locale.KOREA

    fun date(epochSec: Long): LocalDate = Instant.ofEpochSecond(epochSec).atZone(ZONE).toLocalDate()

    fun dateTime(epochSec: Long): LocalDateTime = Instant.ofEpochSecond(epochSec).atZone(ZONE).toLocalDateTime()

    fun epochSec(dateTime: LocalDateTime): Long = dateTime.atZone(ZONE).toEpochSecond()

    fun epochSec(date: LocalDate, time: LocalTime): Long = epochSec(LocalDateTime.of(date, time))

    /** Seconds since local midnight of the instant's own date. */
    fun secondOfDay(epochSec: Long): Int = dateTime(epochSec).toLocalTime().toSecondOfDay()
}
