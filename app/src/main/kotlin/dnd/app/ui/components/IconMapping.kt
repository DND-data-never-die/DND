package dnd.app.ui.components

import dnd.app.ui.icons.DndIcon
import dnd.app.ui.icons.DndIcons
import dnd.app.ui.model.BriefingIcon
import dnd.app.ui.model.SevereKind
import dnd.app.ui.model.SignalKind
import dnd.app.ui.model.StepKind
import dnd.app.ui.model.VehicleEvidenceKind

/** One pictogram per kind; the screens never pick an icon from text. */
fun SevereKind.icon(): DndIcon = when (this) {
    SevereKind.ShortTrips -> DndIcons.ShortTrips
    SevereKind.StopAndGo -> DndIcons.TrafficLight
    SevereKind.Congestion -> DndIcons.Congestion
    SevereKind.HighSpeedHarsh -> DndIcons.HarshEvents
    SevereKind.Cold -> DndIcons.Thermometer
}

fun SignalKind.icon(): DndIcon = when (this) {
    SignalKind.Speed -> DndIcons.Gauge
    SignalKind.Gear -> DndIcons.GearShift
    SignalKind.Steering -> DndIcons.Steering
    SignalKind.Distance -> DndIcons.Route
    SignalKind.OutsideTemperature -> DndIcons.Thermometer
}

fun StepKind.icon(): DndIcon = when (this) {
    StepKind.Split -> DndIcons.Split
    StepKind.Compute -> DndIcons.Bars
    StepKind.Compare -> DndIcons.Compare
}

fun BriefingIcon.icon(): DndIcon = when (this) {
    BriefingIcon.Fuel -> DndIcons.FuelPump
    BriefingIcon.Washer -> DndIcons.Washer
    BriefingIcon.OilCan -> DndIcons.OilCan
    BriefingIcon.ServiceRecord -> DndIcons.Wrench
    BriefingIcon.Check -> DndIcons.Check
}

fun VehicleEvidenceKind.icon(): DndIcon = when (this) {
    VehicleEvidenceKind.Odometer -> DndIcons.Route
    VehicleEvidenceKind.ServiceRecord -> DndIcons.Wrench
    VehicleEvidenceKind.Interval -> DndIcons.Calendar
}
