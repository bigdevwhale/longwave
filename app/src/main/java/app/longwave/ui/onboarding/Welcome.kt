package app.longwave.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.longwave.R
import app.longwave.container
import app.longwave.ru
import app.longwave.ui.components.Pill
import app.longwave.ui.radio.drawEye
import app.longwave.ui.theme.LocalPalette
import app.longwave.ui.theme.LocalType

private val ANIMALS_EN = listOf("Fox", "Owl", "Hare", "Badger", "Otter", "Wren", "Moth", "Hedgehog")
private val ANIMALS_RU = listOf("Лис", "Сова", "Заяц", "Барсук", "Выдра", "Ёж", "Сыч", "Крот")

/** A made-up call sign to start with: an animal and a number, like the radio hams use. */
fun randomCallsign(ru: Boolean): String = "${(if (ru) ANIMALS_RU else ANIMALS_EN).random()}-${(2..99).random()}"

@Composable
fun Welcome() {
    val p = LocalPalette.current
    val type = LocalType.current
    val context = LocalContext.current
    val ru = context.ru
    var call by rememberSaveable { mutableStateOf(randomCallsign(ru)) }
    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Canvas(Modifier.size(72.dp).align(Alignment.CenterHorizontally)) {
            drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0x40E8B765), Color.Transparent), radius = size.minDimension), radius = size.minDimension)
            drawEye(p, 0.85f, 0.6f, 1f)
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.app_name), style = type.display, color = p.ink)
        Text(stringResource(R.string.welcome_lead), style = type.title.copy(fontSize = type.title.fontSize * 0.85f), color = p.muted, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(24.dp))
        listOf(R.string.welcome_1, R.string.welcome_2, R.string.welcome_3, R.string.welcome_4).forEach { id ->
            Row(Modifier.padding(vertical = 7.dp)) {
                Text("·", style = type.body, color = p.accent, modifier = Modifier.width(16.dp))
                Text(stringResource(id), style = type.body, color = p.ink)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.callsign).uppercase(), style = type.label, color = p.faint)
        OutlinedTextField(
            value = call,
            onValueChange = { call = it.take(16) },
            singleLine = true,
            textStyle = type.body.copy(color = p.ink),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p.accent, unfocusedBorderColor = p.line, cursorColor = p.accent),
        )
        Text(stringResource(R.string.callsign_sub), style = type.small, color = p.faint, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(stringResource(R.string.welcome_go), Modifier.fillMaxWidth(), strong = true) {
                val name = call.ifBlank { randomCallsign(ru) }
                context.container.write { settings.welcome(name) }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}
