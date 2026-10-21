package app.longwave.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

/** How loud the air between stations is. */
enum class Air(val level: Float) { QUIET(0.25f), SOFT(0.5f), FULL(0.8f) }

data class RadioSettings(
    val onboarded: Boolean = false,
    /** The listener's call sign, printed on every QSL card. */
    val callsign: String = "",
    /** Where the needle was left, in kHz. */
    val khz: Float = 153f,
    /** Minutes until the radio goes off the air; 0 plays until switched off. */
    val timer: Int = 30,
    /** How long the station takes to drift away into the static and go quiet, in minutes. */
    val signOff: Int = 3,
    val air: Air = Air.SOFT,
    val fading: Boolean = true,
    val haptics: Boolean = true,
    /** The announcer says good night as the radio goes off the air. */
    val goodNight: Boolean = true,
    /** Rare stations heard at least once, so the dial can name them. */
    val found: Set<String> = emptySet(),
)

class SettingsStore(private val context: Context) {
    private val onboardedKey = booleanPreferencesKey("onboarded")
    private val callsignKey = stringPreferencesKey("callsign")
    private val khzKey = floatPreferencesKey("khz")
    private val timerKey = intPreferencesKey("timer")
    private val signOffKey = intPreferencesKey("signOff")
    private val airKey = stringPreferencesKey("air")
    private val fadingKey = booleanPreferencesKey("fading")
    private val hapticsKey = booleanPreferencesKey("haptics")
    private val goodNightKey = booleanPreferencesKey("goodNight")
    private val foundKey = stringPreferencesKey("found")

    val settings: Flow<RadioSettings> = context.dataStore.data.map { p ->
        RadioSettings(
            onboarded = p[onboardedKey] ?: false,
            callsign = p[callsignKey] ?: "",
            khz = p[khzKey] ?: 153f,
            timer = p[timerKey] ?: 30,
            signOff = p[signOffKey] ?: 3,
            air = p[airKey]?.let { runCatching { Air.valueOf(it) }.getOrNull() } ?: Air.SOFT,
            fading = p[fadingKey] ?: true,
            haptics = p[hapticsKey] ?: true,
            goodNight = p[goodNightKey] ?: true,
            found = (p[foundKey] ?: "").split(",").filter { it.isNotBlank() }.toSet(),
        )
    }

    suspend fun now() = settings.first()

    suspend fun welcome(callsign: String) = context.dataStore.edit {
        it[callsignKey] = callsign.trim()
        it[onboardedKey] = true
    }

    suspend fun setCallsign(v: String) = context.dataStore.edit { it[callsignKey] = v.trim() }
    suspend fun setKhz(v: Float) = context.dataStore.edit { it[khzKey] = v }
    suspend fun setTimer(v: Int) = context.dataStore.edit { it[timerKey] = v }
    suspend fun setSignOff(v: Int) = context.dataStore.edit { it[signOffKey] = v }
    suspend fun setAir(v: Air) = context.dataStore.edit { it[airKey] = v.name }
    suspend fun setFading(v: Boolean) = context.dataStore.edit { it[fadingKey] = v }
    suspend fun setHaptics(v: Boolean) = context.dataStore.edit { it[hapticsKey] = v }
    suspend fun setGoodNight(v: Boolean) = context.dataStore.edit { it[goodNightKey] = v }

    suspend fun found(id: String) = context.dataStore.edit { p ->
        val now = (p[foundKey] ?: "").split(",").filter { it.isNotBlank() }.toSet()
        if (id !in now) p[foundKey] = (now + id).joinToString(",")
    }
}
