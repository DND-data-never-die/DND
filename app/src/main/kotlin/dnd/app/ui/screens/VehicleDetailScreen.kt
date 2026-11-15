package dnd.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dnd.app.ui.components.BalancedText
import dnd.app.ui.components.Caption
import dnd.app.ui.components.DndCard
import dnd.app.ui.components.Hairline
import dnd.app.ui.components.ProgressRow
import dnd.app.ui.components.ScreenScaffold
import dnd.app.ui.components.SingleLineText
import dnd.app.ui.components.SwitchBox
import dnd.app.ui.components.icon
import dnd.app.ui.icons.DndIcons
import dnd.app.ui.icons.Pictogram
import dnd.app.ui.icons.PictogramSize
import dnd.app.ui.model.SevereConditionLine
import dnd.app.ui.model.SevereTableForm
import dnd.app.ui.model.VehicleCardState
import dnd.app.ui.model.VehicleDetailUiState
import dnd.app.ui.model.VehicleFact
import dnd.app.ui.theme.DndSpace
import dnd.app.ui.theme.DndTheme

/** Icon box 40 + 12: the text column of every pictogram-labelled line (r4 D4). */
private val IconColumn = 52.dp

/** Between the name and the first number column: 32, a gap of the guide's set (DndSpace). */
private val ColumnGap = DndSpace.xl

/** Between the two number columns: 48, the guide's largest gap, so the two values read as two. */
private val NumberGap = DndSpace.xxxl

/**
 * The vehicle card's 상세 정보, titled "엔진 오일 교체 시기" in the top bar. Left, on the driver's side: the countdown (hero, second line), the two bars since
 * the last change, the date of that change, and the severe-interval switch with its interval, with
 * equal gaps between these four groups. Right: the severe conditions that occurred in this trip
 * under "이번 운행" / "평소", one row each: its name (the manual's phrase, or the short name in
 * [SevereTableForm.ShortLabel]), this trip's value and the usual value. It opens with the detail
 * transition of the navigation, so it has no entry motion of its own.
 */
@Composable
fun VehicleDetailScreen(
    state: VehicleDetailUiState,
    modifier: Modifier = Modifier,
    showDemoControls: Boolean = false,
    onDemoClick: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onSevereIntervalChange: (Boolean) -> Unit = {},
) {
    val v = state.vehicle
    // The title sits in the top bar after the back icon (Pleos page navigation, Depth).
    ScreenScaffold(state.topBar, modifier, showDemoControls, onDemoClick, onBack, title = v.detailTitle) {
        DndCard(Modifier.fillMaxWidth().weight(1f)) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                // The left column needs about 640 dp, the comparison about 824 (values at the key size).
                Column(Modifier.weight(0.46f).fillMaxHeight().testTag("vd_left_column")) {
                    Caption(v.dueCaption, icon = DndIcons.OilCan)
                    DueHero(v, Modifier.height(HeroRowHeight))
                    DueSecondLine(v)
                    Spacer(Modifier.weight(1f))
                    ProgressRow(DndIcons.Route, v.distanceProgress)
                    Spacer(Modifier.height(DndSpace.m))
                    ProgressRow(DndIcons.Calendar, v.timeProgress)
                    Spacer(Modifier.weight(1f))
                    state.facts.forEach { FactLine(it) }
                    Spacer(Modifier.weight(1f))
                    // The severe interval shows in both states, so the block above does not move (red team M7).
                    SwitchBox(
                        v.severeSwitchLabel,
                        v.severeIntervalOn,
                        onSevereIntervalChange,
                        Modifier.testTag("severe_toggle"),
                        detail = v.severeIntervalText,
                    )
                }
                Spacer(Modifier.width(DndSpace.xxxl))
                Column(Modifier.weight(0.54f).fillMaxHeight().testTag("vd_compare")) {
                    Caption(v.severeTitle)
                    SevereTable(v, state.tableForm, Modifier.weight(1f))
                }
            }
        }
    }
}

/** "지난 교체 2025.12.20" in the form of the bar heads above it: pictogram, grey label, value. */
@Composable
private fun FactLine(fact: VehicleFact) {
    val type = DndTheme.type
    Row(Modifier.height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        Pictogram(fact.kind.icon(), size = PictogramSize.Small)
        Spacer(Modifier.width(DndSpace.s))
        SingleLineText(fact.label, type.labelStrong, color = DndTheme.palette.textSecondary)
        Spacer(Modifier.width(DndSpace.s))
        SingleLineText(fact.value, type.bodyStrong)
    }
}

/**
 * The comparison table: the column headers, then one row per condition filling the column, so its
 * bottom is level with the switch box. Both number columns are as wide as the widest header or
 * value of either, so the values stay right-aligned under their headers in every scenario and the
 * two columns look alike; the name column takes the rest.
 */
@Composable
private fun SevereTable(v: VehicleCardState, form: SevereTableForm, modifier: Modifier) {
    val type = DndTheme.type
    val palette = DndTheme.palette
    val rows = v.severeRows
    if (rows.isEmpty()) {
        Spacer(modifier)
        return
    }
    val headerStyle = type.labelStrong
    val valueStyle = type.keyText
    val usualStyle = type.keyText.copy(fontWeight = type.body.fontWeight)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val numberWidth = remember(rows, v.thisTripHeader, v.usualHeader, type, density) {
        fun widest(header: String, values: List<String>, style: TextStyle): Int =
            maxOf(measurer.measure(header, headerStyle).size.width, values.maxOf { measurer.measure(it, style).size.width })
        with(density) {
            // 2 dp of slack against dp-to-pixel rounding (see the replay panel's signal columns).
            maxOf(widest(v.thisTripHeader, rows.map { it.value }, valueStyle), widest(v.usualHeader, rows.map { it.usual }, usualStyle)).toDp() + 2.dp
        }
    }
    val valueWidth = numberWidth
    val usualWidth = numberWidth
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            SingleLineText(v.thisTripHeader, headerStyle, Modifier.width(valueWidth), color = palette.textSecondary, textAlign = TextAlign.End)
            Spacer(Modifier.width(NumberGap))
            SingleLineText(v.usualHeader, headerStyle, Modifier.width(usualWidth), color = palette.textSecondary, textAlign = TextAlign.End)
        }
        Column(Modifier.weight(1f).testTag("vd_comparison_rows")) {
            rows.forEach { row ->
                Hairline()
                SevereRow(row, form, valueWidth, usualWidth, Modifier.weight(1f))
            }
        }
    }
}

/**
 * One condition: pictogram, its name, this trip's value and the usual value. The manual's phrase is
 * a quotation, kept whole and grey, on two lines where it is wider than its column; the short name
 * is one line in the text colour, as the item names on the evidence screen.
 */
@Composable
private fun SevereRow(
    row: SevereConditionLine,
    form: SevereTableForm,
    valueWidth: Dp,
    usualWidth: Dp,
    modifier: Modifier,
) {
    val type = DndTheme.type
    val palette = DndTheme.palette
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(IconColumn)) { Pictogram(row.kind.icon(), size = PictogramSize.Small) }
        when (form) {
            SevereTableForm.ManualPhrase -> BalancedText(row.phrase, type.label, Modifier.weight(1f), color = palette.textSecondary)
            SevereTableForm.ShortLabel -> SingleLineText(row.item, type.bodyStrong, Modifier.weight(1f))
        }
        Spacer(Modifier.width(ColumnGap))
        SingleLineText(row.value, type.keyText, Modifier.width(valueWidth), textAlign = TextAlign.End)
        Spacer(Modifier.width(NumberGap))
        SingleLineText(row.usual, type.keyText.copy(fontWeight = type.body.fontWeight), Modifier.width(usualWidth), textAlign = TextAlign.End)
    }
}
