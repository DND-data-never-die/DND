package dnd.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dnd.app.ui.components.CardHeader
import dnd.app.ui.components.DndCard
import dnd.app.ui.components.GuidanceBox
import dnd.app.ui.components.NumberWithUnit
import dnd.app.ui.components.Pill
import dnd.app.ui.components.ScreenScaffold
import dnd.app.ui.components.SingleLineText
import dnd.app.ui.icons.DndIcon
import dnd.app.ui.icons.DndIcons
import dnd.app.ui.model.ColumnBars
import dnd.app.ui.model.DayColumns
import dnd.app.ui.model.HorizontalBars
import dnd.app.ui.model.WeeklyTile
import dnd.app.ui.model.WeeklyUiState
import dnd.app.ui.theme.DndShapes
import dnd.app.ui.theme.DndSpace
import dnd.app.ui.theme.DndTheme

/** Columns in the weekly charts share one width and corner. */
private val BarWidth = 44.dp
private val BarShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
private val LabelRow = 52.dp

/**
 * Screen D: the week in three glance tiles (title, hero, status label, one chart that draws the
 * reference its label names) and one guidance line. Red only inside the status labels; flagged
 * bars are dark, the others mid grey.
 */
@Composable
fun WeeklyScreen(
    state: WeeklyUiState,
    modifier: Modifier = Modifier,
    showDemoControls: Boolean = false,
    onDemoClick: () -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    ScreenScaffold(state.topBar, modifier, showDemoControls, onDemoClick, onBack, title = state.title) {
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(DndSpace.cardGap)) {
            Tile(DndIcons.Spread, state.departure, Modifier.weight(1f).fillMaxHeight())
            Tile(DndIcons.Bed, state.restGap, Modifier.weight(1f).fillMaxHeight())
            Tile(DndIcons.Congestion, state.congestion, Modifier.weight(1f).fillMaxHeight())
        }
        state.advice?.let {
            Spacer(Modifier.height(DndSpace.l))
            GuidanceBox(null, it, Modifier.testTag("weekly_tip"))
        }
    }
}

@Composable
private fun Tile(icon: DndIcon, tile: WeeklyTile, modifier: Modifier) {
    val type = DndTheme.type
    DndCard(modifier) {
        CardHeader(icon, tile.title)
        Spacer(Modifier.height(DndSpace.s))
        Row(Modifier.height(112.dp), verticalAlignment = Alignment.CenterVertically) {
            // "1시간 10분" with smaller units; a hero without digits ("월·화") in the full size.
            if (tile.value.any { it.isDigit() }) {
                NumberWithUnit(tile.value, type.keyValue, SpanStyle(fontSize = type.keyText.fontSize, fontWeight = type.keyText.fontWeight))
            } else {
                SingleLineText(tile.value, type.keyValue)
            }
        }
        Box(Modifier.height(88.dp), contentAlignment = Alignment.CenterStart) {
            tile.pill?.let { Pill(it, tone = tile.pillTone) }
        }
        // The three charts start and end on one line (r4 D3).
        Spacer(Modifier.height(DndSpace.l))
        when (val chart = tile.chart) {
            is HorizontalBars -> {
                Spacer(Modifier.weight(1f))
                HorizontalBarsChart(chart)
                Spacer(Modifier.weight(1f))
            }
            is ColumnBars -> ColumnBarsChart(chart, Modifier.fillMaxWidth().weight(1f))
            is DayColumns -> DayColumnsChart(chart, Modifier.fillMaxWidth().weight(1f))
            null -> Spacer(Modifier.weight(1f))
        }
    }
}

/** Two labelled horizontal bars, this week against the usual value. */
@Composable
private fun HorizontalBarsChart(chart: HorizontalBars) {
    val type = DndTheme.type
    val palette = DndTheme.palette
    Column(verticalArrangement = Arrangement.spacedBy(DndSpace.m)) {
        chart.bars.forEach { bar ->
            Row(Modifier.fillMaxWidth().height(LabelRow), verticalAlignment = Alignment.CenterVertically) {
                SingleLineText(bar.label, type.label, Modifier.width(132.dp), color = palette.textSecondary)
                BoxWithConstraints(Modifier.weight(1f)) {
                    val room = maxWidth - 100.dp
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .width((room * bar.fraction).coerceAtLeast(20.dp))
                                .height(24.dp)
                                .background(if (bar.emphasized) palette.text else palette.chartMuted, DndShapes.pill),
                        )
                        bar.valueText?.let {
                            Spacer(Modifier.width(DndSpace.s))
                            SingleLineText(it, type.label, color = palette.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

/** Columns with the day under each and a dashed reference line with its label. */
@Composable
private fun ColumnBarsChart(chart: ColumnBars, modifier: Modifier) {
    val type = DndTheme.type
    val palette = DndTheme.palette
    BoxWithConstraints(modifier) {
        val area = maxHeight - LabelRow
        // The scale gives each labelled column a label row above it; a column without a label may
        // reach the top of the chart.
        val valueArea = chart.bars
            .filter { it.valueText != null && it.fraction > 0f }
            .minOfOrNull { (area - LabelRow) / it.fraction }
            ?.coerceAtMost(area) ?: area
        // Equal slots, one per column (r4 D3).
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            chart.bars.forEach { bar ->
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(1f))
                    bar.valueText?.let {
                        SingleLineText(it, if (bar.emphasized) type.labelStrong else type.label, color = if (bar.emphasized) palette.text else palette.textSecondary)
                    }
                    Box(
                        Modifier
                            .width(BarWidth)
                            .height(valueArea * bar.fraction)
                            .background(if (bar.emphasized) palette.text else palette.chartMuted, BarShape),
                    )
                    Box(Modifier.height(LabelRow), contentAlignment = Alignment.BottomCenter) {
                        SingleLineText(bar.label, if (bar.emphasized) type.labelStrong else type.label, color = if (bar.emphasized) palette.text else palette.textSecondary)
                    }
                }
            }
        }
        chart.reference?.let { ref -> ReferenceLine(area - valueArea * ref, chart.referenceLabel) }
    }
}

/**
 * Minutes of congestion per weekday with the week's mean as a dashed line; days above it are dark
 * with their minutes above them.
 */
@Composable
private fun DayColumnsChart(chart: DayColumns, modifier: Modifier) {
    val type = DndTheme.type
    val palette = DndTheme.palette
    val max = chart.bars.maxOfOrNull { it.minutes }?.takeIf { it > 0f } ?: 1f
    BoxWithConstraints(modifier) {
        // The days under the columns; a label row above the tallest column for its minutes.
        val barArea = maxHeight - LabelRow * 2
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            chart.bars.forEach { bar ->
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(1f))
                    bar.minutesText?.let { SingleLineText(it, type.labelStrong) }
                    Box(
                        Modifier
                            .width(BarWidth)
                            .height(barArea * (bar.minutes / max))
                            .background(if (bar.flagged) palette.text else palette.chartMuted, BarShape),
                    )
                    Box(Modifier.height(LabelRow), contentAlignment = Alignment.BottomCenter) {
                        SingleLineText(bar.label, if (bar.flagged) type.labelStrong else type.label, color = if (bar.flagged) palette.text else palette.textSecondary)
                    }
                }
            }
        }
        // The mean, on the same scale as the columns.
        ReferenceLine(maxHeight - LabelRow - barArea * (chart.mean / max), chart.meanLabel)
    }
}

/** A dashed line across the chart at [y] with its label at the right end, above the line. */
@Composable
private fun ReferenceLine(y: Dp, label: String?) {
    val type = DndTheme.type
    val palette = DndTheme.palette
    // The text colour over a 7-dp dashed halo in the surface colour, so the line stays visible over
    // white, grey and dark bars alike (r4 D3).
    val line = palette.text
    val halo = palette.surface
    Canvas(Modifier.fillMaxWidth().height(7.dp).offset(y = y - 2.dp)) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
        val mid = Offset(0f, size.height / 2)
        val end = Offset(size.width, size.height / 2)
        drawLine(halo, mid, end, strokeWidth = size.height, pathEffect = dash)
        drawLine(line, mid, end, strokeWidth = 3.dp.toPx(), pathEffect = dash)
    }
    label?.let {
        Box(Modifier.fillMaxWidth().offset(y = y - 54.dp), contentAlignment = Alignment.TopEnd) {
            SingleLineText(it, type.labelStrong, color = palette.textSecondary)
        }
    }
}
