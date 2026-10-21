package app.longwave.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.longwave.R
import app.longwave.data.Reception
import app.longwave.radio.Dial
import app.longwave.radio.Log
import app.longwave.ru
import app.longwave.ui.card.AlbumSlot
import app.longwave.ui.card.QslCard
import app.longwave.ui.components.Heading
import app.longwave.ui.components.TopBar
import app.longwave.ui.theme.LocalPalette
import app.longwave.ui.theme.LocalType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The log of the air: your wave (the stations you fall asleep to fastest), the album of QSL
 * cards, one slot per station, and every night the radio played you to sleep.
 */
@Composable
fun LogScreen(receptions: List<Reception>, onBack: () -> Unit) {
    val p = LocalPalette.current
    val type = LocalType.current
    val ru = LocalContext.current.ru
    var open by remember { mutableStateOf<Reception?>(null) }
    val got = Log.collected(receptions).map { it.id }.toSet()
    val wave = Log.wave(receptions).take(3)
    val locale = if (ru) Locale("ru") else Locale.UK
    val day = DateTimeFormatter.ofPattern("EEE, d MMM", locale)

    Column(Modifier.fillMaxSize().background(p.bg)) {
        TopBar(stringResource(R.string.log), onBack)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)) {
            item {
                Heading(stringResource(R.string.your_wave))
                if (wave.isEmpty()) {
                    Text(stringResource(R.string.your_wave_empty), style = type.body, color = p.muted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        wave.forEachIndexed { i, w ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.surface).padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${i + 1}", style = type.title, color = Color(w.station.hue).copy(alpha = 0.9f).let { androidx.compose.ui.graphics.lerp(it, p.ink, 0.4f) }, modifier = Modifier.width(26.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(w.station.name(ru), style = type.body, color = p.ink)
                                    Text(stringResource(R.string.wave_line, w.fast, w.fast + w.slow), style = type.small, color = p.muted)
                                }
                                Text(w.station.call, style = type.mono, color = p.faint)
                            }
                        }
                    }
                }
                Heading(stringResource(R.string.album) + " · ${got.size}/${Dial.STATIONS.size}")
            }
            // Three slots a row.
            items(Dial.STATIONS.chunked(3)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    row.forEach { s ->
                        val latest = receptions.firstOrNull { it.stationId == s.id }
                        AlbumSlot(
                            s, s.id in got, ru,
                            Modifier.weight(1f).let { m -> if (latest != null) m.clickable { open = latest } else m },
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            item { Heading(stringResource(R.string.nights)) }
            if (receptions.isEmpty()) item { Text(stringResource(R.string.nights_empty), style = type.body, color = p.muted) }
            items(receptions, key = { it.id }) { r ->
                val s = Dial.byId(r.stationId) ?: return@items
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { open = r }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(s.hue)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name(ru), style = type.body, color = p.ink)
                        Text(
                            day.format(Instant.ofEpochMilli(r.startedAt).atZone(ZoneId.systemDefault())) + " · " +
                                pluralStringResource(R.plurals.minutes, r.minutes, r.minutes),
                            style = type.small, color = p.muted,
                        )
                    }
                    Text(
                        when (r.rating) {
                            Reception.FAST -> stringResource(R.string.rated_fast)
                            Reception.SLOW -> stringResource(R.string.rated_slow)
                            Reception.DONT_KNOW -> "—"
                            else -> ""
                        },
                        style = type.small, color = if (r.rating == Reception.FAST) p.accent else p.faint,
                    )
                }
            }
        }
    }
    open?.let { r ->
        Dialog(onDismissRequest = { open = null }) {
            Box(Modifier.clickable { open = null }) { QslCard(r, Modifier.fillMaxWidth().border(0.dp, Color.Transparent)) }
        }
    }
}
