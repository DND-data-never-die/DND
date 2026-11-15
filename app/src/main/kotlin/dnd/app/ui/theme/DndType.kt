package dnd.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dnd.app.R

/** Asta Sans, the Pleos Connect system font (SIL OFL 1.1, licenses/AstaSans-OFL.txt): static TTFs only. */
val AstaSans = FontFamily(
    Font(R.font.asta_sans_regular, FontWeight.Normal),
    Font(R.font.asta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.asta_sans_extrabold, FontWeight.ExtraBold),
)

/**
 * Type scale in sp; on the Connect-S display (160 DPI) 1 sp = 1 px, and 1 mm = 6.80 px (UI round 3
 * brief, addendum A):
 * - every text: 40 or more. Kim & Park 2012 (J Ergon Soc Korea 31(2)): Hangul on an in-vehicle
 *   screen is legible from about 14 pt = 5.47 mm = 37 px;
 * - key information (hero, status label, guidance sentence, the values a driver acts on, button
 *   labels): 56 (the guide's Headline Medium), at least 50. Kim, Kim & Lee 2015 (Korean J Cogn Biol
 *   Psychol 27(3)): readers aged 60 to 78 keep their top speed from 0.52 degrees = 43 px at 70 cm,
 *   taken as glyph height; the Asta Sans Hangul glyph is 0.875 of the font size.
 * 40/52 is the guide's Title Large. The hero sizes above the scale are a choice for the glance.
 */
@Immutable
data class DndTypeScale(
    /** Card heroes ("42", "38일"). */
    val heroNumber: TextStyle,
    /** Unit or word set beside a card hero ("일", "km", "지남"). */
    val heroUnit: TextStyle,
    /** Weekly heroes, the briefing sentence, the evidence total. */
    val keyValue: TextStyle,
    /** Key information: status labels, guidance, button labels, the values a driver acts on. */
    val keyText: TextStyle,
    /**
     * Key text grown towards the optimum where a card has room: 22 pt = 8.59 mm = 58 px (Kim & Park
     * 2012), for the trip-end guidance and comparison.
     */
    val keyTextLarge: TextStyle,
    val cardTitle: TextStyle,
    val cardTitleStrong: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    /** Captions, axis labels, the manual's phrases: no smaller tier than [body] any more. */
    val label: TextStyle,
    val labelStrong: TextStyle,
    val wordmark: TextStyle,
)

private fun style(size: Int, lineHeight: Int, weight: FontWeight, letterSpacing: TextUnit = 0.sp) = TextStyle(
    fontFamily = AstaSans,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = letterSpacing,
    // Centre the glyphs in the line box so rows of different sizes align on their middles.
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    // Korean breaks between words, not inside them.
    lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase),
)

/** The smallest text and the key-information size (sp). */
const val TextMinSp = 40
const val KeyTextSp = 56

val DefaultTypeScale = DndTypeScale(
    heroNumber = style(200, 200, FontWeight.ExtraBold, letterSpacing = (-0.02).em),
    heroUnit = style(72, 80, FontWeight.SemiBold),
    keyValue = style(96, 104, FontWeight.ExtraBold, letterSpacing = (-0.01).em),
    keyText = style(KeyTextSp, 64, FontWeight.SemiBold),
    keyTextLarge = style(58, 68, FontWeight.SemiBold),
    cardTitle = style(TextMinSp, 52, FontWeight.Normal),
    cardTitleStrong = style(TextMinSp, 52, FontWeight.SemiBold),
    body = style(TextMinSp, 52, FontWeight.Normal),
    bodyStrong = style(TextMinSp, 52, FontWeight.SemiBold),
    label = style(TextMinSp, 52, FontWeight.Normal),
    labelStrong = style(TextMinSp, 52, FontWeight.SemiBold),
    wordmark = style(TextMinSp, 52, FontWeight.ExtraBold, letterSpacing = 0.08.em),
)

val LocalDndTypeScale = staticCompositionLocalOf { DefaultTypeScale }
