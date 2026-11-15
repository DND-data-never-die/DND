package dnd.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dnd.app.ui.theme.DndTheme
import dnd.app.ui.theme.DriverSide

/**
 * A row with its [touch] part on the driver's side and its [read] part on the other side. Each
 * part keeps its own order, so text and charts inside it still read left to right.
 */
@Composable
fun DriverSideRow(
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    gap: Dp = 0.dp,
    touch: @Composable RowScope.() -> Unit,
    read: @Composable RowScope.() -> Unit,
) {
    Row(modifier, verticalAlignment = verticalAlignment) {
        if (DndTheme.driverSide == DriverSide.LEFT) {
            touch()
            Spacer(Modifier.width(gap))
            read()
        } else {
            read()
            Spacer(Modifier.width(gap))
            touch()
        }
    }
}

/** Box alignment at the bottom of the driver's side. */
val driverBottomAlignment: Alignment
    @Composable get() = if (DndTheme.driverSide == DriverSide.LEFT) Alignment.BottomStart else Alignment.BottomEnd
