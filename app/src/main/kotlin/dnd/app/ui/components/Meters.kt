package dnd.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import dnd.app.ui.icons.DndIcon
import dnd.app.ui.icons.Pictogram
import dnd.app.ui.icons.PictogramSize
import dnd.app.ui.model.GradeScale
import dnd.app.ui.model.ProgressState
import dnd.app.ui.theme.DndShapes
import dnd.app.ui.theme.DndSpace
import dnd.app.ui.theme.DndTheme
import dnd.app.ui.theme.gradeColor

private val GradeGap = 4.dp
private val GradeBarHeight = 16.dp
private val GradeKnob = 40.dp
private val BoundLabelWidth = 80.dp

/**
 * Three-segment grade bar from 0 to [GradeScale.max] (low, middle, high) with a knob at the value.
 * Segment widths follow the engine's bounds; the segment holding the value is dark, the others
 * track grey; only the knob's ring carries the grade's colour. With bound labels the ends 0 and
 * 100 are written too.
 */
@Composable
fun GradeBar(scale: GradeScale, modifier: Modifier = Modifier) {
    val palette = DndTheme.palette
    val type = DndTheme.type
    val bounds = listOf(0f, scale.lowUpper, scale.midUpper, scale.max)
    val widths = (0..2).map { (bounds[it + 1] - bounds[it]).coerceAtLeast(0.001f) }
    val active = scale.activeSegment.coerceIn(0, 2)
    Column(modifier) {
        scale.boundLabels?.let { (low, high) ->
            BoxWithConstraints(Modifier.fillMaxWidth().height(40.dp)) {
                val usable = maxWidth - GradeGap * 2
                SingleLineText(scale.startLabel, type.labelStrong, color = palette.textSecondary)
                // Bound values centred over the two gaps between the segments.
                listOf(
                    usable * (scale.lowUpper / scale.max) + GradeGap / 2 to low,
                    usable * (scale.midUpper / scale.max) + GradeGap * 1.5f to high,
                ).forEach { (x, text) ->
                    Box(Modifier.offset(x = x - BoundLabelWidth / 2).width(BoundLabelWidth), contentAlignment = Alignment.Center) {
                        SingleLineText(text, type.labelStrong, color = palette.textSecondary)
                    }
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                    SingleLineText(scale.endLabel, type.labelStrong, color = palette.textSecondary)
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(GradeKnob)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(GradeBarHeight).align(Alignment.CenterStart),
                horizontalArrangement = Arrangement.spacedBy(GradeGap),
            ) {
                for (i in 0..2) {
                    val shape = when (i) {
                        0 -> RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                        2 -> RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
                        else -> RectangleShape
                    }
                    // Inactive segments keep a boundary at 3:1 (WCAG 2.1 SC 1.4.11).
                    Box(
                        Modifier
                            .weight(widths[i])
                            .fillMaxHeight()
                            .background(if (i == active) palette.text else palette.track, shape)
                            .then(if (i == active) Modifier else Modifier.border(DndSpace.outlineWidth, palette.outlineStrong, shape)),
                    )
                }
            }
            val usable = maxWidth - GradeGap * 2
            val x = usable * (scale.value.coerceIn(0f, scale.max) / scale.max) + GradeGap * active
            Box(
                Modifier
                    .offset(x = x - GradeKnob / 2)
                    .size(GradeKnob)
                    .background(palette.surface, CircleShape)
                    .border(7.dp, palette.gradeColor(active), CircleShape),
            )
        }
        Spacer(Modifier.height(DndSpace.xxs))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GradeGap)) {
            for (i in 0..2) {
                Box(Modifier.weight(widths[i]), contentAlignment = Alignment.Center) {
                    SingleLineText(
                        scale.labels[i],
                        if (i == active) type.labelStrong else type.label,
                        color = if (i == active) palette.text else palette.textSecondary,
                    )
                }
            }
        }
    }
}

/**
 * A labelled progress bar: pictogram, "주행" and "13,150 / 15,000km" above, the bar below. Past the
 * interval the bar runs to the current value; the interval is marked with a tick and the part past
 * it is hatched, so the overdue state survives grey print (red team R2).
 */
@Composable
fun ProgressRow(icon: DndIcon, state: ProgressState, modifier: Modifier = Modifier) {
    val palette = DndTheme.palette
    val type = DndTheme.type
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pictogram(icon, size = PictogramSize.Small)
            Spacer(Modifier.width(DndSpace.s))
            SingleLineText(state.lead, type.labelStrong, color = palette.textSecondary)
            Spacer(Modifier.width(DndSpace.s))
            // "13,150" and "/ 15,000km": two elements within 10 characters each.
            val current = state.valueLabel.substringBefore(" / ")
            SingleLineText(current, type.bodyStrong)
            if (current != state.valueLabel) {
                Spacer(Modifier.width(DndSpace.xs))
                SingleLineText("/ " + state.valueLabel.substringAfter(" / "), type.body, color = palette.textSecondary)
            }
        }
        Spacer(Modifier.height(DndSpace.xs))
        val scaleMax = maxOf(state.current, state.total).takeIf { it > 0f } ?: 1f
        val fill = (state.current / scaleMax).coerceIn(0f, 1f)
        val limit = (state.total / scaleMax).coerceIn(0f, 1f)
        BoxWithConstraints(Modifier.fillMaxWidth().height(24.dp).clip(DndShapes.pill).background(palette.track)) {
            if (!state.overdue) {
                Box(Modifier.fillMaxWidth(fill).fillMaxHeight().background(palette.info, DndShapes.pill))
            } else {
                Box(Modifier.fillMaxWidth(limit).fillMaxHeight().background(palette.text))
                // Past the interval: dark diagonal hatch on the track.
                val hatchColor = palette.text
                Canvas(Modifier.offset(x = maxWidth * limit).width(maxWidth * (fill - limit)).fillMaxSize()) {
                    val step = 10.dp.toPx()
                    val w = 3.dp.toPx()
                    var x = -size.height
                    while (x < size.width) {
                        drawLine(hatchColor, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = w)
                        x += step
                    }
                }
                // The interval itself: a white tick between the two parts.
                Box(Modifier.offset(x = maxWidth * limit - 2.dp).width(4.dp).fillMaxHeight().background(palette.surface))
            }
        }
    }
}
