package dnd.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dnd.app.ui.components.NoticeCard
import dnd.app.ui.components.ScreenScaffold
import dnd.app.ui.components.SingleLineText
import dnd.app.ui.components.StepRow
import dnd.app.ui.components.driverBottomAlignment
import dnd.app.ui.components.icon
import dnd.app.ui.icons.DndIcons
import dnd.app.ui.icons.Pictogram
import dnd.app.ui.icons.PictogramSize
import dnd.app.ui.model.DemoPanelState
import dnd.app.ui.model.ReplayState
import dnd.app.ui.model.SignalValue
import dnd.app.ui.model.LabelAnchor
import dnd.app.ui.model.TimelineState
import dnd.app.ui.theme.DndShapes
import dnd.app.ui.theme.DndSpace
import dnd.app.ui.theme.DndTheme
import dnd.app.ui.theme.DriverSide

private val TrackHeight = 16.dp
private val ThumbSize = 40.dp
private val EventLabelWidth = 320.dp

/**
 * Screen E: the trip log replayed. Above, the app window: empty in D (the app shows nothing while
 * driving); in P the app's notification on the driver's side, and after the last trip the
 * processing steps. Below, the dark demo panel, which reads as outside the app: the clock, the
 * timeline with its times, the recorded signals with their live values (the gear among them), and
 * the voice prompt.
 */
@Composable
fun ReplayScreen(
    state: ReplayState,
    modifier: Modifier = Modifier,
    showDemoControls: Boolean = false,
    onDemoClick: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onOpenCards: () -> Unit = {},
) {
    ScreenScaffold(if (state.driving) null else state.topBar, modifier, showDemoControls, onDemoClick, onBack) {
        Box(Modifier.fillMaxWidth().weight(1f).testTag("app_area")) {
            if (state.steps.isNotEmpty()) {
                Column(
                    Modifier.align(if (DndTheme.driverSide == DriverSide.LEFT) Alignment.TopStart else Alignment.TopEnd).padding(horizontal = DndSpace.m),
                    verticalArrangement = Arrangement.spacedBy(DndSpace.xs),
                ) {
                    state.steps.forEach { StepRow(it.kind.icon(), it) }
                }
            }
            if (!state.driving) {
                state.notice?.let { NoticeCard(it, onOpenCards, Modifier.align(driverBottomAlignment), buttonTag = "notice_button") }
            }
        }
        Spacer(Modifier.height(DndSpace.l))
        DemoPanel(state.panel, Modifier.testTag("demo_panel"))
    }
}

/** The demo's player panel: dark, with light text; colours fixed so it never reads as the app. */
@Composable
private fun DemoPanel(panel: DemoPanelState, modifier: Modifier) {
    val palette = DndTheme.palette
    val type = DndTheme.type
    val fg = palette.surface
    val muted = fg.copy(alpha = 0.72f)
    Column(
        modifier
            .fillMaxWidth()
            .background(palette.text, DndShapes.card)
            .padding(horizontal = DndSpace.xl, vertical = DndSpace.l),
    ) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Pictogram(DndIcons.Play, size = PictogramSize.Small, tint = fg)
            Spacer(Modifier.width(DndSpace.s))
            SingleLineText(panel.title, type.labelStrong, Modifier.weight(1f), color = fg)
            // The clock alone: the 기어 signal below shows D / P.
            SingleLineText(panel.clock, type.keyText.tabular(), color = fg)
        }
        Spacer(Modifier.height(DndSpace.s))
        Timeline(panel.timeline, fg, muted)
        // The same gap above and below the voice line as above the timeline.
        Spacer(Modifier.height(DndSpace.s))
        // The voice prompt: one line from its second, for its duration, right under its event on the
        // timeline; the space stays reserved so nothing moves between frames (r4 D8).
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            panel.voicePrompt?.let {
                // Two elements: the label that says the voice is not built yet, then the sentence.
                Pictogram(DndIcons.Speaker, size = PictogramSize.Small, tint = fg)
                Spacer(Modifier.width(DndSpace.s))
                SingleLineText(panel.voiceLabel, type.bodyStrong, color = fg)
                Spacer(Modifier.width(DndSpace.l))
                SingleLineText(it, type.body, color = fg)
            }
        }
        // The signals follow under the voice line with the gap the header has above the timeline;
        // their names say what they are (owner, 2026-11-15: no sub-title).
        Spacer(Modifier.height(DndSpace.s))
        // Equal gaps between the five signals. Each column is as wide as its label or its widest value,
        // whichever is wider, so the columns stay where they are while the values change.
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val labelStyle = type.label
        val valueStyle = type.keyText.tabular()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            panel.signals.forEach { s ->
                val width = remember(s.label, s.widest, labelStyle, valueStyle, density) {
                    val labelPx = measurer.measure(s.label, labelStyle).size.width
                    val valuePx = s.widest.maxOf { measurer.measure(it, valueStyle).size.width }
                    // 2 dp of slack: where the display's scale is not a whole number the dp-to-pixel rounding can
                    // come out a pixel short, and the label would be cut to an ellipsis (seen on the emulator).
                    with(density) { maxOf(PictogramSize.Small.size + DndSpace.xs + labelPx.toDp(), valuePx.toDp()) + 2.dp }
                }
                Signal(s, fg, muted, Modifier.width(width))
            }
        }
    }
}

/** One recorded signal: pictogram and name, the live value under them. */
@Composable
private fun Signal(signal: SignalValue, fg: Color, muted: Color, modifier: Modifier) {
    val type = DndTheme.type
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pictogram(signal.kind.icon(), size = PictogramSize.Small, tint = muted)
            Spacer(Modifier.width(DndSpace.xs))
            SingleLineText(signal.label, type.label, color = muted)
        }
        SingleLineText(signal.value, type.keyText.tabular(), color = fg)
    }
}

/**
 * Driving legs (played part solid, the rest faint) and the thumb. Under the track the times alone:
 * a leg's start time begins at the leg's start, its end time ends at the leg's end, a time inside a
 * leg is centred under a tick. Above the track the names of what happened (the rest over the gap
 * between its two times, the voice prompt over its time). The latest time and its name are drawn
 * bold and bright.
 */
@Composable
private fun Timeline(timeline: TimelineState, fg: Color, muted: Color) {
    // The unplayed part at 3:1 against the panel (WCAG 2.1 SC 1.4.11).
    val faint = DndTheme.palette.outlineStrong
    BoxWithConstraints(Modifier.fillMaxWidth().height(52.dp + 8.dp + ThumbSize + 8.dp + 52.dp)) {
        // The track is inset by half the thumb at both ends, so the thumb stays inside the panel.
        val inset = ThumbSize / 2
        val w = maxWidth - ThumbSize
        timeline.marks.forEach {
            TimelineLabel(it.label, it.active, inset + w * it.at - EventLabelWidth / 2, 0.dp, TextAlign.Center, fg, muted)
        }
        val trackTop = 52.dp + 8.dp + (ThumbSize - TrackHeight) / 2
        timeline.legs.forEach { leg ->
            val x0 = inset + w * leg.start
            val x1 = inset + w * leg.endInclusive
            Box(Modifier.offset(x = x0, y = trackTop).width(x1 - x0).height(TrackHeight).background(faint, DndShapes.pill))
            val done = timeline.cursor.coerceIn(leg.start, leg.endInclusive) - leg.start
            if (done > 0f) {
                Box(Modifier.offset(x = x0, y = trackTop).width(w * done).height(TrackHeight).background(fg, DndShapes.pill))
            }
        }
        // A tick from the track to a time inside a leg; a leg's own ends need none.
        timeline.events.filter { it.anchor == LabelAnchor.Center }.forEach {
            Box(Modifier.offset(x = inset + w * it.at - 2.dp, y = trackTop + TrackHeight).width(4.dp).height(52.dp + 8.dp + ThumbSize + 4.dp - trackTop - TrackHeight).background(muted))
        }
        Box(
            Modifier
                .offset(x = w * timeline.cursor, y = 52.dp + 8.dp)
                .size(ThumbSize)
                .background(fg, CircleShape)
                .border(8.dp, DndTheme.palette.info, CircleShape),
        )
        val belowTop = 52.dp + 8.dp + ThumbSize + 8.dp
        // A label at a leg's end lines up with the thumb's outer edge when the thumb is at that time.
        timeline.events.forEach {
            val place = inset + w * it.at
            when (it.anchor) {
                LabelAnchor.Start -> TimelineLabel(it.label, it.active, place - inset, belowTop, TextAlign.Start, fg, muted)
                LabelAnchor.End -> TimelineLabel(it.label, it.active, place + inset - EventLabelWidth, belowTop, TextAlign.End, fg, muted)
                LabelAnchor.Center -> TimelineLabel(it.label, it.active, place - EventLabelWidth / 2, belowTop, TextAlign.Center, fg, muted)
            }
        }
    }
}

/**
 * Tabular figures (Asta Sans has the OpenType "tnum" feature): every digit has the same advance,
 * so a changing clock or value keeps its width and its left edge between frames.
 */
private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

@Composable
private fun TimelineLabel(label: String, active: Boolean, x: Dp, y: Dp, align: TextAlign, fg: Color, muted: Color) {
    val type = DndTheme.type
    SingleLineText(
        label,
        if (active) type.labelStrong else type.label,
        Modifier.offset(x = x, y = y).width(EventLabelWidth),
        color = if (active) fg else muted,
        textAlign = align,
    )
}
