package dnd.app.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Read the theme: DndTheme.palette, DndTheme.type, DndTheme.driverSide. */
object DndTheme {
    val palette: DndPalette
        @Composable @ReadOnlyComposable get() = LocalDndPalette.current
    val type: DndTypeScale
        @Composable @ReadOnlyComposable get() = LocalDndTypeScale.current
    val driverSide: DriverSide
        @Composable @ReadOnlyComposable get() = LocalDriverSide.current
}

@Composable
fun DndTheme(
    palette: DndPalette = PleosLightPalette,
    type: DndTypeScale = DefaultTypeScale,
    driverSide: DriverSide = DefaultDriverSide,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalDndPalette provides palette,
        LocalDndTypeScale provides type,
        LocalDriverSide provides driverSide,
        LocalContentColor provides palette.text,
        LocalTextStyle provides type.body,
        content = content,
    )
}

/**
 * Lays [content] out on the 1280 x 720 dp design canvas and scales it evenly to the window: down
 * on a smaller window (1024 x 600 dp), up on the Connect display, which reports 160 DPI and so
 * 2560 x 1440 dp. The proportions always hold and nothing clips.
 */
@Composable
fun DndCanvas(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier) {
        val scale = minOf(maxWidth / DndSpace.canvasWidth, maxHeight / DndSpace.canvasHeight)
        if (scale == 1f) {
            content()
        } else {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density * scale, density.fontScale),
                content = content,
            )
        }
    }
}
