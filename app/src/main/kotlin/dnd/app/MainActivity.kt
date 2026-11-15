package dnd.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dnd.app.ui.screens.LaunchScreen
import dnd.app.ui.theme.DndMotion
import dnd.app.ui.theme.DndTheme
import dnd.core.demo.DemoScenarios
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Intent extras (for `adb shell am start -n io.github.dnddataneverdie.dnd/dnd.app.MainActivity ...`):
 * `--es scenario s1|s2|s3` picks the first screen, `--ez autoplay true` starts the scenario 1
 * time-lapse at once. A run started with a scenario is a driven run (recordings), so the "데모"
 * button stays hidden unless `--ez demo_controls true` asks for it.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Full screen, as on the Connect display: the app draws the whole 16:9 area.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).run {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        val scenario = intent?.getStringExtra(EXTRA_SCENARIO)
        val autoplay = intent?.getBooleanExtra(EXTRA_AUTOPLAY, false) ?: false
        val demoControls = intent?.getBooleanExtra(EXTRA_DEMO_CONTROLS, scenario == null) ?: true
        val coldStart = savedInstanceState == null
        setContent {
            DndTheme {
                // The engine replays the scenarios' trip logs; run it off the main thread.
                val data by produceState<ScenarioData?>(initialValue = null) {
                    value = withContext(Dispatchers.Default) {
                        ScenarioData(DemoScenarios.scenario1(), DemoScenarios.scenario2(), DemoScenarios.scenario3())
                    }
                }
                val navigator = remember { Navigator(startRoutes(scenario, autoplay)) }
                val ready = data
                LaunchedEffect(ready != null) { if (ready != null) Log.i(LogTag, "ready scenario=${scenario ?: "default"}") }
                // Launch sequence on a cold start only: it plays up to its hold point, or less when the
                // scenarios are ready sooner, then fades (250 ms) into the first screen drawn underneath.
                val intro = remember { Animatable(if (coldStart) 0f else 1f) }
                val exit = remember { Animatable(if (coldStart) 0f else 1f) }
                val hold = DndMotion.LAUNCH_FADE_START_MS / DndMotion.LAUNCH_MS.toFloat()
                LaunchedEffect(Unit) {
                    if (!coldStart) return@LaunchedEffect
                    coroutineScope {
                        val play = launch { intro.animateTo(hold, tween(DndMotion.LAUNCH_FADE_START_MS, easing = LinearEasing)) }
                        snapshotFlow { data }.first { it != null }
                        play.cancel()
                    }
                    exit.animateTo(1f, tween(DndMotion.LAUNCH_MS - DndMotion.LAUNCH_FADE_START_MS, easing = LinearEasing))
                }
                Box(Modifier.fillMaxSize().background(DndTheme.palette.background)) {
                    if (ready != null) DndApp(ready, navigator, showDemoControls = demoControls)
                    if (exit.value < 1f) LaunchScreen(intro.value, exit = exit.value)
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENARIO = "scenario"
        const val EXTRA_AUTOPLAY = "autoplay"
        const val EXTRA_DEMO_CONTROLS = "demo_controls"
    }
}
