package dnd.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** The engine takes every time explicitly; the machine clock, zone and locale never enter it. */
class NoImplicitClockTest {
    private val forbidden = listOf(
        "systemDefault", "LocalDate.now", "LocalDateTime.now", "ZonedDateTime.now", "Instant.now",
        "Clock.system", "currentTimeMillis", "Locale.getDefault", "TimeZone.getDefault", "Random",
    )

    @Test
    fun mainSourcesNeverReadTheMachineClockZoneOrLocale() {
        val root = File("src/main/kotlin")
        assertTrue(root.isDirectory, "run from the core project directory")
        val hits = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.flatMap { f ->
            f.readLines().withIndex().filter { (_, line) -> forbidden.any { line.contains(it) } }
                .map { (i, line) -> "${f.path}:${i + 1}: ${line.trim()}" }
        }.toList()
        assertTrue(hits.isEmpty(), hits.joinToString("\n"))
    }
}
