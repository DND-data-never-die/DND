package dnd.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dnd.app.ui.components.SingleLineText
import dnd.app.ui.icons.DndIcon
import dnd.app.ui.icons.DndIcons
import dnd.app.ui.icons.Pictogram
import dnd.app.ui.icons.PictogramSize
import dnd.app.ui.theme.DndCanvas
import dnd.app.ui.theme.DndMotion
import dnd.app.ui.theme.DndShapes
import dnd.app.ui.theme.DndSpace
import dnd.app.ui.theme.DndTheme
import dnd.app.ui.theme.fadeMove

/** What the launch screen shows. */
data class LaunchState(
    val wordmark: String = "DND",
    val tagline: String = "Data Never Dies",
    val vehicleLabel: String = "차량 카드",
    val driverLabel: String = "운전자 카드",
)

private val CardWidth = 440.dp
private val CardHeight = 280.dp
/** The app screens' card gap, so the two read as the pair they become on the trip-end screen (r4 D13). */
private val CardGap = DndSpace.cardGap

/**
 * The launch screen at [progress] (0..1 of [DndMotion.LAUNCH_MS]), a pure function of it: the
 * wordmark fades in over 300 ms, the tagline follows, then the two cards fade in, 운전자 카드 left
 * and 차량 카드 right as on the trip-end screen (owner, 2026-11-15: no fork, only the two cards,
 * larger, fading). Fades only, each within the guide's 100 to 450 ms. Everything is in place at
 * [DndMotion.LAUNCH_FADE_START_MS]; from there the screen fades out over the first app screen.
 * [exit] (0..1 of that fade) starts it earlier when the app is ready sooner.
 */
@Composable
fun LaunchScreen(progress: Float, modifier: Modifier = Modifier, state: LaunchState = LaunchState(), exit: Float = 0f) {
    val palette = DndTheme.palette
    val type = DndTheme.type
    val t = progress.coerceIn(0f, 1f) * DndMotion.LAUNCH_MS
    val out = maxOf(
        DndMotion.phase(t, DndMotion.LAUNCH_FADE_START_MS, DndMotion.LAUNCH_MS),
        DndMotion.Easing.transform(exit.coerceIn(0f, 1f)),
    )
    DndCanvas(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - out }
            .background(palette.background),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SingleLineText(
                state.wordmark,
                type.keyValue.copy(fontSize = 160.sp, lineHeight = 168.sp, letterSpacing = 0.08.em),
                Modifier.fadeMove(DndMotion.linear(t, 0, DndMotion.LAUNCH_WORDMARK_END_MS), 1f),
            )
            SingleLineText(
                state.tagline,
                // Light tracking for lowercase (r4 D13).
                type.label.copy(letterSpacing = 0.04.em),
                Modifier.fadeMove(DndMotion.linear(t, DndMotion.LAUNCH_TAGLINE_START_MS, DndMotion.LAUNCH_TAGLINE_END_MS), 1f),
                color = palette.textSecondary,
            )
            Spacer(Modifier.height(DndSpace.xxxl + DndSpace.xxl))
            Row(horizontalArrangement = Arrangement.spacedBy(CardGap)) {
                val cards = DndMotion.LAUNCH_CARDS_START_MS
                // Driver card left, vehicle card right, as on the trip-end screen (red team M4).
                MiniCard(DndIcons.Driver, state.driverLabel, DndMotion.linear(t, cards, DndMotion.LAUNCH_FADE_START_MS - DndMotion.STAGGER_MS))
                MiniCard(DndIcons.Car, state.vehicleLabel, DndMotion.linear(t, cards + DndMotion.STAGGER_MS, DndMotion.LAUNCH_FADE_START_MS))
            }
            // The group sits 40 dp above the centre: its optical centre (r4 D13).
            Spacer(Modifier.height(80.dp))
        }
    }
}

/** An outlined card: one pictogram and its name. */
@Composable
private fun MiniCard(icon: DndIcon, label: String, fade: Float) {
    val palette = DndTheme.palette
    Column(
        Modifier
            .fadeMove(fade, 1f)
            .size(width = CardWidth, height = CardHeight)
            .background(palette.surface, DndShapes.card)
            .border(DndSpace.outlineWidth, palette.outline, DndShapes.card),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Pictogram(icon, size = PictogramSize.Hero)
        Spacer(Modifier.height(DndSpace.m))
        SingleLineText(label, DndTheme.type.keyText)
    }
}
