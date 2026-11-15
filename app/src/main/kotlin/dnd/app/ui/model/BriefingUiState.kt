package dnd.app.ui.model

import dnd.core.demo.BriefingUi
import dnd.core.demo.Scenario2Screens

/** Screen C, 출발 전 브리핑: one sentence is the whole message. */
data class BriefingUiState(
    val topBar: TopBarState,
    /** Pictograms above the sentence, one per thing it mentions, with their tone. */
    val icons: List<BriefingPictogram>,
    val sentence: String,
    val title: String = "출발 전 브리핑",
    val weeklyLabel: String = "주간 요약",
) {
    /** One red label when something needs attention (fuel down, washer low), as on the other screens. */
    val attentionLabel: String? get() = if (icons.any { it.tone == Tone.Attention }) AttentionLabel else null
}

private const val AttentionLabel = "확인 필요"

enum class BriefingIcon { Fuel, Washer, OilCan, ServiceRecord, Check }

/**
 * A briefing pictogram, drawn in the text colour (red team X3: state colours fail 4.5:1 as icons);
 * the tone is kept for a badge the screen may add.
 */
data class BriefingPictogram(val icon: BriefingIcon, val tone: Tone = Tone.Neutral)

/** The three briefings of the application: after the oil change was entered, a handover, nothing changed. */
enum class BriefingCase { AfterService, Handover, Unchanged }

/**
 * The countdown briefing (nothing changed since the last trip) shows the oil can alone: the
 * sentence is the countdown, and nothing on the screen stresses that nothing changed (owner,
 * 2026-11-15: "지난 운행 뒤 달라진 게 없다는 걸 강조할 건 없잖아").
 */
private val CountdownIcons = listOf(BriefingPictogram(BriefingIcon.OilCan))

fun Scenario2Screens.toBriefingUiState(case: BriefingCase): BriefingUiState = when (case) {
    BriefingCase.AfterService -> afterServiceBriefing.toState()
    BriefingCase.Handover -> handoverBriefing.toState()
    // The facade has no nothing-changed briefing. The engine's briefing() returns the countdown line
    // when nothing changed, so this uses the countdown at the handover's departure time, on the
    // severe-condition cycle scenario 2's driver chose (red team N1).
    BriefingCase.Unchanged -> BriefingUiState(
        topBar = appTopBar(),
        icons = CountdownIcons,
        sentence = handoverBriefing.countdownSevere.firstLimitText(),
    )
}

private fun BriefingUi.toState(): BriefingUiState {
    val b = briefing
    return BriefingUiState(
        topBar = appTopBar(),
        // Subject first, then the check (red team B2).
        icons = if (b.showsCountdown) {
            CountdownIcons
        } else {
            b.changes.map(::pictogramFor).distinct() +
                // Only service records were entered: the check says the record is in.
                if (b.changes.all { it.contains(RecordWords) }) listOf(BriefingPictogram(BriefingIcon.Check, Tone.Good)) else emptyList()
        },
        sentence = line,
    )
}

/** The ending of a new service record's phrase ("엔진 오일 교체 완료"). */
private const val RecordWords = "교체 완료"

/**
 * The facade gives the changes as text only, so the pictogram and its tone are read from the
 * words: "연료 45% → 15%" is a drop (attention), "워셔액 부족" and "... → 낮음" are low (attention).
 */
private fun pictogramFor(change: String): BriefingPictogram = when {
    change.startsWith("연료") -> {
        val values = Regex("""(\d+)%""").findAll(change).map { it.groupValues[1].toInt() }.toList()
        val drop = values.size == 2 && values[1] < values[0]
        BriefingPictogram(BriefingIcon.Fuel, if (drop) Tone.Attention else Tone.Neutral)
    }
    change.startsWith("워셔액") -> BriefingPictogram(BriefingIcon.Washer, if (change.endsWith("부족")) Tone.Attention else Tone.Neutral)
    change.contains(RecordWords) -> BriefingPictogram(BriefingIcon.OilCan)
    else -> BriefingPictogram(BriefingIcon.ServiceRecord, if (change.endsWith("낮음")) Tone.Attention else Tone.Neutral)
}
