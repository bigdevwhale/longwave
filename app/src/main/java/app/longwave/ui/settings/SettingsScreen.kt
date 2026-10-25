package app.longwave.ui.settings

import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.longwave.BuildConfig
import app.longwave.R
import app.longwave.audio.Engine
import app.longwave.container
import app.longwave.data.Air
import app.longwave.data.RadioSettings
import app.longwave.ui.components.CommunityLinks
import app.longwave.ui.components.Heading
import app.longwave.ui.components.TopBar
import app.longwave.ui.theme.LocalPalette
import app.longwave.ui.theme.LocalType
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(settings: RadioSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val p = LocalPalette.current
    val type = LocalType.current
    var call by remember { mutableStateOf(settings.callsign) }
    LaunchedEffect(call) {
        delay(400)
        if (call.isNotBlank()) c.write { this.settings.setCallsign(call) }
    }

    Column(Modifier.fillMaxSize().background(p.bg)) {
        TopBar(stringResource(R.string.settings), onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Heading(stringResource(R.string.callsign))
            OutlinedTextField(
                value = call,
                onValueChange = { call = it.take(16) },
                singleLine = true,
                textStyle = type.body.copy(color = p.ink),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p.accent, unfocusedBorderColor = p.line, cursorColor = p.accent),
            )
            Text(stringResource(R.string.callsign_sub), style = type.small, color = p.faint, modifier = Modifier.padding(top = 6.dp))

            Heading(stringResource(R.string.sign_off))
            Choices(listOf(1, 3, 5, 10).map { it to "$it" }, settings.signOff) { v -> c.write { this.settings.setSignOff(v) } }
            Text(stringResource(R.string.sign_off_sub), style = type.small, color = p.faint, modifier = Modifier.padding(top = 6.dp))

            Heading(stringResource(R.string.air))
            Choices(listOf(Air.QUIET to stringResource(R.string.air_quiet), Air.SOFT to stringResource(R.string.air_soft), Air.FULL to stringResource(R.string.air_full)), settings.air) { v ->
                Engine.staticLevel = v.level
                c.write { this.settings.setAir(v) }
            }

            Heading(stringResource(R.string.on_air))
            Toggle(stringResource(R.string.fading), stringResource(R.string.fading_sub), settings.fading) { v ->
                Engine.fading = v
                c.write { this.settings.setFading(v) }
            }
            Toggle(stringResource(R.string.good_night), stringResource(R.string.good_night_sub), settings.goodNight) { v -> c.write { this.settings.setGoodNight(v) } }
            Toggle(stringResource(R.string.haptics), stringResource(R.string.haptics_sub), settings.haptics) { v -> c.write { this.settings.setHaptics(v) } }

            Heading(stringResource(R.string.language))
            val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            val lang = when {
                current.startsWith("ru") -> "ru"
                current.startsWith("en") -> "en"
                else -> ""
            }
            Choices(listOf("" to stringResource(R.string.system), "ru" to stringResource(R.string.lang_ru), "en" to stringResource(R.string.lang_en)), lang) { tag ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }

            Heading(stringResource(R.string.community))
            CommunityLinks()

            Heading(stringResource(R.string.about))
            Text(stringResource(R.string.about_text), style = type.body, color = p.muted)
            Text(stringResource(R.string.credits), style = type.small, color = p.faint, modifier = Modifier.padding(top = 10.dp))
            Text("v${BuildConfig.VERSION_NAME}", style = type.label, color = p.faint, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun <T> Choices(options: List<Pair<T, String>>, selected: T, onPick: (T) -> Unit) {
    val p = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { (v, label) ->
            val on = v == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (on) p.accent.copy(alpha = 0.18f) else p.surface)
                    .border(1.dp, if (on) p.accent.copy(alpha = 0.7f) else p.line, RoundedCornerShape(20.dp))
                    .clickable { onPick(v) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = LocalType.current.small, color = if (on) p.ink else p.muted) }
        }
    }
}

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    val type = LocalType.current
    Row(Modifier.fillMaxWidth().clickable { onChange(!on) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = type.body, color = p.ink)
            Text(sub, style = type.small, color = p.faint)
        }
        Switch(
            checked = on, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = p.woodDark, checkedTrackColor = p.accent, uncheckedThumbColor = p.muted, uncheckedTrackColor = p.surface, uncheckedBorderColor = p.line),
        )
    }
}
