package dnd.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import dnd.app.ui.icons.DndIcons
import dnd.app.ui.icons.Pictogram
import dnd.app.ui.icons.PictogramSize
import dnd.app.ui.model.BriefingCase
import dnd.app.ui.model.ReplayTiming
import dnd.app.ui.model.SevereTableForm
import dnd.app.ui.model.replayEnd
import dnd.app.ui.model.replayStart
import dnd.app.ui.model.replayStateAt
import dnd.app.ui.model.toBriefingUiState
import dnd.app.ui.model.toEvidenceUiState
import dnd.app.ui.model.toTripEndUiState
import dnd.app.ui.model.toVehicleDetailUiState
import dnd.app.ui.model.toWeeklyUiState
import dnd.app.ui.screens.BriefingScreen
import dnd.app.ui.screens.DisplayContext
import dnd.app.ui.screens.EvidenceScreen
import dnd.app.ui.screens.LaunchScreen
import dnd.app.ui.screens.ReplayScreen
import dnd.app.ui.screens.TripEndScreen
import dnd.app.ui.screens.VehicleDetailScreen
import dnd.app.ui.screens.WeeklyScreen
import dnd.app.ui.theme.DefaultDriverSide
import dnd.app.ui.theme.DndMotion
import dnd.app.ui.theme.DndTheme
import dnd.app.ui.theme.DriverSide
import dnd.core.demo.DemoScenarios
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders each screen with Robolectric and writes app/build/outputs/roborazzi/<name>.png.
 * Run with ./gradlew :app:recordRoborazziDebug (a plain test run writes nothing).
 *
 * Display: the Pleos Connect landscape app window, 1700 x 1184 dp at 160 DPI (1 dp = 1 px), Korean
 * locale, Android 14 (API 34). Each capture is also copied to video/still_<name>.png, so the video
 * stills and sequences come from one rasteriser, and its texts are counted into text_counts.tsv.
 *
 * Add a screen with one line: @Test fun name() = capture("name") { Screen(...) }
 * Files under layout_check/ are for checking the drawings only; they are not application images.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ko-rKR-w1700dp-h1184dp-land-mdpi")
class ScreenCaptureTest {

    @get:Rule
    val compose = createComposeRule()

    // Launch screen, finished composition (before it fades into the first screen)
    @Test fun launch() = capture("launch", units = "1") { LaunchScreen(DndMotion.LAUNCH_FADE_START_MS / DndMotion.LAUNCH_MS.toFloat()) }

    // Screen A, scenario 1
    @Test fun tripEnd() = capture("trip_end", units = "4") { TripEndScreen(s1.toTripEndUiState(selfCheck = 6)) }

    @Test fun tripEndUnselected() = capture("trip_end_unselected", units = "4") { TripEndScreen(s1.toTripEndUiState()) }

    // Right-hand drive: the touch controls mirror to the right edge, nearest the driver
    @Test fun tripEndRhd() = capture("trip_end_rhd", DriverSide.RIGHT, units = "4") { TripEndScreen(s1.toTripEndUiState(selfCheck = 6)) }

    // The vehicle card's 상세 정보 (엔진 오일 교체 시기): scenario 1, scenario 2 (normal and severe interval)
    // Inner screens (피로도 지수, 엔진 오일 교체 시기, 주간 요약) are captured with their back icon.
    @Test fun vehicleDetail() = capture("vehicle_detail", units = "4; list 4 rows") { VehicleDetailScreen(s1.toVehicleDetailUiState(), onBack = {}) }

    // Scenario 1 with the severe-condition interval the driver switched on (the video's setting shot)
    @Test fun vehicleDetailSevere() = capture("vehicle_detail_severe", units = "4; list 4 rows") { VehicleDetailScreen(s1.toVehicleDetailUiState(severeIntervalOn = true), onBack = {}) }

    // Scenario 1 with the table's rows named by the manual's phrases instead of the short names
    // (the alternative form behind the state's tableForm)
    @Test fun vehicleDetailPhrases() = capture("vehicle_detail_phrases", units = "4; list 4 rows") {
        VehicleDetailScreen(s1.toVehicleDetailUiState().copy(tableForm = SevereTableForm.ManualPhrase), onBack = {})
    }

    @Test fun vehicleDetailS2() = capture("vehicle_detail_s2", units = "4; list 4 rows") { VehicleDetailScreen(s2.toVehicleDetailUiState(), onBack = {}) }

    @Test fun vehicleDetailS2Severe() = capture("vehicle_detail_s2_severe", units = "4; list 4 rows") { VehicleDetailScreen(s2.toVehicleDetailUiState(severeIntervalOn = true), onBack = {}) }

    // Screen B
    @Test fun evidence() = capture("evidence", units = "list 4 rows + sum + index") { EvidenceScreen(s1.toEvidenceUiState(), onBack = {}) }

    // Screen C
    @Test fun briefingRecord() = capture("briefing_record", units = "2") { BriefingScreen(s2.toBriefingUiState(BriefingCase.AfterService)) }

    @Test fun briefingHandover() = capture("briefing_handover", units = "3") { BriefingScreen(s2.toBriefingUiState(BriefingCase.Handover)) }

    @Test fun briefingNone() = capture("briefing_none", units = "2") { BriefingScreen(s2.toBriefingUiState(BriefingCase.Unchanged)) }

    // Screen D
    @Test fun weekly() = capture("weekly", units = "4") { WeeklyScreen(s3.toWeeklyUiState(), onBack = {}) }

    // Screen E at 13:00 (D), 14:15 (the rest stop starts), 16:30 (the voice prompt), and after 17:10 (steps done, notification)
    @Test fun replay1300() = capture("replay_1300", units = "panel 5 signals") { ReplayScreen(s1.replayStateAt(s1.replayStart)) }

    @Test fun replay1415() = capture("replay_1415", units = "panel 5 signals") { ReplayScreen(s1.replayStateAt(s1.replay.restStop.first)) }

    @Test fun replay1630() = capture("replay_1630", units = "panel 5 signals") { ReplayScreen(s1.replayStateAt(s1.replay.voicePromptEpochSec)) }

    @Test fun replay1710() = capture("replay_1710", units = "panel 5 signals") { ReplayScreen(s1.replayStateAt(s1.replayEnd + ReplayTiming.NOTICE_AFTER_END_SEC)) }

    /** The whole Connect-S display with placeholders for the system parts and the app window in place. */
    @Test
    @Config(qualifiers = "ko-rKR-w2560dp-h1440dp-land-mdpi")
    fun displayContext() = capture("display_context", count = false, still = false) {
        DisplayContext { TripEndScreen(s1.toTripEndUiState(selfCheck = 6)) }
    }

    @Test fun pictograms() = capture("layout_check/pictograms", count = false, still = false) { PictogramSheet() }

    private companion object {
        /** The engine's scenarios (scenario 1 matches the application's values; see TripEndMappingTest). */
        val s1 by lazy { DemoScenarios.scenario1() }
        val s2 by lazy { DemoScenarios.scenario2() }
        val s3 by lazy { DemoScenarios.scenario3() }
    }

    private fun capture(
        name: String,
        side: DriverSide = DefaultDriverSide,
        count: Boolean = true,
        still: Boolean = true,
        units: String = "",
        screen: @Composable () -> Unit,
    ) {
        val path = "build/outputs/roborazzi/$name.png"
        File(path).parentFile?.mkdirs()
        compose.setContent { DndTheme(driverSide = side, content = screen) }
        compose.onRoot().captureRoboImage(path)
        if (count) {
            val texts = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .flatMap { node -> node.config[SemanticsProperties.Text].map { it.text } }
            TextCounts.record(name, texts, units)
            Bounds.record(name, compose)
        }
        if (still) {
            val target = File("build/outputs/roborazzi/video/still_$name.png")
            target.parentFile?.mkdirs()
            File(path).copyTo(target, overwrite = true)
        }
    }
}

/** Every pictogram in the size classes, for checking the drawings. */
@Composable
private fun PictogramSheet() {
    Column(
        Modifier.fillMaxSize().background(DndTheme.palette.background).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        listOf(PictogramSize.Small, PictogramSize.Medium, PictogramSize.Large).forEach { size ->
            DndIcons.all.chunked(15).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                    row.forEach { Pictogram(it, size = size) }
                }
            }
        }
    }
}
