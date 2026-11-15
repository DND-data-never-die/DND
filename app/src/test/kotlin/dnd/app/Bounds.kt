package dnd.app

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onAllNodesWithTag
import java.io.File
import kotlin.math.roundToInt

/**
 * Pixel boxes of the elements the demo video crops and rings use, from their test tags, written to
 * build/outputs/roborazzi/bounds.tsv (screen, tag, left, top, right, bottom in capture pixels).
 */
object Bounds {
    private val file = File("build/outputs/roborazzi/bounds.tsv")
    private val tags = listOf(
        "driver_card", "vehicle_card", "driver_button", "vehicle_button", "selfcheck_6", "guidance_box", "comparison_box",
        "vd_left_column", "vd_comparison_rows", "vd_compare", "severe_toggle", "back_button", "weekly_tip", "notice_button", "demo_panel", "app_area",
    )

    @Synchronized
    fun record(screen: String, compose: SemanticsNodeInteractionsProvider) {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeText("screen\ttag\tleft\ttop\tright\tbottom\n")
        }
        tags.forEach { tag ->
            compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).forEach { node ->
                val r = node.boundsInRoot
                file.appendText(listOf(screen, tag, r.left.roundToInt(), r.top.roundToInt(), r.right.roundToInt(), r.bottom.roundToInt()).joinToString("\t") + "\n")
            }
        }
    }
}
