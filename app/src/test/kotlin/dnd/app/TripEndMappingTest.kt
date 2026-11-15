package dnd.app

import dnd.app.ui.model.toTripEndUiState
import dnd.core.demo.DemoScenarios
import org.junit.Assert.assertEquals
import org.junit.Test

/** The engine's scenario 1, mapped for Screen A, shows exactly the application's values. */
class TripEndMappingTest {

    @Test
    fun scenario1MatchesTheApplicationValues() {
        val expected = Scenario1Fixture.tripEnd(selfCheck = 6)
        val actual = DemoScenarios.scenario1().toTripEndUiState(selfCheck = 6)

        // Positions of marks (knob, segment edges, bar fill) use unrounded engine values (index
        // 41.89, bounds 19.44 and 30.0); the text shows them rounded. Check them to 0.01, then compare
        // everything else exactly.
        val actualDriver = checkNotNull(actual.driver)
        val expectedDriver = checkNotNull(expected.driver)
        val grade = actualDriver.grade
        assertEquals(41.89f, grade.value, 0.01f)
        assertEquals(19.44f, grade.lowUpper, 0.01f)
        assertEquals(30f, grade.midUpper, 0.01f)
        assertEquals(13_150f, actual.vehicle.distanceProgress.current, 0.01f)

        val aligned = actual.copy(
            driver = actualDriver.copy(
                grade = grade.copy(
                    value = expectedDriver.grade.value,
                    lowUpper = expectedDriver.grade.lowUpper,
                    midUpper = expectedDriver.grade.midUpper,
                ),
            ),
            vehicle = actual.vehicle.copy(
                distanceProgress = actual.vehicle.distanceProgress.copy(current = expected.vehicle.distanceProgress.current),
            ),
        )
        assertEquals(expected, aligned)
    }
}
