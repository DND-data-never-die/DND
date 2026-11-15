package dnd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dnd.app.ui.icons.DndIcon
import dnd.app.ui.icons.Pictogram
import dnd.app.ui.icons.PictogramSize
import dnd.app.ui.model.TopBarState
import dnd.app.ui.theme.DndCanvas
import dnd.app.ui.theme.DndSpace
import dnd.app.ui.theme.DndTheme

/**
 * Every screen inside the 1700 x 1184 app window: the 32 dp side margins, the title row (or
 * nothing, while driving), the body, and [bottomGap] at the bottom.
 */
@Composable
fun ScreenScaffold(
    topBar: TopBarState?,
    modifier: Modifier = Modifier,
    showDemoControls: Boolean = false,
    onDemoClick: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    bottomGap: Dp = DndSpace.xl,
    title: String? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    DndCanvas(modifier.fillMaxSize().background(DndTheme.palette.background)) {
        Column(Modifier.fillMaxSize().padding(horizontal = DndSpace.screenPadding)) {
            if (topBar != null) {
                TopBar(topBar, showDemoControls = showDemoControls, onDemoClick = onDemoClick, onBack = onBack, title = title)
            } else {
                Spacer(Modifier.height(DndSpace.titleBarHeight))
            }
            Spacer(Modifier.height(DndSpace.m))
            body()
            Spacer(Modifier.height(bottomGap))
        }
    }
}

/** Grey caption above a key value or a list, with an optional pictogram as tall as the text. */
@Composable
fun Caption(text: String, modifier: Modifier = Modifier, icon: DndIcon? = null) {
    val palette = DndTheme.palette
    Row(modifier.height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Pictogram(icon, size = PictogramSize.Small, tint = palette.textSecondary)
            Spacer(Modifier.width(DndSpace.s))
        }
        SingleLineText(text, DndTheme.type.labelStrong, color = palette.textSecondary)
    }
}
