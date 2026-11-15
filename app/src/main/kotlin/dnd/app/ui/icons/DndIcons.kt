package dnd.app.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dnd.app.ui.theme.DndTheme

/**
 * One line pictogram: SVG path strings on a 24 x 24 grid, drawn as strokes only with round
 * caps and joins, front or side view (no perspective). The stroke width comes from the size
 * class, so every pictogram of one size has the same line weight.
 */
@Immutable
class DndIcon(val name: String, val paths: List<String>)

/**
 * Size classes, each as tall as the text it stands beside (UI round 3 brief, section 2): Small for
 * text at 40, Medium for key text at 56. The stroke is 0.075 x the pictogram
 * size (the Pleos guide: 3.6 px on a 48 px grid).
 */
enum class PictogramSize(val size: Dp, val gridStroke: Float) {
    Small(40.dp, PictogramStroke),
    Medium(56.dp, PictogramStroke),
    Large(72.dp, PictogramStroke),
    Hero(96.dp, PictogramStroke),
}

/** 0.075 x the 24-unit grid. */
private const val PictogramStroke = 1.8f

object DndIcons {
    val Driver = DndIcon(
        "driver",
        listOf(
            "M16,7 A4,4 0 1 1 8,7 A4,4 0 1 1 16,7 Z",
            "M4,21 C4,16.5 7.6,13.5 12,13.5 C16.4,13.5 20,16.5 20,21",
        ),
    )

    /** Car seen from the front. */
    val Car = DndIcon(
        "car",
        listOf(
            "M5.5,10 L7.3,4.9 C7.6,4.1 8.2,3.5 9,3.5 H15 C15.8,3.5 16.4,4.1 16.7,4.9 L18.5,10",
            "M5,10 H19 A2,2 0 0 1 21,12 V17.5 H3 V12 A2,2 0 0 1 5,10 Z",
            "M6,17.5 V21 M18,17.5 V21",
            "M8.25,13.5 A1.25,1.25 0 1 1 5.75,13.5 A1.25,1.25 0 1 1 8.25,13.5 Z",
            "M18.25,13.5 A1.25,1.25 0 1 1 15.75,13.5 A1.25,1.25 0 1 1 18.25,13.5 Z",
            "M10.5,13.5 H13.5",
        ),
    )

    val Clock = DndIcon(
        "clock",
        listOf(
            "M21,12 A9,9 0 1 1 3,12 A9,9 0 1 1 21,12 Z",
            "M12,7 V12 L15.5,14.25",
        ),
    )

    /** Dawn driving. */
    val Moon = DndIcon(
        "moon",
        listOf("M20,14 A8.6,8.6 0 1 1 10,4 A7.4,7.4 0 0 0 20,14 Z"),
    )

    /** Congestion: two cars seen from the front, side by side on a road line. */
    val Congestion = DndIcon(
        "congestion",
        listOf(
            frontCar(1.5f),
            frontCar(12.5f),
            "M1.5,21.5 H22.5",
        ),
    )

    /** Harsh acceleration and braking: a sharp rise next to a sharp drop. */
    val HarshEvents = DndIcon(
        "harsh_events",
        listOf(
            "M7.5,21 V3 M3,7.5 L7.5,3 L12,7.5",
            "M16.5,3 V21 M12,16.5 L16.5,21 L21,16.5",
        ),
    )

    /** Curve driving. */
    val Curve = DndIcon(
        "curve",
        listOf(
            "M5,21 V13.5 A6.5,6.5 0 0 1 11.5,7 H19.5",
            "M15.5,3 L19.5,7 L15.5,11",
        ),
    )

    /** Rest stop. */
    val Coffee = DndIcon(
        "coffee",
        listOf(
            "M4.5,9.5 H15.5 V14.5 A4.5,4.5 0 0 1 11,19 H9 A4.5,4.5 0 0 1 4.5,14.5 Z",
            "M15.5,11 H17 A2.5,2.5 0 0 1 17,16 H15.3",
            "M8.25,2.75 C7.25,3.9 9.25,5.1 8.25,6.25",
            "M11.75,2.75 C10.75,3.9 12.75,5.1 11.75,6.25",
            "M3,21.25 H17",
        ),
    )

    val OilCan = DndIcon(
        "oil_can",
        listOf(
            "M2.5,9 H11 L15.5,13 V21 H2.5 Z",
            "M15.5,13 L21.5,8",
            "M5,9 V3 H9 V9",
            "M21,12 C21,12 19.5,14.1 19.5,15.1 A1.5,1.5 0 0 0 22.5,15.1 C22.5,14.1 21,12 21,12 Z",
        ),
    )

    /** Speed: a speedometer dial, drawn to fill the grid so it is as tall as the text beside it. */
    val Gauge = DndIcon(
        "gauge",
        listOf(
            "M5.28,19.72 A9.5,9.5 0 1 1 18.72,19.72",
            "M12,13 L16.5,8.5",
            "M13.3,13 A1.3,1.3 0 1 1 10.7,13 A1.3,1.3 0 1 1 13.3,13 Z",
            "M8,21 H16",
        ),
    )

    val Calendar = DndIcon(
        "calendar",
        listOf(
            "M5,5.5 H19 A2,2 0 0 1 21,7.5 V19 A2,2 0 0 1 19,21 H5 A2,2 0 0 1 3,19 V7.5 A2,2 0 0 1 5,5.5 Z",
            "M3,10 H21",
            "M8,3 V7.5 M16,3 V7.5",
        ),
    )

    /** Drawn tall (18 of 24 units) so it is as high as the label beside it. */
    val Chevron = DndIcon("chevron", listOf("M8,3 L17,12 L8,21"))

    /** Drawn tall (13 of 24 units) so it is as high as the text beside it. */
    val Check = DndIcon("check", listOf("M3.5,12.5 L9.5,18.5 L20.5,5.5"))

    /** Gear selector: knob, lever, base. */
    val GearShift = DndIcon(
        "gear_shift",
        listOf(
            "M15,6 A3,3 0 1 1 9,6 A3,3 0 1 1 15,6 Z",
            "M12,9 V16",
            "M7.5,16 H16.5 A1.5,1.5 0 0 1 18,17.5 V20.5 H6 V17.5 A1.5,1.5 0 0 1 7.5,16 Z",
        ),
    )

    val Steering = DndIcon(
        "steering",
        listOf(
            "M21,12 A9,9 0 1 1 3,12 A9,9 0 1 1 21,12 Z",
            "M14.5,12 A2.5,2.5 0 1 1 9.5,12 A2.5,2.5 0 1 1 14.5,12 Z",
            "M3,12 H9.5 M14.5,12 H21 M12,14.5 V21",
        ),
    )

    /** Distance driven (odometer, distance since the last change): a route between two points. */
    val Route = DndIcon(
        "route",
        listOf(
            "M7,19 A2,2 0 1 1 3,19 A2,2 0 1 1 7,19 Z",
            "M21,5 A2,2 0 1 1 17,5 A2,2 0 1 1 21,5 Z",
            "M5,17 C5,11.5 19,12.5 19,7",
        ),
    )

    val Thermometer = DndIcon(
        "thermometer",
        listOf(
            "M10,15 V5 A2,2 0 0 1 14,5 V15 A4,4 0 1 1 10,15 Z",
            "M12,18.5 V9",
            "M16.5,6 H19.5 M16.5,9.5 H19.5 M16.5,13 H19.5",
        ),
    )

    /** Voice prompt. */
    val Speaker = DndIcon(
        "speaker",
        listOf(
            "M3,9 H7 L12.5,4 V20 L7,15 H3 Z",
            "M15.5,8.5 A5,5 0 0 1 15.5,15.5",
            "M18.5,5.5 A9,9 0 0 1 18.5,18.5",
        ),
    )

    val FuelPump = DndIcon(
        "fuel_pump",
        listOf(
            "M4,21 V5 A1.5,1.5 0 0 1 5.5,3.5 H12.5 A1.5,1.5 0 0 1 14,5 V21",
            "M2.5,21 H15.5",
            "M6.5,7 H11.5 V11 H6.5 Z",
            "M14,10 H15.5 A1.5,1.5 0 0 1 17,11.5 V16.5 A1.5,1.5 0 0 0 20,16.5 V8.5 L17.5,6",
        ),
    )

    /** Washer fluid: a windscreen with a spray. */
    val Washer = DndIcon(
        "washer",
        listOf(
            "M4,18.5 L5.5,8.5 Q12,6.5 18.5,8.5 L20,18.5 Z",
            "M12,15.5 V12.5 M9,15.25 L8,12.75 M15,15.25 L16,12.75",
        ),
    )

    /** Service record. */
    val Wrench = DndIcon(
        "wrench",
        listOf(
            "M19.2,6.2 L16.4,9 L15,7.6 L17.8,4.8 A4.5,4.5 0 0 0 11.6,10.8 L4.9,17.5 A1.7,1.7 0 0 0 7.6,19.6 L14.3,12.9 A4.5,4.5 0 0 0 19.2,6.2 Z",
        ),
    )

    /** Stop and go. */
    val TrafficLight = DndIcon(
        "traffic_light",
        listOf(
            "M7,3 H17 A2,2 0 0 1 19,5 V19 A2,2 0 0 1 17,21 H7 A2,2 0 0 1 5,19 V5 A2,2 0 0 1 7,3 Z",
            "M14,7.5 A2,2 0 1 1 10,7.5 A2,2 0 1 1 14,7.5 Z",
            "M14,12 A2,2 0 1 1 10,12 A2,2 0 1 1 14,12 Z",
            "M14,16.5 A2,2 0 1 1 10,16.5 A2,2 0 1 1 14,16.5 Z",
        ),
    )

    /** Processing step: splitting the log into trips (scissors). */
    val Split = DndIcon(
        "split",
        listOf(
            "M9,6 A3,3 0 1 1 3,6 A3,3 0 1 1 9,6 Z",
            "M9,18 A3,3 0 1 1 3,18 A3,3 0 1 1 9,18 Z",
            "M8.6,7.6 L21,18.5 M8.6,16.4 L21,5.5",
        ),
    )

    /** Processing step: computing the indicators. */
    val Bars = DndIcon(
        "bars",
        listOf(
            "M4.5,21 V13 M9.5,21 V5 M14.5,21 V9 M19.5,21 V3",
        ),
    )

    /** Processing step: comparing with the usual trips (two opposite arrows). */
    val Compare = DndIcon(
        "compare",
        listOf(
            "M3.5,7 H20 M16,3 L20,7 L16,11",
            "M20.5,17 H4 M8,13 L4,17 L8,21",
        ),
    )

    /** Playback of the demo's trip log. */
    val Play = DndIcon(
        "play",
        listOf(
            "M6,3 L20,12 L6,21 Z",
        ),
    )

    /** Departure-time spread: two limits with a double arrow between them. */
    val Spread = DndIcon(
        "spread",
        listOf(
            "M3,3 V21 M21,3 V21",
            "M6.5,12 H17.5 M10,8.5 L6.5,12 L10,15.5 M14,8.5 L17.5,12 L14,15.5",
        ),
    )

    /** Rest between two days of driving: a bed seen from the side. */
    val Bed = DndIcon(
        "bed",
        listOf(
            "M2.5,4 V21 M21.5,13 V21",
            "M2.5,13 H21.5 M2.5,17.5 H21.5",
            "M5.5,13 V10 A1.5,1.5 0 0 1 7,8.5 H11 A1.5,1.5 0 0 1 12.5,10 V13",
        ),
    )

    /** Short repeated trips: a short way out and back. */
    val ShortTrips = DndIcon(
        "short_trips",
        listOf(
            "M5.5,5 A1.5,1.5 0 1 1 2.5,5 A1.5,1.5 0 1 1 5.5,5 Z",
            "M5.5,5 H14.5 A6.5,6.5 0 0 1 14.5,18 H6.5",
            "M10,14.5 L6.5,18 L10,21.5",
        ),
    )

    val all: List<DndIcon>
        get() = listOf(
            Driver, Car, Clock, Moon, Congestion, HarshEvents, Curve, Coffee, OilCan, Gauge, Calendar, Chevron, Check,
            GearShift, Steering, Route, Thermometer, Speaker, FuelPump, Washer, Wrench, TrafficLight, Split, Bars, Compare, Play,
            Spread, Bed, ShortTrips,
        )

    /** A small car seen from the front, 10 units wide, roof at y 6.5, wheels down to y 17.5. */
    private fun frontCar(x: Float): String {
        fun f(v: Float) = "%.2f".format(java.util.Locale.ROOT, v)
        return "M${f(x + 1.5f)},9.5 L${f(x + 2.6f)},5.6 Q${f(x + 2.85f)},4.5 ${f(x + 3.6f)},4.5 H${f(x + 6.4f)} " +
            "Q${f(x + 7.15f)},4.5 ${f(x + 7.4f)},5.6 L${f(x + 8.5f)},9.5 " +
            "M${f(x + 1.2f)},9.5 H${f(x + 8.8f)} Q${f(x + 10)},9.5 ${f(x + 10)},10.9 V16 H${f(x)} V10.9 " +
            "Q${f(x)},9.5 ${f(x + 1.2f)},9.5 Z " +
            "M${f(x + 2)},16 V18.5 M${f(x + 8)},16 V18.5"
    }
}

fun DndIcon.toImageVector(size: PictogramSize): ImageVector =
    ImageVector.Builder(
        name = "$name-${size.name}",
        defaultWidth = size.size,
        defaultHeight = size.size,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                fill = null,
                // A mask colour only: Pictogram tints it with a palette colour.
                stroke = SolidColor(Color.Black),
                strokeLineWidth = size.gridStroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

/** Draws [icon] at a size class, tinted with a palette colour (the text colour by default). */
@Composable
fun Pictogram(
    icon: DndIcon,
    modifier: Modifier = Modifier,
    size: PictogramSize = PictogramSize.Medium,
    tint: Color = DndTheme.palette.text,
) {
    val vector = remember(icon, size) { icon.toImageVector(size) }
    Image(
        painter = rememberVectorPainter(vector),
        contentDescription = null,
        modifier = modifier.size(size.size),
        colorFilter = ColorFilter.tint(tint),
    )
}
