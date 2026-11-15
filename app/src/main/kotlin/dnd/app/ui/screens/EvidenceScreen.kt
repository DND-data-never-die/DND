package dnd.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dnd.app.ui.components.DndCard
import dnd.app.ui.components.GradeBar
import dnd.app.ui.components.Hairline
import dnd.app.ui.components.ScreenScaffold
import dnd.app.ui.components.SingleLineText
import dnd.app.ui.components.icon
import dnd.app.ui.icons.Pictogram
import dnd.app.ui.icons.PictogramSize
import dnd.app.ui.model.EvidenceLine
import dnd.app.ui.model.EvidenceUiState
import dnd.app.ui.theme.DndMotion
import dnd.app.ui.theme.DndShapes
import dnd.app.ui.theme.DndSpace
import dnd.app.ui.theme.DndTheme
import dnd.app.ui.theme.motion

/** Icon box 40 + 12, as in every pictogram-labelled line (r4 D4). */
private val IconColumn = 52.dp
private val NameColumn = 300.dp
private val ValueColumn = 360.dp
private val LoadColumn = 160.dp
private val BarGap = 48.dp

/** The build-up of the evidence video: one stage every 500 ms, each fading in over 300 ms. */
const val EVIDENCE_BUILD_STEP_MS = 500
const val EVIDENCE_BUILD_FADE_MS = 300

/** Stages of the build-up: one per item row, then the total with the grade bar. */
fun EvidenceUiState.buildStages(): Int = rows.size + 1

/**
 * Screen B, behind the driver card's "상세 정보": which items made the index. One row per item that occurred (name, this
 * trip's value in the driver's units, the 부담 number and its bar), the total "부담 합계", and the
 * grade bar from 0 to 100 with its bounds. No formula, no weights, no sources, no supplementary
 * lines (owner, 2026-11-15). It opens with the detail transition of the navigation; [buildMs]
 * (null = finished) lets the rows appear one after another for the video.
 */
@Composable
fun EvidenceScreen(
    state: EvidenceUiState,
    modifier: Modifier = Modifier,
    showDemoControls: Boolean = false,
    onDemoClick: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    buildMs: Float? = null,
) {
    val type = DndTheme.type
    val palette = DndTheme.palette
    fun stage(i: Int): Float = buildMs?.let {
        DndMotion.linearStage(it, i)
    } ?: 1f
    val maxLoad = state.rows.maxOfOrNull { it.loadPoints }?.takeIf { it > 0f } ?: 1f
    // The title sits in the top bar after the back icon (Pleos page navigation, Depth).
    ScreenScaffold(state.topBar, modifier, showDemoControls, onDemoClick, onBack, title = state.title) {
        DndCard(Modifier.fillMaxWidth().weight(1f)) {
            // Column header over the 부담 numbers.
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(IconColumn + NameColumn + ValueColumn))
                SingleLineText(state.loadHeader, type.labelStrong, Modifier.width(LoadColumn), color = palette.textSecondary, textAlign = TextAlign.End)
            }
            // The item rows fill the card, so the total block ends at its bottom (r4 D11).
            Column(Modifier.weight(1f)) {
                state.rows.forEachIndexed { i, row ->
                    Column(Modifier.weight(1f).motion(stage(i))) {
                        Hairline()
                        FactorRow(row, maxLoad, Modifier.weight(1f))
                    }
                }
            }
            val totalStage = stage(state.rows.size)
            // The fade comes first in the chain, so it covers the rule's own fill (a fill before it would
            // show the rule before the total appears).
            Box(Modifier.motion(totalStage).fillMaxWidth().height(2.dp).background(palette.text))
            Column(Modifier.motion(totalStage)) {
                // The unrounded index, the sum of the items' 부담, in the 부담 column.
                Row(Modifier.fillMaxWidth().height(80.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(IconColumn))
                    SingleLineText(state.loadSumLabel, type.bodyStrong, Modifier.width(NameColumn + ValueColumn))
                    SingleLineText(state.loadSum, type.keyText, Modifier.width(LoadColumn), textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(DndSpace.m))
                // The index under the card's own name, beside the grade bar.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(IconColumn))
                    Column(Modifier.width(NameColumn)) {
                        SingleLineText(state.totalLabel, type.labelStrong, color = palette.textSecondary)
                        SingleLineText(state.total, type.keyValue)
                    }
                    GradeBar(state.grade, Modifier.weight(1f))
                }
            }
        }
    }
}

/** One item: pictogram, name, value, the 부담 number and its bar. */
@Composable
private fun FactorRow(row: EvidenceLine, maxLoad: Float, modifier: Modifier) {
    val type = DndTheme.type
    val palette = DndTheme.palette
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(IconColumn)) { Pictogram(row.kind.icon(), size = PictogramSize.Small) }
        SingleLineText(row.label, type.bodyStrong, Modifier.width(NameColumn))
        val value = row.value
        if (value != null) SingleLineText(value, type.bodyStrong, Modifier.width(ValueColumn)) else Spacer(Modifier.width(ValueColumn))
        SingleLineText(row.load, type.keyText, Modifier.width(LoadColumn), textAlign = TextAlign.End)
        Spacer(Modifier.width(BarGap))
        // The bar is the 부담 on one scale for all rows (the largest 부담 is full length).
        BoxWithConstraints(Modifier.weight(1f)) {
            Box(Modifier.width(maxWidth * (row.loadPoints / maxLoad).coerceIn(0f, 1f)).height(24.dp).background(palette.info, DndShapes.pill))
        }
    }
}

/** Opacity of build stage [i] at [timeMs]: linear over [EVIDENCE_BUILD_FADE_MS] from i x [EVIDENCE_BUILD_STEP_MS]. */
private fun DndMotion.linearStage(timeMs: Float, i: Int): Float =
    linear(timeMs, i * EVIDENCE_BUILD_STEP_MS, i * EVIDENCE_BUILD_STEP_MS + EVIDENCE_BUILD_FADE_MS)
