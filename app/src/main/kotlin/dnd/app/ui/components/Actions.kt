package dnd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dnd.app.ui.icons.DndIcon
import dnd.app.ui.icons.DndIcons
import dnd.app.ui.icons.Pictogram
import dnd.app.ui.icons.PictogramSize
import dnd.app.ui.model.NoticeState
import dnd.app.ui.model.ProcessingStep
import dnd.app.ui.theme.DndShapes
import dnd.app.ui.theme.DndSpace
import dnd.app.ui.theme.DndTheme

/**
 * The guide's basic button, Large: light grey fill (button_basic_enabled), 100 dp high, radius 32,
 * horizontal padding 40, with a boundary at 3:1 (WCAG 2.1 SC 1.4.11) and an optional chevron as
 * tall as the label.
 */
@Composable
fun OutlineButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, chevron: Boolean = false) {
    val palette = DndTheme.palette
    Row(
        modifier = modifier
            .heightIn(min = DndSpace.buttonHeight)
            .background(palette.surface, DndShapes.button)
            .background(palette.buttonFill, DndShapes.button)
            .border(DndSpace.outlineWidth, palette.outlineStrong, DndShapes.button)
            .clickable(onClick = onClick)
            .padding(start = DndSpace.buttonPadding, end = if (chevron) DndSpace.l else DndSpace.buttonPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        SingleLineText(label, DndTheme.type.keyText)
        if (chevron) {
            Spacer(Modifier.width(DndSpace.xs))
            Pictogram(DndIcons.Chevron, size = PictogramSize.Large)
        }
    }
}

/**
 * A card's action as a full button: label at the information size and a chevron, at least 76 dp
 * tall, at the bottom of the card on the driver's side.
 */
@Composable
fun CardButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) = OutlineButton(label, onClick, modifier, chevron = true)

/** Filled neutral button, 76 dp tall. */
@Composable
fun FilledButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = DndTheme.palette
    Box(
        modifier = modifier
            .heightIn(min = DndSpace.buttonHeight)
            .background(palette.emphasis, DndShapes.button)
            .clickable(onClick = onClick)
            .padding(horizontal = DndSpace.buttonPadding),
        contentAlignment = Alignment.Center,
    ) {
        SingleLineText(label, DndTheme.type.keyText, color = palette.onEmphasis)
    }
}

/** Notification card: the button on the driver's side, then a check and one line. */
@Composable
fun NoticeCard(state: NoticeState, onAction: () -> Unit, modifier: Modifier = Modifier, buttonTag: String = "") {
    val palette = DndTheme.palette
    DriverSideRow(
        // Radius 48 = button 32 + padding 16: the button is concentric with the card.
        modifier = modifier
            .background(palette.surface, DndShapes.notification)
            .border(DndSpace.outlineWidth, palette.outline, DndShapes.notification)
            .padding(DndSpace.m),
        verticalAlignment = Alignment.CenterVertically,
        gap = DndSpace.xl,
        touch = { FilledButton(state.actionLabel, onAction, Modifier.testTag(buttonTag)) },
        read = {
            // The check as tall as the 56 sp text beside it, without a badge.
            Pictogram(DndIcons.Check, size = PictogramSize.Hero)
            Spacer(Modifier.width(DndSpace.xs))
            SingleLineText(state.text, DndTheme.type.keyText)
            Spacer(Modifier.width(DndSpace.m))
        },
    )
}

/**
 * Done: a dark check on an active-tint badge (the state pictogram stays #131417 for 4.5:1; the
 * colour sits on the badge; red team X3). The check is as tall as the 40 sp step label.
 */
@Composable
fun DoneMark(modifier: Modifier = Modifier) {
    val palette = DndTheme.palette
    Box(modifier.size(MarkSize).background(palette.goodTint, CircleShape), contentAlignment = Alignment.Center) {
        Pictogram(DndIcons.Check, size = PictogramSize.Large, tint = palette.text)
    }
}

/** An empty circle: a step not done yet. */
@Composable
fun PendingMark(modifier: Modifier = Modifier) {
    Box(modifier.size(MarkSize).border(DndSpace.outlineWidth, DndTheme.palette.outlineStrong, CircleShape))
}

private val MarkSize = 80.dp

/** One processing step: pictogram, label, and a check once it is done. */
@Composable
fun StepRow(icon: DndIcon, step: ProcessingStep, modifier: Modifier = Modifier) {
    val palette = DndTheme.palette
    val tint = if (step.done) palette.text else palette.textSecondary
    Row(modifier.height(88.dp), verticalAlignment = Alignment.CenterVertically) {
        Pictogram(icon, size = PictogramSize.Small, tint = tint)
        Spacer(Modifier.width(DndSpace.l))
        SingleLineText(step.label, DndTheme.type.cardTitleStrong, Modifier.width(280.dp), color = tint)
        if (step.done) DoneMark() else PendingMark()
    }
}

/**
 * Scenario sheet behind the "데모" button: a scrim and a card with one button per scenario.
 * It appears only in the running app.
 */
@Composable
fun BoxScope.DemoSheet(title: String, items: List<String>, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val palette = DndTheme.palette
    Box(
        Modifier
            .matchParentSize()
            .background(palette.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    )
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .background(palette.surface, DndShapes.card)
            .border(DndSpace.outlineWidth, palette.outline, DndShapes.card)
            .padding(DndSpace.xl),
        verticalArrangement = Arrangement.spacedBy(DndSpace.m),
    ) {
        SingleLineText(title, DndTheme.type.cardTitleStrong)
        items.forEachIndexed { i, label -> OutlineButton(label, { onPick(i) }, Modifier.width(400.dp)) }
    }
}
