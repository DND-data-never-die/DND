package dnd.app.ui.model

import dnd.core.Fmt
import dnd.core.Gear
import dnd.core.Sample
import dnd.core.demo.Scenario1Screens
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Screen E, 운행 기록 재생, at one scenario instant: a pure function of the time ([replayStateAt]),
 * so frames can be rendered at any chosen instant.
 *
 * The application says "주행 중에는 화면을 띄우지 않지만, 연속 주행이 2시간에 이르면 음성 한 문장으로 휴식을
 * 권합니다", and the voice is not built yet (TTS not provided). So in D the app window shows
 * nothing; in P it shows its notification and, after the last trip, the processing steps. The
 * recording, the clock, the signals and the voice prompt live in the demo panel, which reads as
 * outside the app.
 */
data class ReplayState(
    val topBar: TopBarState,
    val driving: Boolean,
    /** Processing steps after the last trip ends; empty before. */
    val steps: List<ProcessingStep>,
    val notice: NoticeState?,
    val panel: DemoPanelState,
)

/** The demo's player panel: clock, the log's timeline with its times, the recorded signals (the gear among them). */
data class DemoPanelState(
    val clock: String,
    val timeline: TimelineState,
    val signals: List<SignalValue>,
    /** The voice sentence from the prompt's second for its duration; null otherwise. */
    val voicePrompt: String?,
    val title: String = "운행 기록",
    /** The label before the voice sentence, its own element (no status note; owner, 2026-11-15). */
    val voiceLabel: String = "음성 안내",
)

/**
 * Fractions of the 13:00-17:10 log: driving legs and the cursor. The line carries times alone
 * ([events]); what happened is named above it ([marks]) (owner, 2026-11-15).
 */
data class TimelineState(
    val legs: List<ClosedFloatingPointRange<Float>>,
    val cursor: Float,
    val events: List<TimelineEvent>,
    val marks: List<TimelineMark>,
)

/** Where a time's label sits against its place: starting there, centred on it or ending there. */
enum class LabelAnchor { Start, Center, End }

/**
 * A time under the track ("14:15"). The start of a driving leg has its label starting there, the
 * end of a leg ending there, a time inside a leg centred under a tick. [active] while it is the
 * latest time.
 */
data class TimelineEvent(val at: Float, val label: String, val active: Boolean, val anchor: LabelAnchor)

/** The name of what happened ("휴식", "음성 안내"), above the track and centred on [at]; [active] with its time. */
data class TimelineMark(val at: Float, val label: String, val active: Boolean)

enum class SignalKind { Speed, Gear, Steering, Distance, OutsideTemperature }

/**
 * A recorded signal with its live value ("속도", "87km/h") and the widest values it takes
 * ([widest]), which set its column's width so the columns do not move as the values change.
 */
data class SignalValue(val kind: SignalKind, val label: String, val value: String, val widest: List<String> = listOf(value))

/**
 * The widest value of each signal in the log: a three-digit speed, a 25-degree turn to either side,
 * a five-digit odometer, a two-digit temperature in whole degrees. The panel's figures are tabular
 * (every digit has the same width), so one string stands for every value of its length.
 */
private const val SpeedWidest = "103km/h"
private val SteeringWidest = listOf("L 25.0°", "R 25.0°")
private const val DistanceWidest = "42,380km"
private const val TemperatureWidest = "15°C"

enum class StepKind { Split, Compute, Compare }

data class ProcessingStep(val kind: StepKind, val label: String, val done: Boolean)

/** "운행을 마쳤습니다" with the button that opens the cards. */
data class NoticeState(val text: String, val actionLabel: String)

/** Display choices for the replay (not definitions of the engine). */
object ReplayTiming {
    /** The voice prompt line stays for three scenario minutes. */
    const val VOICE_PROMPT_SEC = 180

    /** After the log ends, one processing step ticks every 10 s, then the notification appears. */
    const val STEP_SEC = 10
    const val STEP_COUNT = 3

    /** Scenario seconds after the log end when the notification with "카드 보기" appears. */
    const val NOTICE_AFTER_END_SEC = (STEP_COUNT + 1) * STEP_SEC
}

private const val RestLabel = "휴식"
private const val VoiceLabel = "음성 안내"
private const val NoticeText = "운행을 마쳤습니다"
private const val NoticeAction = "카드 보기"
private val StepLabels = listOf(StepKind.Split to "운행 구분", StepKind.Compute to "지표 계산", StepKind.Compare to "평소와 비교")

/** The replay at scenario time [t] (epoch seconds); t past the log end runs the processing steps. */
fun Scenario1Screens.replayStateAt(t: Long): ReplayState {
    val r = replay
    val start = r.logStartEpochSec
    val end = r.logEndEpochSec
    val span = (end - start).toFloat()
    fun frac(s: Long) = ((s - start) / span).coerceIn(0f, 1f)

    // Events appear exactly at their second: the frames of 14:15:00 and 16:30:00 show them.
    val driving = r.legs.any { (a, b) -> t in a until b }
    val afterEnd = t >= end
    val sinceEnd = t - end
    val atRest = t >= r.restStop.first && t < r.restStop.second
    val prompting = t >= r.voicePromptEpochSec && t < r.voicePromptEpochSec + ReplayTiming.VOICE_PROMPT_SEC

    val steps = if (afterEnd) {
        StepLabels.mapIndexed { i, (kind, label) -> ProcessingStep(kind, label, done = sinceEnd >= (i + 1L) * ReplayTiming.STEP_SEC) }
    } else {
        emptyList()
    }
    val notice = when {
        afterEnd && sinceEnd >= ReplayTiming.NOTICE_AFTER_END_SEC -> NoticeState(NoticeText, NoticeAction)
        atRest -> NoticeState(NoticeText, NoticeAction)
        else -> null
    }
    val clockAt = minOf(t, end)
    val sample = sampleAt(clockAt)
    val times = listOf(start, r.restStop.first, r.restStop.second, r.voicePromptEpochSec, end)
    val latest = times.indexOfLast { t >= it }
    return ReplayState(
        topBar = appTopBar(),
        driving = driving,
        steps = steps,
        notice = notice,
        panel = DemoPanelState(
            clock = clockText(clockAt),
            timeline = TimelineState(
                legs = r.legs.map { (a, b) -> frac(a)..frac(b) },
                cursor = frac(clockAt),
                // The five times: departure, the rest's two ends, the voice prompt, arrival.
                events = times.mapIndexed { i, time ->
                    val anchor = when {
                        i == 0 || r.legs.any { it.first == time } -> LabelAnchor.Start
                        i == times.lastIndex || r.legs.any { it.second == time } -> LabelAnchor.End
                        else -> LabelAnchor.Center
                    }
                    TimelineEvent(if (i == times.lastIndex) 1f else frac(time), clockText(time), active = i == latest, anchor = anchor)
                },
                // The rest is named over the gap between its two times, the voice prompt over its time.
                marks = listOf(
                    TimelineMark((frac(r.restStop.first) + frac(r.restStop.second)) / 2, RestLabel, active = latest == 1),
                    TimelineMark(frac(r.voicePromptEpochSec), VoiceLabel, active = latest == 3),
                ),
            ),
            signals = listOf(
                SignalValue(SignalKind.Speed, "속도", "${sample.speedKmh.roundToLong()}km/h", listOf(SpeedWidest)),
                SignalValue(SignalKind.Gear, "기어", sample.gear.name, Gear.entries.map { it.name }),
                // One format per signal (red team M5): angles with one decimal.
                SignalValue(SignalKind.Steering, "조향각", steeringText(sample.steeringDeg), SteeringWidest),
                SignalValue(SignalKind.Distance, "누적 주행거리", "${Fmt.integer(sample.odometerKm)}km", listOf(DistanceWidest)),
                // The outside temperature in whole degrees, rounded half up, as a car's display shows it (round 4).
                SignalValue(SignalKind.OutsideTemperature, "외부 온도", "${Fmt.roundHalfUp(sample.outsideTempC)}°C", listOf(TemperatureWidest)),
            ),
            voicePrompt = if (prompting) r.voicePromptText else null,
        ),
    )
}

/**
 * The side as a letter and the angle with one decimal ("L 0.2°", "R 25.0°"; owner, 2026-11-15: L and
 * R, no signs). The log's positive angles are to the right. An angle that reads 0.0 has no side.
 */
internal fun steeringText(deg: Double): String {
    val text = oneDecimal(abs(deg), "°")
    return when {
        text == oneDecimal(0.0, "°") -> text
        deg < 0 -> "L $text"
        else -> "R $text"
    }
}

/** The log's 1 Hz sample at [t], or the last one before it. */
private fun Scenario1Screens.sampleAt(t: Long): Sample {
    val samples = replay.samples
    val i = samples.binarySearchBy(t) { it.epochSec }
    return samples[if (i >= 0) i else (-i - 2).coerceIn(0, samples.lastIndex)]
}

/** Scenario instants used by the captures and the video frames. */
val Scenario1Screens.replayStart: Long get() = replay.logStartEpochSec
val Scenario1Screens.replayEnd: Long get() = replay.logEndEpochSec
