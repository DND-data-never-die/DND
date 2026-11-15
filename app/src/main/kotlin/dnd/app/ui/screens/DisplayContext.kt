package dnd.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dnd.app.ui.components.SingleLineText
import dnd.app.ui.theme.DndSpace
import dnd.app.ui.theme.DndTheme

/**
 * Where the app window sits on the Connect-S display (2560 x 1440 dp, research note pleos_ui_design
 * section 4): status bar 96 high, driving view 840 x 1280 at the driver's side, GNB 160 high, and
 * the 1700 x 1184 landscape app window with radius 40. The window padding 20 is read as the space
 * left between the driving view and the right edge (1720 - 1700), split evenly. The system parts
 * are plain neutral placeholders with a label; they do not imitate Hyundai's system UI.
 */
@Composable
fun DisplayContext(app: @Composable () -> Unit) {
    val palette = DndTheme.palette
    Box(Modifier.size(2560.dp, 1440.dp).background(palette.track)) {
        Placeholder("주행 정보 화면", Modifier.offset(0.dp, 0.dp).size(840.dp, 1280.dp), Color(0xFFD5D7DC), palette.textSecondary)
        Placeholder("상태 표시줄", Modifier.offset(840.dp, 0.dp).size(1720.dp, 96.dp), palette.surfaceSecondary, palette.textSecondary)
        Placeholder("GNB", Modifier.offset(0.dp, 1280.dp).size(2560.dp, 160.dp), palette.emphasis, palette.onEmphasis)
        Box(
            Modifier
                .offset(850.dp, 96.dp)
                .size(DndSpace.canvasWidth, DndSpace.canvasHeight)
                .clip(RoundedCornerShape(40.dp)),
        ) {
            app()
        }
    }
}

@Composable
private fun Placeholder(label: String, modifier: Modifier, fill: Color, text: Color) {
    // 22 dp above and below: the 52 dp label line centred in the 96 dp status bar.
    Box(modifier.background(fill).padding(horizontal = DndSpace.xl, vertical = 22.dp), contentAlignment = Alignment.TopStart) {
        SingleLineText(label, DndTheme.type.labelStrong, color = text)
    }
}
