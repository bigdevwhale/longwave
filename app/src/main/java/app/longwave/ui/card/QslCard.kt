package app.longwave.ui.card

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.longwave.R
import app.longwave.data.Reception
import app.longwave.radio.Dial
import app.longwave.radio.Station
import app.longwave.radio.khzLabel
import app.longwave.ru
import app.longwave.ui.theme.Hand
import app.longwave.ui.theme.LocalPalette
import app.longwave.ui.theme.Mono
import app.longwave.ui.theme.Prata
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

/**
 * A QSL card: the postcard a station sends to confirm it was heard. This one confirms a night —
 * which station played you to sleep, when, for how long — addressed to your call sign.
 */
@Composable
fun QslCard(r: Reception, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val ru = LocalContext.current.ru
    val s: Station = Dial.byId(r.stationId) ?: Dial.STATIONS.first()
    val hue = Color(s.hue)
    val zone = ZoneId.systemDefault()
    val locale = if (ru) Locale("ru") else Locale.UK
    val start = Instant.ofEpochMilli(r.startedAt).atZone(zone)
    val end = Instant.ofEpochMilli(r.endedAt).atZone(zone)
    val hm = DateTimeFormatter.ofPattern("HH:mm", locale)
    val ink = p.paperInk
    val label = TextStyle(fontFamily = Mono, fontSize = 8.5.sp, letterSpacing = 0.8.sp, color = ink.copy(alpha = 0.55f))
    val value = TextStyle(fontFamily = Mono, fontSize = 13.sp, color = ink)
    Box(
        modifier
            .aspectRatio(1.75f)
            .clip(RoundedCornerShape(6.dp))
            .background(p.paper)
            .border(1.dp, hue.copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
    ) {
        // Card stock: a few specks and a tinted band down the left edge.
        Canvas(Modifier.matchParentSize()) {
            val rnd = Random(r.id + 7)
            repeat(70) { drawCircle(ink.copy(alpha = 0.05f), radius = 0.6f + rnd.nextFloat() * 1.2f, center = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)) }
            drawRect(hue.copy(alpha = 0.85f), size = androidx.compose.ui.geometry.Size(7.dp.toPx(), size.height))
            if (s.rare) drawRect(Color(0xFFC9A45C), topLeft = Offset(7.dp.toPx(), 0f), size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height))
        }
        Column(Modifier.padding(start = 20.dp, end = 14.dp, top = 12.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(s.call, style = TextStyle(fontFamily = Prata, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = 3.sp, color = hue))
                    Text(
                        "${khzLabel(s.khz, ru)} · ${if (ru) "ДВ" else "LW"} · ${stringResource(R.string.app_name).uppercase()}",
                        style = label.copy(fontSize = 9.sp),
                    )
                    Text(s.name(ru), style = TextStyle(fontFamily = Prata, fontSize = 15.sp, color = ink), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
                Canvas(Modifier.size(70.dp)) {
                    drawCircle(hue.copy(alpha = 0.16f))
                    drawCircle(hue.copy(alpha = 0.5f), style = Stroke(1.dp.toPx()))
                    drawEmblem(s.id, ink, hue.copy(alpha = 0.75f))
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(stringResource(R.string.qsl_to), style = label, modifier = Modifier.padding(bottom = 6.dp))
                Spacer(Modifier.width(8.dp))
                Text(r.callsign.ifBlank { "—" }, style = TextStyle(fontFamily = Hand, fontSize = 28.sp, lineHeight = 28.sp, color = Color(0xFF23376B)))
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Field(stringResource(R.string.qsl_date), DateTimeFormatter.ofPattern("d MMM yy", locale).format(start).uppercase(locale), label, value)
                Field(stringResource(R.string.qsl_time), "${hm.format(start)}–${hm.format(end)}", label, value)
                Field(stringResource(R.string.qsl_minutes), r.minutes.toString(), label, value)
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.qsl_note), style = TextStyle(fontFamily = Hand, fontSize = 17.sp, lineHeight = 18.sp, color = Color(0xFF23376B)), maxLines = 2)
        }
        // The station's postmark across the corner.
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 12.dp).rotate(-14f)) {
            Canvas(Modifier.size(58.dp)) {
                val c = hue.copy(alpha = 0.55f)
                drawCircle(c, style = Stroke(1.6.dp.toPx()))
                drawCircle(c, radius = size.minDimension * 0.38f, style = Stroke(0.8.dp.toPx()))
            }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.call, style = TextStyle(fontFamily = Mono, fontSize = 9.sp, color = hue.copy(alpha = 0.7f)))
                Text(DateTimeFormatter.ofPattern("dd.MM", locale).format(end), style = TextStyle(fontFamily = Mono, fontSize = 10.sp, color = hue.copy(alpha = 0.7f)))
            }
        }
    }
}

@Composable
private fun Field(name: String, v: String, label: TextStyle, value: TextStyle) {
    Column {
        Text(name, style = label)
        Text(v, style = value)
    }
}

/** A station's slot in the album: its emblem if it has sent a card, a faint question otherwise. */
@Composable
fun AlbumSlot(s: Station, got: Boolean, ru: Boolean, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val hue = Color(s.hue)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.55f)
                .clip(RoundedCornerShape(4.dp))
                .background(if (got) p.paper else p.surface)
                .border(1.dp, if (got) hue.copy(alpha = 0.6f) else p.line, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (got) {
                Canvas(Modifier.size(40.dp)) { drawEmblem(s.id, p.paperInk, hue.copy(alpha = 0.75f)) }
                Text(s.call, style = TextStyle(fontFamily = Mono, fontSize = 8.sp, color = hue), modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
            } else {
                Text(if (s.rare) "?" else s.call, style = TextStyle(fontFamily = Prata, fontSize = 16.sp, color = p.faint))
            }
        }
        Text(
            if (got || !s.rare) s.name(ru) else "· · ·",
            style = TextStyle(fontSize = 10.sp, color = if (got) p.muted else p.faint),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}
