package app.longwave.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.longwave.container
import app.longwave.data.RadioSettings
import app.longwave.data.Reception
import app.longwave.radio.Log
import app.longwave.ui.card.MorningScreen
import app.longwave.ui.log.LogScreen
import app.longwave.ui.onboarding.Welcome
import app.longwave.ui.radio.RadioScreen
import app.longwave.ui.settings.SettingsScreen

/** The radio is home; the log and the settings are a step away and straight back. */
private enum class Place { RADIO, LOG, SETTINGS }

@Composable
fun Root(settings: RadioSettings) {
    if (!settings.onboarded) {
        Welcome()
        return
    }
    val c = LocalContext.current.container
    val receptions: List<Reception>? by c.db.dao().receptions().collectAsState(initial = null)
    var place by rememberSaveable { mutableStateOf(Place.RADIO) }
    // Asked once per opening: a card put aside stays in the log, unasked.
    var putAside by rememberSaveable { mutableStateOf(0L) }
    BackHandler(enabled = place != Place.RADIO) { place = Place.RADIO }

    val all = receptions ?: return
    val card = Log.pending(all, System.currentTimeMillis())?.takeIf { it.id != putAside }
    if (card != null && place == Place.RADIO) {
        BackHandler { putAside = card.id }
        MorningScreen(card, onDone = { putAside = card.id })
        return
    }
    Crossfade(targetState = place, label = "place") { here ->
        when (here) {
            Place.RADIO -> RadioScreen(
                settings,
                hasCard = Log.pending(all, System.currentTimeMillis()) != null,
                onLog = { place = Place.LOG },
                onSettings = { place = Place.SETTINGS },
            )
            Place.LOG -> LogScreen(all, onBack = { place = Place.RADIO })
            Place.SETTINGS -> SettingsScreen(settings, onBack = { place = Place.RADIO })
        }
    }
}
