package dnd.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import dnd.app.ui.components.Caption
import dnd.app.ui.components.CardButton
import dnd.app.ui.components.CardHeader
import dnd.app.ui.components.DndCard
import dnd.app.ui.components.GuidanceBox
import dnd.app.ui.components.NumberWithUnit
import dnd.app.ui.components.Pill
import dnd.app.ui.components.ScreenScaffold
import dnd.app.ui.components.SelfCheckStrip
import dnd.app.ui.components.SingleLineText
import dnd.app.ui.components.Tag
import dnd.app.ui.components.icon
import dnd.app.ui.icons.DndIcons
import dnd.app.ui.icons.Pictogram
import dnd.app.ui.icons.PictogramSize
import dnd.app.ui.model.DriverCardState
import dnd.app.ui.model.Tone
import dnd.app.ui.model.TripEndUiState
import dnd.app.ui.model.VehicleCardState
import dnd.app.ui.theme.DndMotion
import dnd.app.ui.theme.DndShapes
import dnd.app.ui.theme.DndSpace
import dnd.app.ui.theme.DndTheme
import dnd.app.ui.theme.DriverSide
import dnd.app.ui.theme.motion

/**
 * Height of the hero row, shared by both cards so the two heroes sit on one line. The 200-sp line
 * box leaves 46 dp above the digits and 10 below; the row reports 176 and lifts the text 30, so the
 * digits have 16 above and below and a label centred in the row sits on their middle (r4 D2).
 */
internal val HeroRowHeight = 176.dp
private val HeroLift = 30.dp

/** The hero text in a row of [HeroRowHeight]: measured unconstrained, lifted by [HeroLift]. */
internal fun Modifier.heroBox(): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    layout(p.width, HeroRowHeight.roundToPx()) { p.place(0, -HeroLift.roundToPx()) }
}

/**
 * Screen A, a glance screen: each card holds its title, one hero with its caption, one status or
 * second value, one supporting group and one action button at the bottom, on the driver's side
 * (at most 4 information units per card; the button is not counted). Without a driver card the
 * vehicle card takes the full width.
 */
@Composable
fun TripEndScreen(
    state: TripEndUiState,
    modifier: Modifier = Modifier,
    showDemoControls: Boolean = false,
    onDemoClick: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    /** Null hides the card's action. */
    onDriverEvidence: (() -> Unit)? = {},
    onVehicleDetail: (() -> Unit)? = {},
    onSelfCheck: (Int) -> Unit = {},
    /** Entry motion, 0..1 of [DndMotion.ENTER_MS]; 1 is the finished screen (captures). */
    enter: Float = 1f,
) {
    val t = enter.coerceIn(0f, 1f) * DndMotion.ENTER_MS
    // The cards fade in and rise 20 ms apart; the self-check strip follows.
    val first = DndMotion.phase(t, 0, 400)
    val second = DndMotion.phase(t, DndMotion.STAGGER_MS, 400 + DndMotion.STAGGER_MS)
    val strip = DndMotion.phase(t, 150, 550)
    ScreenScaffold(state.topBar, modifier, showDemoControls, onDemoClick, onBack) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(DndSpace.cardGap),
        ) {
            val driver = state.driver
            if (driver != null) {
                DriverCard(driver, onDriverEvidence, Modifier.weight(1f).fillMaxHeight().testTag("driver_card").motion(first, rise = 12.dp))
            }
            VehicleCard(
                state.vehicle,
                onVehicleDetail,
                Modifier.weight(1f).fillMaxHeight().testTag("vehicle_card").motion(if (driver != null) second else first, rise = 12.dp),
            )
        }
        Spacer(Modifier.height(DndSpace.l))
        // The strip is the driver card's footer: exactly the card's width (r4 D6).
        SelfCheckStrip(state.selfCheck, onSelfCheck, Modifier.motion(strip), width = (DndSpace.canvasWidth - DndSpace.screenPadding * 2 - DndSpace.cardGap) / 2)
    }
}

@Composable
private fun DriverCard(state: DriverCardState, onEvidence: (() -> Unit)?, modifier: Modifier) {
    val type = DndTheme.type
    DndCard(modifier) {
        CardHeader(DndIcons.Driver, state.title)
        Spacer(Modifier.height(DndSpace.l))
        Caption(state.indexCaption)
        Row(Modifier.fillMaxWidth().height(HeroRowHeight), verticalAlignment = Alignment.CenterVertically) {
            SingleLineText(state.index, type.heroNumber, Modifier.heroBox())
            Spacer(Modifier.width(DndSpace.xl))
            Pill(state.levelPill, tone = state.levelTone)
        }
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Pictogram(state.largestFactor.kind.icon(), size = PictogramSize.Small)
            Spacer(Modifier.width(DndSpace.s))
            SingleLineText(state.largestFactor.value, type.bodyStrong)
            Spacer(Modifier.width(DndSpace.m))
            Tag(state.largestFactor.note)
        }
        Spacer(Modifier.height(DndSpace.m))
        // The guidance takes the rest of the card, its text grown towards the optimum (red team M2).
        val guidance = state.guidance
        if (guidance != null) {
            GuidanceBox(null, guidance, Modifier.weight(1f).testTag("guidance_box"), style = type.keyTextLarge)
        } else {
            Spacer(Modifier.weight(1f))
        }
        CardAction(state.evidenceLabel, onEvidence, "driver_button")
    }
}

@Composable
private fun VehicleCard(state: VehicleCardState, onDetail: (() -> Unit)?, modifier: Modifier) {
    DndCard(modifier) {
        CardHeader(DndIcons.Car, state.title)
        Spacer(Modifier.height(DndSpace.l))
        Caption(state.dueCaption, icon = DndIcons.OilCan)
        DueHero(state, Modifier.height(HeroRowHeight))
        DueSecondLine(state)
        Spacer(Modifier.height(DndSpace.m))
        if (state.severeRows.isNotEmpty()) SevereValuesBox(state, Modifier.weight(1f).testTag("comparison_box")) else Spacer(Modifier.weight(1f))
        CardAction(state.detailLabel, onDetail, "vehicle_button")
    }
}

/** The card's action: a full button at the bottom, on the driver's side. */
@Composable
private fun CardAction(label: String, onClick: (() -> Unit)?, tag: String) {
    if (onClick == null) return
    Spacer(Modifier.height(DndSpace.m))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (DndTheme.driverSide == DriverSide.LEFT) Arrangement.Start else Arrangement.End,
    ) {
        CardButton(label, onClick, Modifier.testTag(tag))
    }
}

/**
 * The vehicle card's comparison: "가혹 조건 비교" and one line per condition, this trip's value with
 * its usual value ("정체 48분 · 평소 24분": two information units per line). The manual's phrases
 * stay on the detail screen.
 */
@Composable
private fun SevereValuesBox(state: VehicleCardState, modifier: Modifier) {
    val type = DndTheme.type
    val palette = DndTheme.palette
    // The box takes the rest of the card; the values grown towards the optimum (red team M2).
    Column(
        modifier
            .fillMaxWidth()
            .background(palette.surfaceSecondary, DndShapes.box)
            .padding(horizontal = DndSpace.l, vertical = DndSpace.m),
        verticalArrangement = Arrangement.Center,
    ) {
        SingleLineText(state.severeTitle, type.labelStrong, color = palette.textSecondary)
        Spacer(Modifier.height(DndSpace.xs))
        state.severeRows.forEach { row ->
            Row(Modifier.height(68.dp), verticalAlignment = Alignment.CenterVertically) {
                SingleLineText(row.itemValue, type.keyTextLarge)
                Spacer(Modifier.width(DndSpace.l))
                SingleLineText(row.usualText, type.keyTextLarge.copy(fontWeight = type.body.fontWeight), color = palette.textSecondary)
            }
        }
    }
}

/**
 * The limit reached first as the hero ("38일"). A passed limit shows its number with the attention
 * label "지남" beside it, with no tint behind the hero (red team R3).
 */
@Composable
internal fun DueHero(state: VehicleCardState, modifier: Modifier = Modifier) {
    val type = DndTheme.type
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val unitStyle = SpanStyle(fontSize = type.heroUnit.fontSize, fontWeight = type.heroUnit.fontWeight)
        if (state.dueHeroOverdue) {
            NumberWithUnit(state.dueHero.removeSuffix(" ${state.overdueLabel}"), type.heroNumber, unitStyle, Modifier.heroBox())
            Spacer(Modifier.width(DndSpace.xl))
            Pill(state.overdueLabel, tone = Tone.Attention)
        } else {
            NumberWithUnit(state.dueHero, type.heroNumber, unitStyle, Modifier.heroBox())
        }
    }
}

/** "또는 1,850km", "거리로는 3,910km 남음": the lead-in grey at 40, the value at the key size. */
@Composable
internal fun DueSecondLine(state: VehicleCardState) {
    val type = DndTheme.type
    Row(Modifier.height(64.dp), verticalAlignment = Alignment.CenterVertically) {
        val split = state.dueSecond.indexOfFirst { it.isDigit() }.coerceAtLeast(0)
        SingleLineText(state.dueSecond.take(split).trimEnd(), type.labelStrong, color = DndTheme.palette.textSecondary)
        Spacer(Modifier.width(DndSpace.s))
        SingleLineText(state.dueSecond.drop(split), type.keyText)
    }
}
