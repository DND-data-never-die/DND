package dnd.app.ui.model

/**
 * What the trip-end screen shows. Every string and number on the screen comes from here; the
 * composables add no copy of their own. Fixed copy has defaults; values come from the engine.
 */
data class TripEndUiState(
    val topBar: TopBarState,
    /** Null when the engine has no driver card for the trip; the vehicle card then takes the full width. */
    val driver: DriverCardState?,
    val vehicle: VehicleCardState,
    val selfCheck: SelfCheckState,
)

/**
 * The app's title row. The Pleos status bar above the app window already shows the clock and the
 * driving status, so the app draws neither.
 */
data class TopBarState(
    val driverName: String,
    val wordmark: String = "DND",
    val demoLabel: String = "데모",
    /** Not drawn: the back control is the "<" icon alone (Pleos page navigation, Depth); read out by TalkBack. */
    val backLabel: String = "뒤로",
)

/** Factor kinds of the fatigue index; each has its own pictogram. */
enum class FactorKind { Dawn, ContinuousDriving, HarshEvents, Curves, Congestion }

data class DriverCardState(
    /** Fatigue index as shown ("42"). */
    val index: String,
    /** Status label next to the index ("평소보다 높음") and its tone (attention for 높음). */
    val levelPill: String,
    val levelTone: Tone,
    val grade: GradeScale,
    /** Largest factor of this trip, e.g. ContinuousDriving / "연속 주행 2시간 40분". */
    val largestFactor: FactLine,
    /** A comparison with the usual trips, e.g. Congestion / "정체 48분" / "평소의 2배"; null hides the row. */
    val comparison: FactLine?,
    /** Rest advice; null hides the box. */
    val guidance: String?,
    val title: String = "운전자 카드",
    val indexCaption: String = "피로도 지수",
    /** The card's button (Hyundai's on-screen "상세 정보" with the ">" suffix icon). */
    val evidenceLabel: String = "상세 정보",
)

/**
 * Index scale split into low, middle and high grades at [lowUpper] and [midUpper]. [activeSegment]
 * (0 low, 1 middle, 2 high) is the engine's grade, not recomputed by the UI.
 */
data class GradeScale(
    val value: Float,
    val lowUpper: Float,
    val midUpper: Float,
    val activeSegment: Int,
    val max: Float = 100f,
    val labels: List<String> = listOf("낮음", "보통", "높음"),
    /** Bound values written at the two segment edges ("19", "30"); null hides them and the ends. */
    val boundLabels: Pair<String, String>? = null,
    val startLabel: String = "0",
    val endLabel: String = "100",
)

/** A pictogram, a bold value and a quieter note after it. */
data class FactLine(val kind: FactorKind, val value: String, val note: String)

data class VehicleCardState(
    /**
     * The limit reached first, shown as the hero: the one whose used share (used ÷ interval) is
     * larger, "38일"; a passed limit ("160일 지남") once one has passed.
     */
    val dueHero: String,
    /** The hero has passed its service point. */
    val dueHeroOverdue: Boolean,
    /** One line under the hero: "또는 1,850km", "거리로는 3,910km 남음", "거리로도 5,650km 지남". */
    val dueSecond: String,
    val distanceProgress: ProgressState,
    val timeProgress: ProgressState,
    /** The severe conditions that occurred in this trip; one that did not is left out (round 4). */
    val severeRows: List<SevereConditionLine>,
    val severeIntervalOn: Boolean,
    /** The severe-condition interval beside the switch ("7,500km 또는 6개월"). */
    val severeIntervalText: String,
    val title: String = "차량 카드",
    /** "엔진 오일 교체까지", or "엔진 오일 교체 시기" once a limit has passed. */
    val dueCaption: String = "엔진 오일 교체까지",
    /** Attention label of a passed limit, beside its number ("160일" + "지남"). */
    val overdueLabel: String = "지남",
    /** The card's one action on the trip-end screen (as the driver card's), and the title of the screen it opens. */
    val detailLabel: String = "상세 정보",
    val detailTitle: String = "엔진 오일 교체 시기",
    val severeTitle: String = "가혹 조건 비교",
    val thisTripHeader: String = "이번 운행",
    /** The column of the usual values: one word for the whole column (red team V2; round 4). */
    val usualHeader: String = "평소",
    /** The manual's term; the switch itself says "apply". */
    val severeSwitchLabel: String = "가혹 조건 주기",
)

/**
 * A bar from 0 to [total] filled to [current]: "주행" + "13,150 / 15,000km". Past the interval
 * the bar runs to [current] and the part beyond the interval is hatched (survives grey print).
 */
data class ProgressState(
    val current: Float,
    val total: Float,
    /** "주행", "기간" */
    val lead: String,
    /** "13,150 / 15,000km", "342 / 182일" */
    val valueLabel: String,
    val overdue: Boolean = false,
)

/** The manual's severe conditions; each has its own pictogram. */
enum class SevereKind { ShortTrips, StopAndGo, Congestion, HighSpeedHarsh, Cold }

/**
 * One severe-condition row: the manual's phrase, the item's short name and this trip's value
 * ("정체", "48분"), the usual value over the last 28 days ("24분").
 */
data class SevereConditionLine(
    val kind: SevereKind,
    val phrase: String,
    val item: String,
    val value: String,
    val usual: String,
) {
    /** "정체 48분" */
    val itemValue: String get() = "$item $value"

    /** "평소 24분", where the column header does not already say "평소" (the trip-end card). */
    val usualText: String get() = "평소 $usual"
}

/**
 * The vehicle card's detail screen: countdown, both bars with the fact behind them, the switch,
 * the severe conditions of this trip in the table form [tableForm].
 */
data class VehicleDetailUiState(
    val topBar: TopBarState,
    val vehicle: VehicleCardState,
    val facts: List<VehicleFact>,
    val tableForm: SevereTableForm = SevereTableForm.ShortLabel,
)

/**
 * The two forms of the "가혹 조건 비교" table (round 4): each row named by the item's short name
 * ("운행 거리", "정체"; the default), or by the manual's phrase (the alternative).
 */
enum class SevereTableForm { ManualPhrase, ShortLabel }

enum class VehicleEvidenceKind { Odometer, ServiceRecord, Interval }

/** A fact behind the countdown: grey label and value ("지난 교체", "2025.12.20"). */
data class VehicleFact(val kind: VehicleEvidenceKind, val label: String, val value: String)

data class SelfCheckState(
    /** 1 to [levels], or null when nothing is selected yet. */
    val selected: Int? = null,
    val levels: Int = 9,
    val question: String = "지금 얼마나 졸리신가요?",
    /** The scale's ends (KSS 1 "extremely alert", 9 "very sleepy"; red team T5, Codex 7). */
    val lowCaption: String = "정신이 매우 맑음",
    val highCaption: String = "매우 졸림",
)
