package dnd.app.ui.model

import dnd.core.FatigueItem
import dnd.core.demo.Scenario1Screens
import java.util.Locale

/**
 * Screen B, behind the driver card's "상세 정보": which items made the fatigue index. No research
 * sources, no weights and no formula on the driver's screen (owner, 2026-11-15: the formula belongs
 * in the manual). Each item shows this trip's value in the driver's units and its 부담 (owner: a
 * "score" reads as if higher were better). Rows are sorted by 부담, largest first; an item that did not occur is not shown
 * (owner, round 4: no statements that something is absent).
 */
data class EvidenceUiState(
    val topBar: TopBarState,
    val rows: List<EvidenceLine>,
    /** "41.9": the exact sum of the 부담 values (the unrounded index to one decimal). */
    val loadSum: String,
    /** "42": the index as the card shows it. */
    val total: String,
    val grade: GradeScale,
    /** The screen's title in the top bar, after the back icon (the subject of the screen). */
    val title: String = "피로도 지수",
    val loadHeader: String = "부담",
    val loadSumLabel: String = "부담 합계",
    /** The same name as on the trip-end card. */
    val totalLabel: String = "피로도 지수",
)

/** One item: name, this trip's value (null for 커브 주행) and its 부담. */
data class EvidenceLine(
    val kind: FactorKind,
    val label: String,
    val value: String?,
    /** 부담 as shown ("20.0"). */
    val load: String,
    val loadPoints: Float,
)

fun Scenario1Screens.toEvidenceUiState(): EvidenceUiState {
    val d = driverCard
    return EvidenceUiState(
        topBar = appTopBar(),
        // An item that did not occur (value 0) is left out, as the muted "… 없음" row was drawn
        // without a 부담 before; the sum line still shows the whole index.
        rows =d.evidence.filter { it.value != 0.0 }.map { e ->
            val value = when (e.item) {
                // "9회, 크기 합 115" -> "9회" (the magnitude sum means nothing to a driver; red team E2).
                FatigueItem.HARSH -> e.valueText.substringBefore(", ")
                // The curve value is a sum of lateral accelerations without a driver's unit: name, bar, 부담 only.
                FatigueItem.CURVE -> null
                else -> e.valueText
            }
            EvidenceLine(
                kind = e.item.toFactorKind(),
                label = e.label,
                value = value,
                load = e.scoreText,
                loadPoints = e.score.toFloat(),
            )
        }.sortedByDescending { it.loadPoints },
        loadSum = String.format(Locale.ROOT, "%.1f", d.index),
        total = d.indexRounded.toString(),
        // The three grade words and the marker; no bound numbers (round 4: fewer texts).
        grade = d.gradeScale(),
    )
}
