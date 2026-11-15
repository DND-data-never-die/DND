package dnd.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Motion, following the Pleos guide: each movement lasts 100 to 450 ms and eases with
 * cubic-bezier(0.16, 1, 0.3, 1); nothing moves for decoration while driving. Screens take a
 * progress value and are pure functions of it, so captures (progress 1) stay deterministic.
 */
object DndMotion {
    val Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    /**
     * The launch sequence: at most 1.0 s (Android: a splash should not exceed 1,000 ms; Pleos:
     * do not stay long on the splash), cold start only, and it ends early when the first screen
     * is ready.
     */
    const val LAUNCH_MS = 1_000

    /** From here the launch screen cross-fades into the first screen (250 ms). */
    const val LAUNCH_FADE_START_MS = 750

    /**
     * The launch composition, fades one after another, each inside the guide's 100 to 450 ms: the
     * wordmark over 300 ms, the tagline from 150 ms, the two cards from 400 ms (20 ms apart); all
     * finished at [LAUNCH_FADE_START_MS].
     */
    const val LAUNCH_WORDMARK_END_MS = 300
    const val LAUNCH_TAGLINE_START_MS = 150
    const val LAUNCH_TAGLINE_END_MS = 450
    const val LAUNCH_CARDS_START_MS = 400

    /** Window of the entry of a screen; each movement inside it lasts at most 450 ms. */
    const val ENTER_MS = 900

    /** Stagger between cards or rows (Material: no more than 20 ms apart). */
    const val STAGGER_MS = 20

    /** Opening a detail screen: the incoming screen scales from 92 % and fades in. */
    const val DETAIL_OPEN_MS = 300
    const val DETAIL_SCALE_FROM = 0.92f

    /** Going back is shorter. */
    const val BACK_MS = 250

    /**
     * Any other screen change, a fade-through: the outgoing screen fades out over the first 35 %,
     * then the incoming one fades in and grows from [DETAIL_SCALE_FROM] (Material fade through;
     * MDC-Android FadeThroughProvider uses the 0.35 threshold).
     */
    const val FADE_THROUGH_MS = 300
    const val FADE_THROUGH_OUT_MS = FADE_THROUGH_MS * 35 / 100

    /** Eased progress of one movement from [startMs] to [endMs], read at [timeMs] of its timeline. */
    fun phase(timeMs: Float, startMs: Int, endMs: Int): Float =
        Easing.transform(linear(timeMs, startMs, endMs))

    /**
     * Linear progress, for opacity: with the eased curve a fade looks finished after about a third
     * of its time (red team M3: the wordmark appeared in about 100 ms of its 300).
     */
    fun linear(timeMs: Float, startMs: Int, endMs: Int): Float =
        ((timeMs - startMs) / (endMs - startMs).toFloat()).coerceIn(0f, 1f)
}

/** Opacity [fade] (0..1, linear) with a move [move] (0..1, eased) rising by [rise]. */
fun Modifier.fadeMove(fade: Float, move: Float, rise: Dp = 0.dp): Modifier =
    if (fade >= 1f && move >= 1f) {
        this
    } else {
        graphicsLayer {
            alpha = fade
            translationY = (rise * (1f - move)).toPx()
        }
    }

/**
 * Fade in with [progress] (0..1), rising by [rise] and growing from [scaleFrom]. At progress 1 the
 * modifier adds nothing, so the finished state renders exactly as without motion.
 */
fun Modifier.motion(progress: Float, rise: Dp = 0.dp, scaleFrom: Float = 1f): Modifier =
    if (progress >= 1f) {
        this
    } else {
        graphicsLayer {
            alpha = progress
            translationY = (rise * (1f - progress)).toPx()
            val s = scaleFrom + (1f - scaleFrom) * progress
            scaleX = s
            scaleY = s
        }
    }
