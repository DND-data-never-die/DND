package dnd.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dnd.app.ui.icons.DndIcons
import dnd.app.ui.icons.Pictogram
import dnd.app.ui.icons.PictogramSize
import dnd.app.ui.model.TopBarState
import dnd.app.ui.theme.DndShapes
import dnd.app.ui.theme.DndSpace
import dnd.app.ui.theme.DndTheme
import dnd.app.ui.theme.DriverSide

/**
 * The app's title row: wordmark and driver chip. The Pleos status bar above the window already
 * shows the clock and the driving status, so the app draws neither. On inner screens the Pleos page
 * navigation (Depth): the back control is the "<" icon alone, with the screen's [title] right after
 * it, on the driver's side: after the driver chip (left-hand drive, so the wordmark and the chip
 * stay where they are) or at the right end (right-hand drive). The demo button appears only when
 * [showDemoControls] is true (never in captures).
 */
@Composable
fun TopBar(
    state: TopBarState,
    modifier: Modifier = Modifier,
    showDemoControls: Boolean = false,
    onDemoClick: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    /** The screen's title, after the back icon. */
    title: String? = null,
) {
    val left = DndTheme.driverSide == DriverSide.LEFT
    Row(
        modifier = modifier.fillMaxWidth().height(DndSpace.titleBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SingleLineText(state.wordmark, DndTheme.type.wordmark)
        Spacer(Modifier.width(DndSpace.l))
        OutlinePill(state.driverName, icon = DndIcons.Driver)
        if (left) {
            Spacer(Modifier.width(DndSpace.xl))
            PageNavigation(state.backLabel, onBack, title)
        }
        Spacer(Modifier.weight(1f))
        if (showDemoControls) {
            OutlinePill(state.demoLabel, modifier = Modifier.clickable(onClick = onDemoClick))
        }
        if (!left) {
            Spacer(Modifier.width(DndSpace.m))
            PageNavigation(state.backLabel, onBack, title)
        }
    }
}

/** The back icon (when [onBack] is given) and the screen title after it. */
@Composable
private fun PageNavigation(backLabel: String, onBack: (() -> Unit)?, title: String?) {
    if (onBack != null) BackButton(backLabel, onBack, Modifier.testTag("back_button"))
    title?.let {
        Spacer(Modifier.width(DndSpace.xs))
        SingleLineText(it, DndTheme.type.cardTitleStrong)
    }
}

/**
 * The back control: the "<" icon alone (Pleos page navigation, Depth), in a touch target the size of
 * the guide's basic button Medium (80 x 80; at least the 76 of the screen spec). The chevron is
 * drawn at 56, taller than the 40 sp title beside it. [label] is read out by TalkBack, not drawn.
 */
@Composable
fun BackButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(DndSpace.buttonMediumHeight)
            .clip(DndShapes.buttonMedium)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Pictogram(DndIcons.Chevron, Modifier.rotate(180f), size = PictogramSize.Medium)
    }
}
