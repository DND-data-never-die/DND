package dnd.core

/** spec 8: the compared vehicle state (fuel, washer, oil and brake-fluid levels, service records). */
data class VehicleState(
    val fuelPercent: Double,
    val washerLow: Boolean,
    val engineOil: FluidLevel,
    val brakeFluid: FluidLevel,
    val records: List<MaintenanceRecord>,
)

data class Briefing(
    /** What changed, in the order of spec 8's list; empty when nothing changed. */
    val changes: List<String>,
    /** The one line the screen shows. */
    val line: String,
    /** True when nothing changed and the line is the service countdown. */
    val showsCountdown: Boolean,
)

private fun levelText(level: FluidLevel): String = when (level) {
    FluidLevel.LOW -> "낮음"
    FluidLevel.NORMAL -> "정상"
}

/**
 * spec 8: compare the state when the selected driver last finished driving with the state now.
 * - Fuel changed: "연료 45% → 15%". SPEC GAP: fuel counts as changed when the displayed integer
 *   percent differs (raw sensor values always differ a little).
 * - Washer: "워셔액 부족" when it became low. SPEC GAP: when it is no longer low, "워셔액 보충".
 * - SPEC GAP: level changes are written "엔진 오일 레벨 정상 → 낮음", "브레이크액 레벨 정상 → 낮음".
 * - New service record: "엔진 오일 교체 완료". The line names what changed and nothing else
 *   (owner, 2026-11-15: no stress on what did not change).
 * - Nothing changed: the countdown line from [countdownWhenUnchanged].
 * SPEC GAP: several changes are joined with ", " in spec 8's order (fuel, washer, levels, records).
 */
fun briefing(last: VehicleState, now: VehicleState, countdownWhenUnchanged: Countdown): Briefing {
    val state = mutableListOf<String>()
    val lastFuel = Fmt.roundHalfUp(last.fuelPercent)
    val nowFuel = Fmt.roundHalfUp(now.fuelPercent)
    if (lastFuel != nowFuel) state += "연료 $lastFuel% → $nowFuel%"
    if (last.washerLow != now.washerLow) state += if (now.washerLow) "워셔액 부족" else "워셔액 보충"
    if (last.engineOil != now.engineOil) state += "엔진 오일 레벨 ${levelText(last.engineOil)} → ${levelText(now.engineOil)}"
    if (last.brakeFluid != now.brakeFluid) state += "브레이크액 레벨 ${levelText(last.brakeFluid)} → ${levelText(now.brakeFluid)}"
    val newRecords = now.records.filter { it !in last.records }
    val recordTexts = newRecords.map { "${it.item.label} 교체 완료" }.distinct()
    val changes = state + recordTexts
    val line = when {
        changes.isEmpty() -> countdownWhenUnchanged.text
        else -> changes.joinToString(", ")
    }
    return Briefing(changes, line, changes.isEmpty())
}
