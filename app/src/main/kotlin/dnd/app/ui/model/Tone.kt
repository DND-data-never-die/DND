package dnd.app.ui.model

/**
 * Meaning of a coloured element; the palette turns it into a colour. Every toned element also
 * keeps its text or pictogram, so the meaning survives black-and-white print.
 */
enum class Tone {
    /** Plain emphasis without a judgement. */
    Neutral,

    /** Needs attention: higher than usual, too short, overdue, low. */
    Attention,

    /** Active or normal: on, recording, nothing changed, lower than usual. */
    Good,

    /** Information and advice. */
    Info,
}
