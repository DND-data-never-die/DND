package dnd.app

import dnd.app.ui.model.DriverCardState
import dnd.app.ui.model.FactLine
import dnd.app.ui.model.FactorKind
import dnd.app.ui.model.GradeScale
import dnd.app.ui.model.ProgressState
import dnd.app.ui.model.SelfCheckState
import dnd.app.ui.model.SevereConditionLine
import dnd.app.ui.model.SevereKind
import dnd.app.ui.model.Tone
import dnd.app.ui.model.TopBarState
import dnd.app.ui.model.TripEndUiState
import dnd.app.ui.model.VehicleCardState

/**
 * Scenario 1 (trip end at 17:10) with the values of the application text. The vehicle hero is the
 * limit reached first: 327 of 365 days (89.6 %) against 13,150 of 15,000 km (87.7 %), so "38일".
 */
object Scenario1Fixture {
    fun tripEnd(selfCheck: Int?) = TripEndUiState(
        topBar = TopBarState(driverName = "운전자 1"),
        driver = DriverCardState(
            index = "42",
            levelPill = "평소보다 높음",
            levelTone = Tone.Attention,
            grade = GradeScale(value = 41.89f, lowUpper = 19.44f, midUpper = 30f, activeSegment = 2, max = 100f),
            largestFactor = FactLine(FactorKind.ContinuousDriving, "연속 주행 2시간 40분", "가장 큰 요인"),
            comparison = FactLine(FactorKind.Congestion, "정체 48분", "평소의 2배"),
            guidance = "돌아가는 길에는 휴게소에서 15분 쉬어 가세요",
        ),
        vehicle = VehicleCardState(
            dueHero = "38일",
            dueHeroOverdue = false,
            dueSecond = "또는 1,850km",
            distanceProgress = ProgressState(current = 13_150f, total = 15_000f, lead = "교체 후", valueLabel = "13,150 / 15,000km"),
            timeProgress = ProgressState(current = 327f, total = 365f, lead = "교체 후", valueLabel = "327 / 365일"),
            severeRows = listOf(
                SevereConditionLine(SevereKind.Congestion, "교통 체증이 심한 구역 주행", "정체", "48분", "24분"),
                SevereConditionLine(SevereKind.HighSpeedHarsh, "잦은 고속 주행 및 급가감속", "급가감속", "9회", "6회"),
            ),
            severeIntervalOn = false,
            severeIntervalText = "7,500km 또는 6개월",
        ),
        selfCheck = SelfCheckState(selected = selfCheck),
    )
}
