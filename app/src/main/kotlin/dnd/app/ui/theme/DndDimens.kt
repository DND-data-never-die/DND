package dnd.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Spacing from the gaps the Pleos guide uses between components (4, 8, 12, 16, 24, 32, 40, 48);
 * the guide gives no page margin or grid inside an app window, so the margins are picked from that
 * set. Fixed sizes name their source.
 */
object DndSpace {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 40.dp
    val xxxl = 48.dp

    /**
     * Design canvas: the Pleos Connect landscape app window, 1700 x 1184 dp (Connect-S 17", 160 DPI,
     * 1 dp = 1 px; research note pleos_ui_design section 4).
     */
    val canvasWidth = 1700.dp
    val canvasHeight = 1184.dp

    val screenPadding = 32.dp
    /** The app's title row: wordmark, driver chip, back button (76 dp touch target). */
    val titleBarHeight = 96.dp
    val cardGap = 32.dp
    val cardPaddingHorizontal = 32.dp
    /** 32 on all four sides: a value of the guide's gap set (r4 D1). */
    val cardPaddingVertical = 32.dp
    /** Touch target height of the screen spec. */
    val touchTarget = 76.dp
    /** The guide's basic button, Large: 100 high, radius 32, horizontal padding 40 (research note section 5). */
    val buttonHeight = 100.dp
    val buttonPadding = 40.dp
    /** The guide's basic button, Medium: 80 high, radius 24, horizontal padding 32 (the back button). */
    val buttonMediumHeight = 80.dp
    val buttonMediumPadding = 32.dp
    val outlineWidth = 2.dp
}

/**
 * Corner radii, from the outside in so nothing is rounder than its container (r4 D1): cards 40 (the
 * guide's app window radius), the notification 48 (its system notification), basic buttons 32 / 24,
 * inner boxes 12 (the guide's radius scale).
 */
object DndShapes {
    val card = RoundedCornerShape(40.dp)
    val notification = RoundedCornerShape(48.dp)
    val box = RoundedCornerShape(12.dp)
    val pill = RoundedCornerShape(percent = 50)
    /** Basic button Large. */
    val button = RoundedCornerShape(32.dp)
    /** Basic button Medium. */
    val buttonMedium = RoundedCornerShape(24.dp)
}
