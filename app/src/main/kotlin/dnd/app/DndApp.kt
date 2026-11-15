package dnd.app

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import dnd.app.ui.components.DemoSheet
import dnd.app.ui.model.BriefingCase
import dnd.app.ui.model.ReplayTiming
import dnd.app.ui.model.replayEnd
import dnd.app.ui.model.replayStart
import dnd.app.ui.model.replayStateAt
import dnd.app.ui.model.toBriefingUiState
import dnd.app.ui.model.toEvidenceUiState
import dnd.app.ui.model.toTripEndUiState
import dnd.app.ui.model.toVehicleDetailUiState
import dnd.app.ui.model.toWeeklyUiState
import dnd.app.ui.screens.BriefingScreen
import dnd.app.ui.screens.EvidenceScreen
import dnd.app.ui.screens.ReplayScreen
import dnd.app.ui.screens.TripEndScreen
import dnd.app.ui.screens.VehicleDetailScreen
import dnd.app.ui.screens.WeeklyScreen
import dnd.app.ui.theme.DndMotion
import dnd.core.demo.Scenario1Screens
import dnd.core.demo.Scenario2Screens
import dnd.core.demo.Scenario3Screens
import kotlinx.coroutines.delay

/** The engine's three scenarios, computed once when the app starts. */
class ScenarioData(val s1: Scenario1Screens, val s2: Scenario2Screens, val s3: Scenario3Screens)

sealed interface Route {
    /** [tour]: opened by the unattended scenario 1 run; moves on to 상세 정보 by itself. */
    data class TripEnd(val scenario: Int, val tour: Boolean = false) : Route
    data class Evidence(val tour: Boolean = false) : Route
    /** 차량 카드 자세히 for scenario 1 or 2. */
    data class VehicleDetail(val scenario: Int) : Route
    data class Briefing(val case: BriefingCase) : Route
    data object Weekly : Route
    data class Replay(val autoplay: Boolean) : Route
}

/** The detail screens behind a card's action; they open with the detail transition. */
private val Route.isDetail: Boolean get() = this is Route.Evidence || this is Route.VehicleDetail

enum class NavOp { Push, Pop, Reset }

/** A back stack without a navigation library; [lastOp] picks the transition. */
class Navigator(start: List<Route>) {
    private val stack = mutableStateListOf<Route>().apply { addAll(start) }
    var lastOp by mutableStateOf(NavOp.Reset)
        private set
    /** Counts resets, so a scenario started again from the sheet starts with fresh choices. */
    var generation by mutableStateOf(0)
        private set
    val current: Route get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1
    fun push(route: Route) {
        lastOp = NavOp.Push
        stack.add(route)
    }
    fun pop() {
        if (stack.size > 1) {
            lastOp = NavOp.Pop
            stack.removeAt(stack.lastIndex)
        }
    }
    fun reset(routes: List<Route>) {
        lastOp = NavOp.Reset
        generation++
        stack.clear()
        stack.addAll(routes)
    }
}

/**
 * First screens for the intent extra "scenario": s1 plays the trip log, s3 opens the weekly
 * summary, s2 (and no extra) opens the briefing. Scenario 1 and 3 have no briefing in the engine.
 */
fun startRoutes(scenario: String?, autoplay: Boolean): List<Route> = when (scenario) {
    "s1" -> listOf(Route.Replay(autoplay))
    "s3" -> listOf(Route.Weekly)
    else -> listOf(Route.Briefing(BriefingCase.AfterService))
}

/** What each entry of the scenario sheet opens; the last route is shown, back returns along the list. */
private fun scenarioRoutes(index: Int): List<Route> = when (index) {
    0 -> listOf(Route.Replay(autoplay = true))
    1 -> listOf(Route.Briefing(BriefingCase.AfterService), Route.VehicleDetail(2))
    else -> listOf(Route.Weekly)
}

/** Log tag; the device workflow waits for these lines. */
const val LogTag = "DND"

/** How long the unattended run holds the cards and the evidence screen. */
private const val TourHoldMillis = 6_000L

/** How long the notification stays before the unattended run opens the cards. */
private const val NoticeHoldMillis = 1_500L

private const val SheetTitle = "데모"
private val SheetItems = listOf("상황 1", "상황 2", "상황 3")

@Composable
fun DndApp(data: ScenarioData, navigator: Navigator, showDemoControls: Boolean = true) {
    var sheetOpen by remember { mutableStateOf(false) }
    val back: (() -> Unit)? = if (navigator.canGoBack) ({ navigator.pop() }) else null
    BackHandler(enabled = navigator.canGoBack) { navigator.pop() }
    val openSheet = { sheetOpen = true }
    // Keeps each screen's choices (self-check, severe switch) while a detail screen is on top.
    val saved = rememberSaveableStateHolder()
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = navigator.current,
            transitionSpec = { screenTransition(navigator.lastOp) },
            label = "screen",
        ) { route ->
            // The generation this screen was opened in, kept while it fades out after a reset.
            val generation = remember { navigator.generation }
            saved.SaveableStateProvider("$generation $route") {
                when (route) {
                    is Route.TripEnd -> key(route) {
                        var selfCheck by rememberSaveable { mutableStateOf<Int?>(null) }
                        // Coming back from a detail screen, the cards are already there: no entry motion.
                        val entry = remember { navigator.lastOp != NavOp.Pop }
                        val state = if (route.scenario == 1) data.s1.toTripEndUiState(selfCheck) else data.s2.toTripEndUiState(selfCheck)
                        // Screen B explains scenario 1's driver card, so only scenario 1 links to it.
                        val evidence: (() -> Unit)? = if (route.scenario == 1) ({ navigator.push(Route.Evidence()) }) else null
                        if (route.tour) {
                            LaunchedEffect(route) {
                                Log.i(LogTag, "cards shown")
                                delay(TourHoldMillis)
                                navigator.push(Route.Evidence(tour = true))
                            }
                        }
                        TripEndScreen(
                            state = state,
                            showDemoControls = showDemoControls,
                            onDemoClick = openSheet,
                            onBack = back,
                            onDriverEvidence = evidence,
                            onVehicleDetail = { navigator.push(Route.VehicleDetail(route.scenario)) },
                            onSelfCheck = { selfCheck = it },
                            enter = if (entry) rememberEntry() else 1f,
                        )
                    }
                    is Route.Evidence -> key(route) {
                        if (route.tour) {
                            LaunchedEffect(route) {
                                Log.i(LogTag, "evidence shown")
                                delay(TourHoldMillis)
                                Log.i(LogTag, "tour done")
                            }
                        }
                        EvidenceScreen(data.s1.toEvidenceUiState(), showDemoControls = showDemoControls, onDemoClick = openSheet, onBack = back)
                    }
                    is Route.VehicleDetail -> key(route) {
                        var severe by rememberSaveable { mutableStateOf(false) }
                        val state = if (route.scenario == 1) data.s1.toVehicleDetailUiState(severe) else data.s2.toVehicleDetailUiState(severe)
                        VehicleDetailScreen(
                            state,
                            showDemoControls = showDemoControls,
                            onDemoClick = openSheet,
                            onBack = back,
                            onSevereIntervalChange = { severe = it },
                        )
                    }
                    is Route.Briefing -> BriefingScreen(
                        data.s2.toBriefingUiState(route.case),
                        showDemoControls = showDemoControls,
                        onDemoClick = openSheet,
                        onBack = back,
                        onWeekly = { navigator.push(Route.Weekly) },
                    )
                    Route.Weekly -> WeeklyScreen(data.s3.toWeeklyUiState(), showDemoControls = showDemoControls, onDemoClick = openSheet, onBack = back)
                    is Route.Replay -> key(route) {
                        ReplayPlayer(
                            data.s1,
                            autoplay = route.autoplay,
                            showDemoControls = showDemoControls,
                            onDemoClick = openSheet,
                            onBack = back,
                            onOpenCards = { navigator.push(Route.TripEnd(1)) },
                            // Unattended run: after the time-lapse the cards open by themselves.
                            onFinished = if (route.autoplay) ({ navigator.push(Route.TripEnd(1, tour = true)) }) else null,
                        )
                    }
                }
            }
        }
        if (sheetOpen) {
            DemoSheet(
                title = SheetTitle,
                items = SheetItems,
                onPick = { i ->
                    sheetOpen = false
                    navigator.reset(scenarioRoutes(i))
                },
                onDismiss = { sheetOpen = false },
            )
        }
    }
}

/**
 * Screen changes. A card's action opens its detail screen: the new screen fades in and grows from
 * 92 % over the old one (300 ms). Back reverses it in 250 ms: the detail screen shrinks and fades
 * out over the screen underneath. Everything else is a 300 ms fade-through.
 */
private fun AnimatedContentTransitionScope<Route>.screenTransition(op: NavOp): ContentTransform = when {
    op == NavOp.Push && targetState.isDetail ->
        (fadeIn(tween(DndMotion.DETAIL_OPEN_MS, easing = DndMotion.Easing)) +
            scaleIn(tween(DndMotion.DETAIL_OPEN_MS, easing = DndMotion.Easing), initialScale = DndMotion.DETAIL_SCALE_FROM))
            .togetherWith(ExitTransition.KeepUntilTransitionsFinished)
    op == NavOp.Pop && initialState.isDetail ->
        EnterTransition.None.togetherWith(
            fadeOut(tween(DndMotion.BACK_MS, easing = DndMotion.Easing)) +
                scaleOut(tween(DndMotion.BACK_MS, easing = DndMotion.Easing), targetScale = DndMotion.DETAIL_SCALE_FROM),
        ).apply { targetContentZIndex = -1f }
    else -> {
        val inMs = DndMotion.FADE_THROUGH_MS - DndMotion.FADE_THROUGH_OUT_MS
        (fadeIn(tween(inMs, DndMotion.FADE_THROUGH_OUT_MS, DndMotion.Easing)) +
            scaleIn(tween(inMs, DndMotion.FADE_THROUGH_OUT_MS, DndMotion.Easing), initialScale = DndMotion.DETAIL_SCALE_FROM))
            .togetherWith(fadeOut(tween(DndMotion.FADE_THROUGH_OUT_MS, easing = DndMotion.Easing)))
    }
}

/** Entry progress of a card screen: 0 to 1 over [DndMotion.ENTER_MS], once per screen. */
@Composable
private fun rememberEntry(): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(DndMotion.ENTER_MS, easing = LinearEasing)) }
    return progress.value
}

/** One stretch of the time-lapse: scenario seconds [from, to] played in [millis]. */
private class Stretch(val from: Long, val to: Long, val millis: Long)

/**
 * About 20 seconds for 13:00 to 17:10, slower where something appears (the first notification,
 * the voice prompt, the processing steps). Display choices, not engine definitions.
 */
private fun timeLapse(s: Scenario1Screens): List<Stretch> {
    val r = s.replay
    val leg1End = r.legs.first().second
    val leg2Start = r.legs[1].first
    val voice = r.voicePromptEpochSec
    val voiceEnd = voice + ReplayTiming.VOICE_PROMPT_SEC
    val end = s.replayEnd
    return listOf(
        Stretch(s.replayStart, leg1End, 4_500),
        Stretch(leg1End, leg1End + 60, 1_200),
        Stretch(leg1End + 60, leg2Start, 600),
        Stretch(leg2Start, voice, 6_000),
        Stretch(voice, voiceEnd, 2_500),
        Stretch(voiceEnd, end, 2_000),
        Stretch(end, end + ReplayTiming.NOTICE_AFTER_END_SEC, 2_400),
    )
}

@Composable
private fun ReplayPlayer(
    s1: Scenario1Screens,
    autoplay: Boolean,
    showDemoControls: Boolean,
    onDemoClick: () -> Unit,
    onBack: (() -> Unit)?,
    onOpenCards: () -> Unit,
    onFinished: (() -> Unit)?,
) {
    var t by remember { mutableStateOf(s1.replayStart) }
    var playing by remember { mutableStateOf(autoplay) }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        Log.i(LogTag, "replay playing")
        for (stretch in timeLapse(s1)) {
            val start = withFrameNanos { it }
            while (true) {
                val elapsedMs = (withFrameNanos { it } - start) / 1_000_000.0
                val f = (elapsedMs / stretch.millis).coerceAtMost(1.0)
                t = stretch.from + ((stretch.to - stretch.from) * f).toLong()
                if (f >= 1.0) break
            }
        }
        Log.i(LogTag, "replay finished")
        if (onFinished != null) {
            delay(NoticeHoldMillis)
            onFinished()
        }
    }
    val state = remember(t) { s1.replayStateAt(t) }
    // Before playback starts, a tap anywhere starts it.
    Box(
        Modifier
            .fillMaxSize()
            .clickable(enabled = !playing, interactionSource = remember { MutableInteractionSource() }, indication = null) { playing = true },
    ) {
        ReplayScreen(state, showDemoControls = showDemoControls, onDemoClick = onDemoClick, onBack = onBack, onOpenCards = onOpenCards)
    }
}
