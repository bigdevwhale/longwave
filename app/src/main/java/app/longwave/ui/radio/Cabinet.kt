package app.longwave.ui.radio

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.longwave.radio.Dial
import app.longwave.radio.Sky
import app.longwave.ui.theme.LocalPalette
import app.longwave.ui.theme.Mono
import app.longwave.ui.theme.Palette
import app.longwave.ui.theme.Prata
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** Dial spacing: how many dp one kilohertz takes on the glass. */
const val DP_PER_KHZ = 12f

/** Walnut veneer: a warm gradient with a few long, wavering grain lines. */
fun DrawScope.drawWood(p: Palette, corner: Float) {
    drawRoundRect(
        Brush.verticalGradient(listOf(p.woodLight, p.wood, p.woodDark)),
        cornerRadius = CornerRadius(corner),
    )
    val rnd = Random(11)
    clipRect {
        repeat(26) {
            val y0 = rnd.nextFloat() * size.height
            val amp = 4f + rnd.nextFloat() * 14f
            val len = 0.6f + rnd.nextFloat() * 1.6f
            val path = Path()
            var x = 0f
            path.moveTo(0f, y0)
            while (x <= size.width) {
                path.lineTo(x, y0 + sin(x / size.width * PI.toFloat() * 2 * len + it) * amp)
                x += 12f
            }
            drawPath(path, Color.Black.copy(alpha = 0.06f + rnd.nextFloat() * 0.07f), style = Stroke(width = 1f + rnd.nextFloat() * 2.5f))
        }
    }
    // A soft sheen along the top edge, the lamp in the room catching the varnish.
    drawRoundRect(
        Brush.verticalGradient(0f to Color.White.copy(alpha = 0.10f), 0.12f to Color.Transparent),
        cornerRadius = CornerRadius(corner),
    )
}

/** The speaker cloth: a dark weave behind the brass name plate. */
fun DrawScope.drawCloth(p: Palette, corner: Float) {
    drawRoundRect(p.cloth, cornerRadius = CornerRadius(corner))
    clipRect {
        val step = 5.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            drawLine(p.clothLine, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 1.2f)
            drawLine(p.clothLine.copy(alpha = 0.5f), Offset(x, 0f), Offset(x + size.height, size.height), strokeWidth = 1f)
            x += step
        }
    }
    // The speaker cone shows faintly through the cloth.
    val cone = minOf(size.width, size.height) * 0.42f
    for (k in 0 until 4) drawCircle(Color.Black.copy(alpha = 0.16f), radius = cone * (1f - k * 0.22f), style = Stroke(cone * 0.07f))
    drawRoundRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)), radius = size.maxDimension * 0.7f), cornerRadius = CornerRadius(corner))
    drawRoundRect(p.woodDark, cornerRadius = CornerRadius(corner), style = Stroke(3.dp.toPx()))
}

/**
 * The glass of the dial: the long-wave scale with its stations, lit amber from behind when the
 * set is warm ([glow] 0..1), and the red needle, which sits still in the middle while the scale
 * slides under it. [needle] moves the needle off the middle, in kHz, as the radio drifts away
 * at the end of the night.
 */
@Composable
fun DialGlass(
    khz: Float,
    needle: Float,
    glow: Float,
    sky: Sky,
    found: Set<String>,
    ru: Boolean,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val nameStyle = remember { TextStyle(fontFamily = Prata, fontSize = 10.5.sp, lineHeight = 12.sp, textAlign = TextAlign.Center) }
    val numStyle = remember { TextStyle(fontFamily = Mono, fontSize = 10.sp) }
    Canvas(modifier) {
        val corner = 10.dp.toPx()
        val glass = lerp(p.glassOff, p.glassLit, glow)
        val edge = lerp(p.glassOff, p.glassDim, glow)
        drawRoundRect(Brush.verticalGradient(listOf(edge, glass, edge)), cornerRadius = CornerRadius(corner))
        drawRoundRect(
            Brush.radialGradient(listOf(p.glassLit.copy(alpha = 0.4f * glow), Color.Transparent), center = center, radius = size.width * 0.55f),
            cornerRadius = CornerRadius(corner),
        )
        val ppk = DP_PER_KHZ.dp.toPx()
        val cx = size.width / 2
        fun x(k: Float) = cx + (k - khz) * ppk
        // Paint on unlit glass shows dimly ivory; lit from behind it reads dark.
        fun ink(c: Color, alpha: Float = 1f) = lerp(Color(0xFF8D7A5C), c, glow).copy(alpha = alpha)
        val base = size.height * 0.70f
        clipRect(left = 4.dp.toPx(), right = size.width - 4.dp.toPx()) {
            val lo = floor(khz - cx / ppk).toInt() - 1
            val hi = ceil(khz + cx / ppk).toInt() + 1
            for (k in lo..hi) {
                if (k < Dial.MIN || k > Dial.MAX) continue
                val len = when {
                    k % 10 == 0 -> 14.dp.toPx()
                    k % 5 == 0 -> 9.dp.toPx()
                    else -> 5.dp.toPx()
                }
                val xx = x(k.toFloat())
                drawLine(ink(p.print, 0.85f), Offset(xx, base - len), Offset(xx, base), strokeWidth = if (k % 10 == 0) 1.6f else 1f)
                if (k % 10 == 0) {
                    val t = measurer.measure(k.toString(), numStyle.copy(color = ink(p.print)))
                    drawText(t, topLeft = Offset(xx - t.size.width / 2, base + 3.dp.toPx()))
                }
            }
            drawLine(ink(p.print, 0.7f), Offset(x(Dial.MIN), base), Offset(x(Dial.MAX), base), strokeWidth = 1.4f)
            Dial.STATIONS.forEachIndexed { i, s ->
                val xx = x(s.khz)
                if (xx < -80.dp.toPx() || xx > size.width + 80.dp.toPx()) return@forEachIndexed
                val onAir = sky.onAir(s)
                val known = !s.rare || s.id in found
                if (s.rare && !known && !onAir) return@forEachIndexed
                val colour = when {
                    s.rare -> p.printRare
                    s.programme != null -> p.printProgramme
                    else -> p.print
                }
                val alpha = if (onAir) 1f else 0.4f
                val label = if (known) s.name(ru) else "?"
                val t = measurer.measure(
                    label,
                    nameStyle.copy(color = ink(colour, alpha), fontWeight = if (s.programme != null) FontWeight.Normal else FontWeight.Normal),
                    constraints = Constraints(maxWidth = 92.dp.toPx().toInt()),
                )
                val rowTop = if (i % 2 == 0) 6.dp.toPx() else 31.dp.toPx()
                drawText(t, topLeft = Offset(xx - t.size.width / 2, rowTop))
                drawLine(ink(colour, alpha * 0.6f), Offset(xx, rowTop + t.size.height + 2.dp.toPx()), Offset(xx, base - 16.dp.toPx()), strokeWidth = 1f)
                drawCircle(ink(colour, alpha), radius = 2.6.dp.toPx(), center = Offset(xx, base - 16.dp.toPx()))
            }
        }
        // The needle, with a faint red glow of its own.
        val nx = cx + needle * ppk
        drawLine(p.needle.copy(alpha = 0.25f * (0.4f + glow)), Offset(nx, 4.dp.toPx()), Offset(nx, size.height - 4.dp.toPx()), strokeWidth = 6.dp.toPx())
        drawLine(lerp(Color(0xFF7A2A20), p.needle, 0.4f + 0.6f * glow), Offset(nx, 4.dp.toPx()), Offset(nx, size.height - 4.dp.toPx()), strokeWidth = 2.dp.toPx())
        // A reflection across the glass, then the brass bezel.
        drawRoundRect(
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent), start = Offset(0f, 0f), end = Offset(size.width * 0.4f, size.height)),
            cornerRadius = CornerRadius(corner),
        )
        drawRoundRect(p.brassDark, cornerRadius = CornerRadius(corner), style = Stroke(4.dp.toPx()))
        drawRoundRect(p.brass.copy(alpha = 0.7f), cornerRadius = CornerRadius(corner), style = Stroke(1.5.dp.toPx()))
    }
}

/**
 * The magic eye, a green tuning valve: a glowing fan with a dark wedge that narrows as a
 * station comes clear, flickering faintly with the sound.
 */
fun DrawScope.drawEye(p: Palette, signal: Float, level: Float, glow: Float) {
    val r = size.minDimension / 2
    drawCircle(Brush.radialGradient(listOf(p.brass, p.brassDark), radius = r * 1.1f), radius = r)
    drawCircle(Color(0xFF050807), radius = r * 0.84f)
    drawCircle(p.eyeDark, radius = r * 0.78f)
    if (glow > 0.01f) {
        val half = 8f + 52f * (1f - signal)
        val bright = glow * (0.78f + 0.22f * level.coerceIn(0f, 1f))
        val inner = r * 0.78f
        drawArc(
            Brush.radialGradient(listOf(p.eye.copy(alpha = bright), p.eye.copy(alpha = bright * 0.55f), Color(0xFF1F6B45).copy(alpha = bright * 0.5f)), center = center, radius = inner),
            startAngle = -90f + half, sweepAngle = 360f - 2 * half, useCenter = true,
            topLeft = Offset(center.x - inner, center.y - inner), size = Size(inner * 2, inner * 2),
        )
        // The overlapping edges of the fan glow brightest.
        for (side in listOf(-1f, 1f)) {
            val a = Math.toRadians((-90f + side * half).toDouble())
            drawLine(p.eye.copy(alpha = bright), center, Offset(center.x + cos(a).toFloat() * inner, center.y + sin(a).toFloat() * inner), strokeWidth = 1.5.dp.toPx())
        }
    }
    drawCircle(Brush.radialGradient(listOf(Color(0xFF3A3A36), Color(0xFF111111)), radius = r * 0.3f), radius = r * 0.28f)
    drawCircle(Color.White.copy(alpha = 0.06f), radius = r * 0.78f, center = center + Offset(-r * 0.15f, -r * 0.2f))
}

/** A knurled bakelite knob with a brass cap and a pointer notch at [angle] degrees. */
fun DrawScope.drawKnob(p: Palette, angle: Float) {
    val r = size.minDimension / 2
    drawCircle(Color.Black.copy(alpha = 0.45f), radius = r, center = center + Offset(0f, r * 0.08f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFF3B302A), Color(0xFF15100D)), center = center + Offset(-r * 0.3f, -r * 0.3f), radius = r * 1.4f), radius = r * 0.96f)
    rotate(angle) {
        for (i in 0 until 48) {
            val a = i * 2 * PI / 48
            val o = Offset(center.x + cos(a).toFloat() * r * 0.96f, center.y + sin(a).toFloat() * r * 0.96f)
            val n = Offset(center.x + cos(a).toFloat() * r * 0.84f, center.y + sin(a).toFloat() * r * 0.84f)
            drawLine(Color.Black.copy(alpha = 0.5f), n, o, strokeWidth = 1.5f)
        }
        drawCircle(Brush.radialGradient(listOf(p.brass, p.brassDark), center = center + Offset(-r * 0.15f, -r * 0.15f), radius = r * 0.7f), radius = r * 0.56f)
        drawLine(p.woodDark, Offset(center.x, center.y - r * 0.5f), Offset(center.x, center.y - r * 0.2f), strokeWidth = 3.dp.toPx())
    }
}

/** Centres text on a point, for the few words painted on the cabinet. */
fun DrawScope.drawCentred(m: TextMeasurer, text: String, style: TextStyle, at: Offset) {
    val t = m.measure(text, style)
    drawText(t, topLeft = Offset(at.x - t.size.width / 2, at.y - t.size.height / 2))
}
