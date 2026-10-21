package app.longwave.ui.card

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The little picture on each station's QSL card, drawn in two inks like a cheap letterpress
 * print: [ink] for the lines, [fill] for the one flat colour. Drawn in a unit square of the
 * current size, so the same emblem works on a card and on a tiny album slot.
 */
fun DrawScope.drawEmblem(id: String, ink: Color, fill: Color) {
    val s = size.minDimension
    fun o(x: Float, y: Float) = Offset(x * s, y * s)
    val line = Stroke(width = s * 0.035f, cap = StrokeCap.Round)
    val thin = Stroke(width = s * 0.022f, cap = StrokeCap.Round)
    fun path(vararg pts: Pair<Float, Float>, close: Boolean = true) = Path().apply {
        moveTo(pts[0].first * s, pts[0].second * s)
        for (i in 1 until pts.size) lineTo(pts[i].first * s, pts[i].second * s)
        if (close) close()
    }
    fun cloud(cx: Float, cy: Float, w: Float) {
        val p = Path().apply {
            addOval(Rect(o(cx - w * 0.5f, cy - w * 0.12f), Size(w * 0.45f * s, w * 0.34f * s)))
            addOval(Rect(o(cx - w * 0.25f, cy - w * 0.3f), Size(w * 0.5f * s, w * 0.48f * s)))
            addOval(Rect(o(cx + w * 0.05f, cy - w * 0.14f), Size(w * 0.45f * s, w * 0.34f * s)))
            addRect(Rect(o(cx - w * 0.36f, cy + w * 0.02f), Size(w * 0.72f * s, w * 0.2f * s)))
        }
        drawPath(p, fill)
        drawLine(ink, o(cx - w * 0.42f, cy + w * 0.22f), o(cx + w * 0.42f, cy + w * 0.22f), strokeWidth = line.width, cap = StrokeCap.Round)
    }
    when (id) {
        "rain" -> {
            cloud(0.5f, 0.38f, 0.7f)
            for (i in 0 until 6) {
                val x = 0.24f + i * 0.1f
                drawLine(ink, o(x, 0.6f + (i % 2) * 0.05f), o(x - 0.05f, 0.75f + (i % 2) * 0.05f), strokeWidth = thin.width, cap = StrokeCap.Round)
            }
        }
        "storm" -> {
            cloud(0.5f, 0.36f, 0.74f)
            drawPath(path(0.52f to 0.56f, 0.4f to 0.72f, 0.5f to 0.72f, 0.42f to 0.9f, 0.62f to 0.66f, 0.52f to 0.66f, 0.58f to 0.56f), ink)
        }
        "light" -> {
            drawPath(path(0.42f to 0.86f, 0.46f to 0.36f, 0.54f to 0.36f, 0.58f to 0.86f), fill)
            drawPath(path(0.42f to 0.86f, 0.46f to 0.36f, 0.54f to 0.36f, 0.58f to 0.86f), ink, style = thin)
            for (y in listOf(0.5f, 0.64f, 0.78f)) drawLine(ink, o(0.44f, y), o(0.56f, y), strokeWidth = thin.width)
            drawRect(ink, o(0.44f, 0.27f), Size(0.12f * s, 0.09f * s))
            drawPath(path(0.43f to 0.27f, 0.5f to 0.19f, 0.57f to 0.27f), ink)
            drawPath(path(0.56f to 0.29f, 0.92f to 0.18f, 0.92f to 0.42f), fill.copy(alpha = 0.6f))
            drawPath(path(0.44f to 0.29f, 0.08f to 0.18f, 0.08f to 0.42f), fill.copy(alpha = 0.35f))
            for (i in 0 until 3) drawArc(ink, 200f, 140f, false, o(0.1f + i * 0.28f, 0.84f), Size(0.24f * s, 0.12f * s), style = thin)
        }
        "owl" -> {
            drawOval(fill, o(0.24f, 0.22f), Size(0.52f * s, 0.64f * s))
            drawPath(path(0.3f to 0.3f, 0.34f to 0.14f, 0.42f to 0.26f), fill)
            drawPath(path(0.7f to 0.3f, 0.66f to 0.14f, 0.58f to 0.26f), fill)
            for (x in listOf(0.39f, 0.61f)) {
                drawCircle(Color.White.copy(alpha = 0.9f), 0.1f * s, o(x, 0.42f))
                drawCircle(ink, 0.1f * s, o(x, 0.42f), style = thin)
                drawCircle(ink, 0.045f * s, o(x, 0.42f))
            }
            drawPath(path(0.46f to 0.52f, 0.54f to 0.52f, 0.5f to 0.6f), ink)
            drawLine(ink, o(0.18f, 0.88f), o(0.82f, 0.88f), strokeWidth = line.width, cap = StrokeCap.Round)
        }
        "forecast" -> {
            drawCircle(fill, 0.34f * s, o(0.5f, 0.5f))
            drawCircle(ink, 0.34f * s, o(0.5f, 0.5f), style = thin)
            drawPath(path(0.5f to 0.12f, 0.56f to 0.5f, 0.5f to 0.88f, 0.44f to 0.5f), ink)
            drawPath(path(0.12f to 0.5f, 0.5f to 0.56f, 0.88f to 0.5f, 0.5f to 0.44f), ink.copy(alpha = 0.55f))
            drawCircle(fill, 0.04f * s, o(0.5f, 0.5f))
        }
        "train" -> {
            drawRoundRect(fill, o(0.12f, 0.3f), Size(0.76f * s, 0.42f * s), androidx.compose.ui.geometry.CornerRadius(0.06f * s))
            drawRoundRect(ink, o(0.12f, 0.3f), Size(0.76f * s, 0.42f * s), androidx.compose.ui.geometry.CornerRadius(0.06f * s), style = thin)
            for (i in 0 until 3) drawRect(Color(0xFFF6D98A), o(0.18f + i * 0.24f, 0.38f), Size(0.16f * s, 0.14f * s))
            for (x in listOf(0.24f, 0.38f, 0.62f, 0.76f)) drawCircle(ink, 0.05f * s, o(x, 0.76f))
            drawLine(ink, o(0.04f, 0.82f), o(0.96f, 0.82f), strokeWidth = thin.width)
            val moon = Path().apply { addOval(Rect(o(0.78f, 0.16f), 0.07f * s)) }
            val bite = Path().apply { addOval(Rect(o(0.81f, 0.14f), 0.06f * s)) }
            drawPath(Path.combine(PathOperation.Difference, moon, bite), ink)
        }
        "rollcall" -> {
            drawRoundRect(fill, o(0.24f, 0.16f), Size(0.52f * s, 0.7f * s), androidx.compose.ui.geometry.CornerRadius(0.04f * s))
            drawRoundRect(ink, o(0.24f, 0.16f), Size(0.52f * s, 0.7f * s), androidx.compose.ui.geometry.CornerRadius(0.04f * s), style = thin)
            drawRect(ink, o(0.4f, 0.12f), Size(0.2f * s, 0.08f * s))
            for (i in 0 until 4) {
                val y = 0.32f + i * 0.13f
                drawPath(path(0.3f to y, 0.33f to y + 0.03f, 0.38f to y - 0.03f, close = false), ink, style = thin)
                drawLine(ink, o(0.43f, y), o(0.68f, y), strokeWidth = thin.width, cap = StrokeCap.Round)
            }
        }
        "hearth" -> {
            drawRoundRect(fill, o(0.2f, 0.28f), Size(0.6f * s, 0.6f * s), androidx.compose.ui.geometry.CornerRadius(0.05f * s))
            drawRoundRect(ink, o(0.2f, 0.28f), Size(0.6f * s, 0.6f * s), androidx.compose.ui.geometry.CornerRadius(0.05f * s), style = thin)
            drawRect(ink, o(0.6f, 0.08f), Size(0.1f * s, 0.2f * s))
            drawArc(ink, 180f, 180f, true, o(0.32f, 0.48f), Size(0.36f * s, 0.3f * s))
            drawRect(ink, o(0.32f, 0.63f), Size(0.36f * s, 0.17f * s))
            drawPath(path(0.5f to 0.5f, 0.6f to 0.7f, 0.5f to 0.78f, 0.4f to 0.7f), Color(0xFFF2A44A))
        }
        "lottery" -> {
            // A lotto keg with a word-sized number.
            drawPath(Path().apply {
                moveTo(0.32f * s, 0.18f * s)
                quadraticTo(0.22f * s, 0.5f * s, 0.32f * s, 0.82f * s)
                lineTo(0.68f * s, 0.82f * s)
                quadraticTo(0.78f * s, 0.5f * s, 0.68f * s, 0.18f * s)
                close()
            }, fill)
            drawOval(ink, o(0.32f, 0.12f), Size(0.36f * s, 0.12f * s), style = thin)
            drawOval(ink, o(0.32f, 0.76f), Size(0.36f * s, 0.12f * s), style = thin)
            drawCircle(Color.White.copy(alpha = 0.85f), 0.13f * s, o(0.5f, 0.5f))
            drawLine(ink, o(0.44f, 0.5f), o(0.56f, 0.5f), strokeWidth = line.width, cap = StrokeCap.Round)
            drawLine(ink, o(0.46f, 0.44f), o(0.54f, 0.44f), strokeWidth = thin.width, cap = StrokeCap.Round)
            drawLine(ink, o(0.46f, 0.56f), o(0.54f, 0.56f), strokeWidth = thin.width, cap = StrokeCap.Round)
        }
        "crickets" -> {
            val moon = Path().apply { addOval(Rect(o(0.68f, 0.3f), 0.16f * s)) }
            val bite = Path().apply { addOval(Rect(o(0.75f, 0.25f), 0.14f * s)) }
            drawPath(Path.combine(PathOperation.Difference, moon, bite), ink)
            for (i in 0 until 9) {
                val x = 0.12f + i * 0.095f
                val h = 0.2f + ((i * 37) % 10) / 40f
                drawLine(ink, o(x, 0.9f), o(x + 0.04f, 0.9f - h), strokeWidth = thin.width, cap = StrokeCap.Round)
            }
            for ((x, y) in listOf(0.2f to 0.24f, 0.36f to 0.14f, 0.46f to 0.32f)) drawCircle(ink, 0.012f * s, o(x, y))
        }
        "metronome" -> {
            drawPath(path(0.38f to 0.12f, 0.62f to 0.12f, 0.78f to 0.88f, 0.22f to 0.88f), fill)
            drawPath(path(0.38f to 0.12f, 0.62f to 0.12f, 0.78f to 0.88f, 0.22f to 0.88f), ink, style = thin)
            rotate(18f, o(0.5f, 0.74f)) {
                drawLine(ink, o(0.5f, 0.74f), o(0.5f, 0.2f), strokeWidth = line.width, cap = StrokeCap.Round)
                drawRect(ink, o(0.45f, 0.36f), Size(0.1f * s, 0.07f * s))
            }
            drawLine(ink, o(0.28f, 0.74f), o(0.72f, 0.74f), strokeWidth = thin.width)
        }
        "harbour" -> {
            drawCircle(ink, 0.07f * s, o(0.5f, 0.18f), style = line)
            drawLine(ink, o(0.5f, 0.25f), o(0.5f, 0.84f), strokeWidth = line.width, cap = StrokeCap.Round)
            drawLine(ink, o(0.34f, 0.36f), o(0.66f, 0.36f), strokeWidth = line.width, cap = StrokeCap.Round)
            drawArc(ink, 20f, 140f, false, o(0.2f, 0.42f), Size(0.6f * s, 0.44f * s), style = line)
            drawPath(path(0.18f to 0.62f, 0.25f to 0.72f, 0.12f to 0.7f), ink)
            drawPath(path(0.82f to 0.62f, 0.75f to 0.72f, 0.88f to 0.7f), ink)
            drawCircle(fill.copy(alpha = 0.5f), 0.4f * s, o(0.5f, 0.52f))
        }
        "stores" -> {
            // A storm lantern.
            drawArc(ink, 180f, 180f, false, o(0.38f, 0.08f), Size(0.24f * s, 0.2f * s), style = thin)
            drawRect(ink, o(0.32f, 0.2f), Size(0.36f * s, 0.07f * s))
            drawRoundRect(fill, o(0.34f, 0.28f), Size(0.32f * s, 0.44f * s), androidx.compose.ui.geometry.CornerRadius(0.12f * s))
            drawRoundRect(ink, o(0.34f, 0.28f), Size(0.32f * s, 0.44f * s), androidx.compose.ui.geometry.CornerRadius(0.12f * s), style = thin)
            drawPath(path(0.5f to 0.38f, 0.56f to 0.52f, 0.5f to 0.6f, 0.44f to 0.52f), Color(0xFFF2B04A))
            drawRect(ink, o(0.3f, 0.72f), Size(0.4f * s, 0.12f * s))
        }
        "moon" -> {
            drawCircle(fill, 0.3f * s, o(0.46f, 0.48f))
            drawCircle(ink, 0.3f * s, o(0.46f, 0.48f), style = thin)
            for ((x, y, r) in listOf(Triple(0.38f, 0.4f, 0.06f), Triple(0.55f, 0.58f, 0.045f), Triple(0.5f, 0.34f, 0.03f))) drawCircle(ink.copy(alpha = 0.35f), r * s, o(x, y))
            drawLine(ink, o(0.82f, 0.2f), o(0.82f, 0.52f), strokeWidth = thin.width)
            drawOval(ink, o(0.74f, 0.48f), Size(0.09f * s, 0.07f * s))
            drawLine(ink, o(0.82f, 0.2f), o(0.9f, 0.26f), strokeWidth = thin.width, cap = StrokeCap.Round)
        }
        "river" -> {
            drawPath(path(0.3f to 0.12f, 0.46f to 0.5f, 0.14f to 0.5f), fill)
            drawPath(path(0.3f to 0.3f, 0.5f to 0.66f, 0.1f to 0.66f), fill)
            drawLine(ink, o(0.3f, 0.66f), o(0.3f, 0.74f), strokeWidth = line.width)
            drawPath(path(0.7f to 0.2f, 0.84f to 0.5f, 0.56f to 0.5f), fill.copy(alpha = 0.7f))
            for (i in 0 until 3) {
                val y = 0.76f + i * 0.07f
                val p = Path().apply {
                    moveTo(0.1f * s, y * s)
                    var x = 0.1f
                    while (x < 0.9f) {
                        quadraticTo((x + 0.05f) * s, (y - 0.03f) * s, (x + 0.1f) * s, y * s)
                        x += 0.1f
                    }
                }
                drawPath(p, ink, style = thin)
            }
        }
        "countdown" -> {
            // Counting sheep, of course.
            for ((x, y, r) in listOf(Triple(0.38f, 0.48f, 0.12f), Triple(0.52f, 0.42f, 0.13f), Triple(0.62f, 0.52f, 0.12f), Triple(0.46f, 0.58f, 0.12f), Triple(0.34f, 0.58f, 0.1f))) {
                drawCircle(Color.White.copy(alpha = 0.9f), r * s, o(x, y))
                drawCircle(ink, r * s, o(x, y), style = thin)
            }
            drawOval(ink, o(0.66f, 0.36f), Size(0.16f * s, 0.12f * s))
            for (x in listOf(0.38f, 0.46f, 0.56f, 0.62f)) drawLine(ink, o(x, 0.66f), o(x, 0.8f), strokeWidth = line.width, cap = StrokeCap.Round)
            drawLine(ink, o(0.12f, 0.84f), o(0.88f, 0.84f), strokeWidth = thin.width)
            drawCircle(fill, 0.08f * s, o(0.2f, 0.2f))
        }
        "pass" -> {
            drawPath(path(0.06f to 0.84f, 0.34f to 0.3f, 0.5f to 0.56f, 0.66f to 0.22f, 0.94f to 0.84f), fill)
            drawPath(path(0.06f to 0.84f, 0.34f to 0.3f, 0.5f to 0.56f, 0.66f to 0.22f, 0.94f to 0.84f), ink, style = thin)
            drawPath(path(0.6f to 0.34f, 0.66f to 0.22f, 0.72f to 0.34f, 0.66f to 0.31f), Color.White.copy(alpha = 0.85f))
            for ((y, x0) in listOf(0.14f to 0.1f, 0.22f to 0.2f)) {
                drawLine(ink, o(x0, y), o(x0 + 0.3f, y), strokeWidth = thin.width, cap = StrokeCap.Round)
                drawArc(ink, -90f, 180f, false, o(x0 + 0.26f, y - 0.06f), Size(0.08f * s, 0.06f * s), style = thin)
            }
        }
        "deep" -> {
            for (i in 1..4) drawArc(ink.copy(alpha = 1f - i * 0.18f), -60f, 120f, false, o(0.5f - i * 0.1f, 0.5f - i * 0.1f), Size(i * 0.2f * s, i * 0.2f * s), style = thin)
            for (i in 1..4) drawArc(ink.copy(alpha = 1f - i * 0.18f), 120f, 120f, false, o(0.5f - i * 0.1f, 0.5f - i * 0.1f), Size(i * 0.2f * s, i * 0.2f * s), style = thin)
            drawCircle(fill, 0.07f * s, o(0.5f, 0.5f))
            drawCircle(ink, 0.07f * s, o(0.5f, 0.5f), style = thin)
            for (i in 0 until 5) {
                val a = i * 2 * PI / 5 - PI / 2
                drawCircle(ink, 0.012f * s, o(0.5f + cos(a).toFloat() * 0.42f, 0.5f + sin(a).toFloat() * 0.42f))
            }
        }
        else -> drawCircle(fill, 0.3f * s, o(0.5f, 0.5f))
    }
}
