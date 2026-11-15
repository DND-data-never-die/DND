package dnd.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dnd.app.ui.model.Tone

/**
 * Every colour the app draws. Composables read colours only from here (DndTheme.palette), so a
 * different palette is one object.
 *
 * Colour carries meaning only (attention, active, information); everything else is grey. A state
 * is never told by colour alone: red and green print as nearly the same grey, so each coloured
 * element keeps its text label or pictogram.
 */
@Immutable
data class DndPalette(
    /** Screen background. */
    val background: Color,
    /** Cards. */
    val surface: Color,
    /** Inner boxes, tags. */
    val surfaceSecondary: Color,
    /** Card outline. */
    val outline: Color,
    /**
     * Boundaries of controls (buttons, self-check circles, inactive grade segments, the unplayed
     * replay track): 3:1 or more against their background (WCAG 2.1 SC 1.4.11).
     */
    val outlineStrong: Color,
    /** Fill of the guide's basic button (button_basic_enabled). */
    val buttonFill: Color,
    /** Hairlines between rows. */
    val divider: Color,
    /** Text and pictograms. */
    val text: Color,
    /** Captions, typical values, units. */
    val textSecondary: Color,
    /** Filled neutral controls: selected self-check button, the gear pill, neutral pills, filled buttons. */
    val emphasis: Color,
    val onEmphasis: Color,
    /** Unfilled part of bars and inactive grade segments. */
    val track: Color,
    /**
     * Chart bars that are not flagged: no lighter than text_secondary, since they carry values
     * (guide: 4.5:1 for icons, text and other images). Flagged bars are [text].
     */
    val chartMuted: Color,
    /**
     * What needs attention. Red appears only as the fill of an attention label (white text) and
     * as the grade marker ring (owner, 2026-11-15); no red pictograms or chart dots.
     */
    val attention: Color,
    val onAttention: Color,
    /** Soft background of an attention pill: [attention] at 12 % over the card surface. */
    val attentionTint: Color,
    /** An active or normal state: switch on, recording, grade 낮음, nothing changed. */
    val good: Color,
    val onGood: Color,
    /** Soft background of a good-state pill or mark: [good] at 12 % over the card surface. */
    val goodTint: Color,
    /** Information and advice: guidance, the voice prompt, the filled part of data bars. */
    val info: Color,
    val onInfo: Color,
    /** Dims the screen behind a sheet. */
    val scrim: Color,
)

/** Fill and content colour of a filled element with this tone. */
fun DndPalette.container(tone: Tone): Color = when (tone) {
    Tone.Neutral -> emphasis
    Tone.Attention -> attention
    Tone.Good -> good
    Tone.Info -> info
}

fun DndPalette.onContainer(tone: Tone): Color = when (tone) {
    Tone.Neutral -> onEmphasis
    Tone.Attention -> onAttention
    Tone.Good -> onGood
    Tone.Info -> onInfo
}

/** Grade marker ring: 낮음 good, 보통 neutral, 높음 attention. The segment itself is always neutral. */

/**
 * How an attention label looks: [Filled] (red fill, white text; the owner's choice of 2026-11-15)
 * or [Dot] (red dot and dark text on a soft tint, the earlier form he may still pick). One switch.
 */
enum class AttentionStyle { Filled, Dot }

val AttentionLabelStyle = AttentionStyle.Filled
fun DndPalette.gradeColor(segment: Int): Color = when (segment) {
    0 -> good
    1 -> emphasis
    else -> attention
}

/**
 * Light theme of the Pleos Connect UI design guide (Foundations > Colors): background_primary,
 * surface_basic, surface_high, basic_300 outline, basic_200 track, text/icon #131417 (secondary
 * at 64 %), button_filled basic_600, and the informative tokens negative, active and positive.
 * White on the red is 3.56:1 (the owner chose it, 2026-11-15: "글자는 배경색으로"); white on blue is 4.92:1.
 */
val PleosLightPalette = DndPalette(
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceSecondary = Color(0xFFEDEEF2),
    outline = Color(0xFFC7C9CE),
    // basic_500: 5.4:1 on white, 3.4:1 on #131417.
    outlineStrong = Color(0xFF686A72),
    // alpha_dark_50: #131417 at 5 %.
    buttonFill = Color(0x0D131417),
    divider = Color(0xFFC7C9CE),
    text = Color(0xFF131417),
    textSecondary = Color(0xA3131417),
    emphasis = Color(0xFF44464E),
    onEmphasis = Color(0xFFFFFFFF),
    track = Color(0xFFE0E1E6),
    chartMuted = Color(0xA3131417),
    attention = Color(0xFFFE3D16),
    onAttention = Color(0xFFFFFFFF),
    attentionTint = Color(0xFFFFE8E3),
    good = Color(0xFF02C265),
    onGood = Color(0xFF131417),
    goodTint = Color(0xFFE1F8ED),
    info = Color(0xFF0064FF),
    onInfo = Color(0xFFFFFFFF),
    scrim = Color(0x33131417),
)

/** The earlier black-and-white palette, kept for comparison prints. */
val GreyPalette = DndPalette(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceSecondary = Color(0xFFF2F2F2),
    outline = Color(0xFF111111),
    outlineStrong = Color(0xFF111111),
    buttonFill = Color(0xFFF2F2F2),
    divider = Color(0xFFCCCCCC),
    text = Color(0xFF111111),
    textSecondary = Color(0xFF555555),
    emphasis = Color(0xFF111111),
    onEmphasis = Color(0xFFFFFFFF),
    track = Color(0xFFCCCCCC),
    chartMuted = Color(0xFFCCCCCC),
    attention = Color(0xFF111111),
    onAttention = Color(0xFFFFFFFF),
    attentionTint = Color(0xFFF2F2F2),
    good = Color(0xFF111111),
    onGood = Color(0xFFFFFFFF),
    goodTint = Color(0xFFF2F2F2),
    info = Color(0xFF111111),
    onInfo = Color(0xFFFFFFFF),
    scrim = Color(0x99111111),
)

val LocalDndPalette = staticCompositionLocalOf { PleosLightPalette }
