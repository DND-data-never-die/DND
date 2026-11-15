package dnd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dnd.app.ui.model.SelfCheckState
import dnd.app.ui.theme.DndShapes
import dnd.app.ui.theme.DndSpace
import dnd.app.ui.theme.DndTheme
import dnd.app.ui.theme.DriverSide

private val SelfCheckGap = 16.dp

/**
 * Drowsiness self-check: the question first, then round buttons 1 to n, all from the driver's
 * edge, so the question is read before the scale (red team M3). The numbers always run 1 to n
 * left to right. The question says what the scale is, so it has no caption (owner, 2026-11-15).
 */
@Composable
fun SelfCheckStrip(state: SelfCheckState, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, width: Dp? = null) {
    val type = DndTheme.type
    val button = DndSpace.touchTarget
    // As wide as the card above it when given (r4 D6); otherwise the buttons with their gaps.
    val rowWidth = width ?: (button * state.levels + SelfCheckGap * (state.levels - 1))
    val side = if (DndTheme.driverSide == DriverSide.LEFT) Alignment.Start else Alignment.End
    Column(modifier.fillMaxWidth(), horizontalAlignment = side) {
        Row(Modifier.height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            SingleLineText(state.question, type.keyText)
        }
        Spacer(Modifier.height(DndSpace.xs))
        SelfCheckButtons(state, onSelect, Modifier.width(rowWidth))
    }
}

/** The buttons with the scale's two ends under the first and the last. */
@Composable
private fun SelfCheckButtons(state: SelfCheckState, onSelect: (Int) -> Unit, modifier: Modifier) {
    val palette = DndTheme.palette
    val type = DndTheme.type
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (n in 1..state.levels) {
                SelfCheckButton(n.toString(), selected = n == state.selected, onClick = { onSelect(n) }, modifier = Modifier.testTag("selfcheck_$n"))
            }
        }
        Spacer(Modifier.height(DndSpace.xs))
        Row(Modifier.fillMaxWidth()) {
            // Captions start at the first button and end at the last one, inside the strip.
            SingleLineText(state.lowCaption, type.label, color = palette.textSecondary)
            Spacer(Modifier.weight(1f))
            SingleLineText(state.highCaption, type.label, color = palette.textSecondary)
        }
    }
}

/** Round 76 dp button; filled dark when selected. */
@Composable
fun SelfCheckButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = DndTheme.palette
    val base = modifier
        .size(DndSpace.touchTarget)
        .clickable(onClick = onClick)
    Box(
        modifier = if (selected) {
            base.background(palette.emphasis, CircleShape)
        } else {
            base.background(palette.surface, CircleShape).border(DndSpace.outlineWidth, palette.outlineStrong, CircleShape)
        },
        contentAlignment = Alignment.Center,
    ) {
        SingleLineText(label, DndTheme.type.keyText, color = if (selected) palette.onEmphasis else palette.text)
    }
}

/**
 * Two-state switch. Off: white track with a grey outline, white thumb with a dark outline, on the
 * left. On: active-green track (switch_on in the Pleos guide), white thumb on the right.
 */
@Composable
fun DndSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val palette = DndTheme.palette
    // The touch target is the whole row of [SwitchBox]; the track only shows the state.
    val track = modifier.size(width = 88.dp, height = 48.dp)
    Box(
        modifier = (
            if (checked) {
                track.background(palette.good, DndShapes.pill)
            } else {
                track.background(palette.surface, DndShapes.pill).border(DndSpace.outlineWidth, palette.textSecondary, DndShapes.pill)
            }
            ).padding(5.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        val thumb = Modifier.size(38.dp).background(palette.surface, CircleShape)
        Box(if (checked) thumb else thumb.border(DndSpace.outlineWidth, palette.emphasis, CircleShape))
    }
}

/**
 * Secondary box: the switch on the driver's side, its label and a grey value. The whole box is the
 * touch target (at least 76 dp high; red team I2), and its height does not change with the state.
 */
@Composable
fun SwitchBox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, detail: String? = null) {
    SecondaryBox(modifier.clip(DndShapes.box).clickable { onCheckedChange(!checked) }) {
        DriverSideRow(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            gap = DndSpace.l,
            touch = { DndSwitch(checked, onCheckedChange) },
            read = {
                Column(Modifier.weight(1f)) {
                    SingleLineText(label, DndTheme.type.bodyStrong)
                    detail?.let { PairText(it, DndTheme.type.label, color = DndTheme.palette.textSecondary) }
                }
            },
        )
    }
}
