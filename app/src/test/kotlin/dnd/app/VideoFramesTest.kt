package dnd.app

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import dnd.app.ui.model.ReplayTiming
import dnd.app.ui.model.TripEndUiState
import dnd.app.ui.model.replayEnd
import dnd.app.ui.model.replayStart
import dnd.app.ui.model.replayStateAt
import dnd.app.ui.model.toEvidenceUiState
import dnd.app.ui.model.toTripEndUiState
import dnd.app.ui.model.toVehicleDetailUiState
import dnd.app.ui.screens.EVIDENCE_BUILD_STEP_MS
import dnd.app.ui.screens.EvidenceScreen
import dnd.app.ui.screens.LaunchScreen
import dnd.app.ui.screens.ReplayScreen
import dnd.app.ui.screens.TripEndScreen
import dnd.app.ui.screens.VehicleDetailScreen
import dnd.app.ui.screens.buildStages
import dnd.app.ui.theme.DndMotion
import dnd.app.ui.theme.DndTheme
import dnd.app.ui.theme.motion
import dnd.core.demo.DemoScenarios
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Locale

/**
 * Frames for the demo video at the capture size, 1700 x 1184 px (the app window, 1 dp = 1 px), so
 * sequences and the still_<name>.png copies written by ScreenCaptureTest come from one rasteriser.
 * To be played at 30 fps, written to app/build/outputs/roborazzi/video/:
 * - intro_0000 … intro_0029: the launch composition building up over 0.75 s (frames 0-22), then
 *   held finished (frames 23-29); no fade into another screen
 * - replay_0000 … replay_0750: the trip log every 20 scenario seconds, 13:00:00 to 17:10:00
 *   (the events at 14:15:00 and 16:30:00 fall on frames 225 and 630)
 * - replay_0751 … replay_0753: the three processing steps ticking; replay_0754: the notification
 * - enter_trip_end_0000 … 0026: the 0.9 s entry of the cards (nothing selected yet)
 * - enter_vehicle_detail_0000 … 0026, enter_evidence_0000 … 0026: each detail screen opening over
 *   trip_end with self-check 6 selected (0.3 s, then held to 0.9 s)
 * - evidence_build_0000 … 0075: the four item rows appear one after another, 연속 주행 first, then
 *   the total; the last frame equals evidence.png
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ko-rKR-w1700dp-h1184dp-land-mdpi")
class VideoFramesTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun intro() {
        val ms = mutableFloatStateOf(0f)
        compose.setContent { DndTheme { LaunchScreen(ms.floatValue / DndMotion.LAUNCH_MS) } }
        for (i in 0 until INTRO_FRAMES) {
            ms.floatValue = minOf(i * 1_000f / FPS, DndMotion.LAUNCH_FADE_START_MS.toFloat())
            compose.waitForIdle()
            compose.onRoot().captureRoboImage(path(name("intro", i)))
        }
    }

    @Test
    fun replay() {
        val start = s1.replayStart
        val end = s1.replayEnd
        val instants = (0..((end - start) / REPLAY_STEP_SEC).toInt()).map { start + it * REPLAY_STEP_SEC } +
            (1..ReplayTiming.STEP_COUNT).map { end + it.toLong() * ReplayTiming.STEP_SEC } +
            (end + ReplayTiming.NOTICE_AFTER_END_SEC)
        val time = mutableLongStateOf(start)
        compose.setContent { DndTheme { ReplayScreen(s1.replayStateAt(time.longValue)) } }
        instants.forEachIndexed { i, t ->
            time.longValue = t
            compose.waitForIdle()
            compose.onRoot().captureRoboImage(path(name("replay", i)))
        }
    }

    @Test
    fun tripEnd() {
        val state = s1.toTripEndUiState()
        sequence("enter_trip_end", DndMotion.ENTER_MS * FPS / 1_000) { TripEndScreen(state, enter = it) }
    }

    @Test
    fun vehicleDetail() {
        val state = s1.toVehicleDetailUiState()
        detailOpen("enter_vehicle_detail", s1.toTripEndUiState(selfCheck = 6)) { VehicleDetailScreen(state, it, onBack = {}) }
    }

    @Test
    fun evidence() {
        val state = s1.toEvidenceUiState()
        detailOpen("enter_evidence", s1.toTripEndUiState(selfCheck = 6)) { EvidenceScreen(state, it, onBack = {}) }
    }

    @Test
    fun evidenceBuild() {
        val state = s1.toEvidenceUiState()
        val totalMs = state.buildStages() * EVIDENCE_BUILD_STEP_MS
        val ms = mutableFloatStateOf(0f)
        compose.setContent { DndTheme { EvidenceScreen(state, onBack = {}, buildMs = ms.floatValue) } }
        // Frame k shows k/30 s; the last frame is the finished screen.
        for (i in 0..(totalMs * FPS / 1_000)) {
            ms.floatValue = i * 1_000f / FPS
            compose.waitForIdle()
            compose.onRoot().captureRoboImage(path(name("evidence_build", i)))
        }
    }

    private companion object {
        const val FPS = 30
        const val REPLAY_STEP_SEC = 20L
        const val INTRO_FRAMES = 30

        val s1 by lazy { DemoScenarios.scenario1() }

        fun name(prefix: String, i: Int) = String.format(Locale.ROOT, "%s_%04d", prefix, i)

        fun path(name: String): String {
            val path = "build/outputs/roborazzi/video/$name.png"
            File(path).parentFile?.mkdirs()
            return path
        }
    }

    /** [frames] frames at 30 fps; frame i shows progress i / frames, so the next clip starts at 1. */
    private fun sequence(prefix: String, frames: Int, screen: @Composable (Float) -> Unit) {
        val progress = mutableFloatStateOf(0f)
        compose.setContent { DndTheme { screen(progress.floatValue) } }
        for (i in 0 until frames) {
            progress.floatValue = i / frames.toFloat()
            compose.waitForIdle()
            compose.onRoot().captureRoboImage(path(name(prefix, i)))
        }
    }

    /**
     * A detail screen opening over the cards as in the app: it fades in and grows from 92 % over
     * [DndMotion.DETAIL_OPEN_MS], then holds; [DndMotion.ENTER_MS] of frames in all.
     */
    private fun detailOpen(prefix: String, under: TripEndUiState, detail: @Composable (Modifier) -> Unit) {
        sequence(prefix, DndMotion.ENTER_MS * FPS / 1_000) { progress ->
            val p = DndMotion.phase(progress * DndMotion.ENTER_MS, 0, DndMotion.DETAIL_OPEN_MS)
            Box {
                TripEndScreen(under)
                detail(Modifier.motion(p, scaleFrom = DndMotion.DETAIL_SCALE_FROM))
            }
        }
    }
}
