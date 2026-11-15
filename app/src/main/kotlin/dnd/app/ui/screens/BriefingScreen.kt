package dnd.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dnd.app.ui.components.BalancedText
import dnd.app.ui.components.CardHeader
import dnd.app.ui.components.DndCard
import dnd.app.ui.components.OutlineButton
import dnd.app.ui.components.Pill
import dnd.app.ui.model.Tone
import dnd.app.ui.components.ScreenScaffold
import dnd.app.ui.components.icon
import dnd.app.ui.icons.DndIcons
import dnd.app.ui.icons.Pictogram
import dnd.app.ui.icons.PictogramSize
import dnd.app.ui.model.BriefingUiState
import dnd.app.ui.theme.DndSpace
import dnd.app.ui.theme.DndTheme
import dnd.app.ui.theme.DriverSide

/** Screen C: one sentence before departure, with its pictograms above it. */
@Composable
fun BriefingScreen(
    state: BriefingUiState,
    modifier: Modifier = Modifier,
    showDemoControls: Boolean = false,
    onDemoClick: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onWeekly: () -> Unit = {},
) {
    val type = DndTheme.type
    ScreenScaffold(state.topBar, modifier, showDemoControls, onDemoClick, onBack) {
        DndCard(Modifier.fillMaxWidth().weight(1f)) {
            CardHeader(DndIcons.Car, state.title)
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // State pictograms in the text colour (4.5:1); the sentence carries the state.
                Row(horizontalArrangement = Arrangement.spacedBy(DndSpace.xxl), verticalAlignment = Alignment.CenterVertically) {
                    state.icons.forEach { Pictogram(it.icon.icon(), size = PictogramSize.Hero) }
                    state.attentionLabel?.let { Pill(it, tone = Tone.Attention) }
                }
                Spacer(Modifier.height(DndSpace.xl))
                // One measure for all three briefings: wide enough for the record line, narrower than
                // the handover line, so all three become centred two-line blocks (r4 D10).
                BalancedText(state.sentence, type.keyValue, Modifier.widthIn(max = 960.dp), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.weight(1f))
            // The button on the driver's side.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = if (DndTheme.driverSide == DriverSide.LEFT) Arrangement.Start else Arrangement.End,
            ) {
                OutlineButton(state.weeklyLabel, onWeekly, chevron = true)
            }
        }
    }
}
