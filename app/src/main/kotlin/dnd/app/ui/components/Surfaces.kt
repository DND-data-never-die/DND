package dnd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dnd.app.ui.icons.DndIcon
import dnd.app.ui.icons.DndIcons
import dnd.app.ui.icons.Pictogram
import dnd.app.ui.icons.PictogramSize
import dnd.app.ui.model.FactorKind
import dnd.app.ui.model.Tone
import dnd.app.ui.theme.AttentionLabelStyle
import dnd.app.ui.theme.AttentionStyle
import dnd.app.ui.theme.DndShapes
import dnd.app.ui.theme.DndSpace
import dnd.app.ui.theme.DndTheme
import dnd.app.ui.theme.container
import dnd.app.ui.theme.onContainer

/** Card: white surface, outline, 24 dp corners. */
@Composable
fun DndCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val palette = DndTheme.palette
    Column(
        modifier = modifier
            .background(palette.surface, DndShapes.card)
            .border(DndSpace.outlineWidth, palette.outline, DndShapes.card)
            .padding(horizontal = DndSpace.cardPaddingHorizontal, vertical = DndSpace.cardPaddingVertical),
        content = content,
    )
}

/** Card header: pictogram and title. A card's action is a button at its bottom, on the driver's side. */
@Composable
fun CardHeader(icon: DndIcon, title: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Pictogram(icon, size = PictogramSize.Small)
        Spacer(Modifier.width(DndSpace.s))
        SingleLineText(title, DndTheme.type.cardTitleStrong, Modifier.weight(1f))
    }
}

/**
 * Status label. Attention ("평소보다 높음", "교환 시점 지남"): by default a red fill with white text and
 * no dot (owner, 2026-11-15); [AttentionLabelStyle] switches to the dot-and-text form. In grey
 * print the fill reads darker than any neutral tag, which is outline-only. Good: soft tint with a
 * dot. Neutral and information: filled.
 */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, tone: Tone = Tone.Neutral, style: TextStyle = DndTheme.type.keyText) {
    val palette = DndTheme.palette
    val horizontal = if (style.fontSize.value >= 60f) DndSpace.xl else DndSpace.l
    when {
        tone == Tone.Attention && AttentionLabelStyle == AttentionStyle.Filled -> Box(
            modifier = modifier
                .background(palette.attention, DndShapes.pill)
                .padding(horizontal = horizontal, vertical = DndSpace.xs),
        ) {
            SingleLineText(text, style, color = palette.onAttention)
        }
        tone == Tone.Attention || tone == Tone.Good -> Row(
            modifier = modifier
                .background(if (tone == Tone.Attention) palette.attentionTint else palette.goodTint, DndShapes.pill)
                .padding(horizontal = horizontal, vertical = DndSpace.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentDot(palette.container(tone), 12.dp)
            Spacer(Modifier.width(DndSpace.s))
            SingleLineText(text, style, color = palette.text)
        }
        else -> Box(
            modifier = modifier
                .background(palette.container(tone), DndShapes.pill)
                .padding(horizontal = horizontal, vertical = DndSpace.xs),
        ) {
            SingleLineText(text, style, color = palette.onContainer(tone))
        }
    }
}

/** A small solid dot. */
@Composable
fun AccentDot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(color, CircleShape))
}

/** Outlined pill, optionally with a pictogram before the text. */
@Composable
fun OutlinePill(
    text: String,
    modifier: Modifier = Modifier,
    icon: DndIcon? = null,
    style: TextStyle = DndTheme.type.labelStrong,
) {
    val palette = DndTheme.palette
    Row(
        modifier = modifier
            .border(DndSpace.outlineWidth, palette.outline, DndShapes.pill)
            .padding(start = if (icon != null) DndSpace.s else DndSpace.m, end = DndSpace.m, top = DndSpace.xxs, bottom = DndSpace.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Pictogram(icon, size = PictogramSize.Small)
            Spacer(Modifier.width(DndSpace.xs))
        }
        SingleLineText(text, style)
    }
}

/** Secondary-surface box, as tall as its content plus padding. */
@Composable
fun SecondaryBox(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DndSpace.touchTarget + DndSpace.m)
            .background(DndTheme.palette.surfaceSecondary, DndShapes.box)
            .padding(horizontal = DndSpace.l, vertical = DndSpace.s),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * Guidance: secondary box with an information bar on its left edge, an optional pictogram and one
 * instruction; two lines at most.
 */
@Composable
fun GuidanceBox(icon: DndIcon?, text: String, modifier: Modifier = Modifier, style: TextStyle = DndTheme.type.keyText) {
    val palette = DndTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(DndShapes.box)
            .background(palette.surfaceSecondary)
            // The information bar on the left edge, as tall as the box.
            .drawBehind { drawRect(palette.info, size = Size(6.dp.toPx(), size.height)) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The 6-dp bar is drawn inside the 24-dp margin, so the text starts 24 from the box edge (r4 D4).
        Spacer(Modifier.width(DndSpace.l))
        if (icon != null) {
            Pictogram(icon, size = PictogramSize.Medium, tint = palette.text)
            Spacer(Modifier.width(DndSpace.m))
        }
        BalancedText(text, style, Modifier.weight(1f).padding(end = DndSpace.l, top = DndSpace.m, bottom = DndSpace.m))
    }
}

/** Neutral tag ("가장 큰 요인"): outline only, so in grey print it never looks like an attention label. */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(DndSpace.outlineWidth, DndTheme.palette.outline, DndShapes.pill)
            .padding(horizontal = DndSpace.m, vertical = DndSpace.xxs),
    ) {
        SingleLineText(text, DndTheme.type.labelStrong)
    }
}

/** One line of text that shows an ellipsis instead of wrapping, so overflow is visible. */
@Composable
fun SingleLineText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = DndTheme.palette.text,
    textAlign: TextAlign? = null,
) {
    Text(text, modifier = modifier, style = style, color = color, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, textAlign = textAlign)
}

/**
 * "1,850km", "1시간 10분": digit runs in [numberStyle], every other run (units, words) in
 * [unitStyle], on one baseline.
 */
@Composable
fun NumberWithUnit(text: String, numberStyle: TextStyle, unitStyle: SpanStyle, modifier: Modifier = Modifier) {
    fun isNumberChar(c: Char) = c.isDigit() || c == ',' || c == '.'
    val annotated = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val number = isNumberChar(text[i])
            var j = i
            while (j < text.length && isNumberChar(text[j]) == number) j++
            if (number) append(text.substring(i, j)) else withStyle(unitStyle) { append(text.substring(i, j)) }
            i = j
        }
    }
    Text(annotated, modifier = modifier, style = numberStyle, color = DndTheme.palette.text, maxLines = 1, softWrap = false)
}

/** "또는 1,850km": the words before the first digit in the secondary colour, the rest in the text colour. */
@Composable
fun LeadInText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val palette = DndTheme.palette
    val split = text.indexOfFirst { it.isDigit() }.let { if (it < 0) 0 else it }
    val annotated = buildAnnotatedString {
        withStyle(SpanStyle(color = palette.textSecondary)) { append(text.substring(0, split)) }
        append(text.substring(split))
    }
    Text(annotated, modifier = modifier, style = style, color = palette.text, maxLines = 1, softWrap = false)
}

/**
 * Values joined by " 또는 " or " · " ("15,000km 또는 12개월", "2025.12.20 · 29,230km") drawn as
 * separate elements, so each stays within 10 characters (UI round 3, addendum B1); "또는" stays
 * with the value after it.
 */
@Composable
fun PairText(text: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = DndTheme.palette.text) {
    val parts = text.split(Regex(" · | (?=또는 )"))
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        parts.forEachIndexed { i, part ->
            if (i > 0) Spacer(Modifier.width(DndSpace.m))
            SingleLineText(part, style, color = color)
        }
    }
}

/** Pictogram for each factor of the fatigue index. */
fun FactorKind.icon(): DndIcon = when (this) {
    FactorKind.Dawn -> DndIcons.Moon
    FactorKind.ContinuousDriving -> DndIcons.Clock
    FactorKind.HarshEvents -> DndIcons.HarshEvents
    FactorKind.Curves -> DndIcons.Curve
    FactorKind.Congestion -> DndIcons.Congestion
}

/** Thin horizontal rule in the divider colour. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(DndTheme.palette.divider))
}

/**
 * Text that never breaks inside a word: when [text] is wider than the space, it is split once at
 * the space that gives the most even two lines, preferring the space after a comma.
 */
@Composable
fun BalancedText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = DndTheme.palette.text,
    textAlign: TextAlign = TextAlign.Start,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val maxPx = constraints.maxWidth
        val shown = remember(text, style, maxPx) {
            fun width(s: String) = measurer.measure(s, style, softWrap = false, maxLines = 1).size.width
            if (width(text) <= maxPx) {
                text
            } else {
                val splits = text.indices.filter { text[it] == ' ' }.map { i -> i to maxOf(width(text.substring(0, i)), width(text.substring(i + 1))) }
                val afterComma = splits.filter { (i, w) -> i > 0 && text[i - 1] == ',' && w <= maxPx }.minByOrNull { it.second }
                (afterComma ?: splits.minByOrNull { it.second })?.let { (i, _) -> text.substring(0, i) + "\n" + text.substring(i + 1) } ?: text
            }
        }
        Text(shown, style = style, color = color, textAlign = textAlign, softWrap = false, maxLines = 2)
    }
}
