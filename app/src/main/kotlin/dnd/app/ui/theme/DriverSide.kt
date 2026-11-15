package dnd.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The edge of the centre display nearest the driver. In a left-hand-drive car it is the left edge:
 * what the driver touches sits there, what he only reads sits on the other side. A right-hand-drive
 * market sets [RIGHT] and the screens mirror their touch controls; text, numbers, charts and time
 * axes keep reading left to right.
 */
enum class DriverSide { LEFT, RIGHT }

/** The one place the side is set: left-hand drive (Korea). */
val DefaultDriverSide = DriverSide.LEFT

val LocalDriverSide = staticCompositionLocalOf { DefaultDriverSide }
