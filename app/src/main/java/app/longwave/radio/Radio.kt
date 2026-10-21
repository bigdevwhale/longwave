package app.longwave.radio

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import app.longwave.AppContainer
import app.longwave.BuildConfig
import app.longwave.audio.Engine
import app.longwave.content.Script
import app.longwave.data.Reception
import app.longwave.localized
import app.longwave.ru
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * The radio as the screen and the notification see it: on or off, when it goes off the air, and
 * whether it is going off the air right now. It runs the evening too: the programme of whichever
 * spoken station the needle rests on, the timer, and the sign-off, when the station drifts away
 * into the static and the static goes quiet. A sign-off that finishes is a night you probably
 * fell asleep, and it leaves a QSL card from the station in the log.
 */
object Radio {
    data class State(
        val on: Boolean = false,
        /** Wall-clock millis when it goes off the air; 0 plays until switched off. */
        val offAirAt: Long = 0,
        val signingOff: Boolean = false,
        val startedAt: Long = 0,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private lateinit var c: AppContainer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var session: Job? = null
    private var programme: Job? = null
    private var programmeOf: String? = null
    @Volatile private var lineGain = 1f
    /** Silenced for a phone call or another app's sound. */
    @Volatile var muted = false

    // The sign-off in progress.
    private var signOffStart = 0L
    private var signOffMs = 0L
    private var direction = 1f
    private var stationAtSignOff: String? = null
    private val heard = HashMap<String, Long>()

    fun init(container: AppContainer) {
        c = container
    }

    fun switchOn(context: Context) {
        if (_state.value.on) return
        scope.launch {
            val s = c.settings.now()
            Engine.sky = Sky(LocalDateTime.now(), BuildConfig.DEMO)
            Engine.staticLevel = s.air.level
            Engine.fading = s.fading
            Engine.tuned = s.khz
            Engine.drift = 0f
            Engine.fade = 1f
            heard.clear()
            val now = System.currentTimeMillis()
            _state.value = State(on = true, offAirAt = if (s.timer > 0) now + s.timer * 60_000L else 0, startedAt = now)
            Engine.start()
            ContextCompat.startForegroundService(context, Intent(context, RadioService::class.java))
            session = scope.launch { evening(context.applicationContext) }
        }
    }

    fun switchOff(context: Context) {
        session?.cancel()
        session = null
        stopProgramme()
        Engine.stop()
        Engine.drift = 0f
        Engine.fade = 1f
        _state.value = State()
        context.stopService(Intent(context, RadioService::class.java))
    }

    fun tune(khz: Float) {
        Engine.tuned = khz.coerceIn(Dial.MIN, Dial.MAX)
        // Someone turning the knob is awake: the radio stays on a while longer.
        if (_state.value.signingOff) extend(15)
    }

    /** A new timer from now, in minutes; 0 for none. */
    fun setTimer(minutes: Int) {
        val st = _state.value
        if (!st.on) return
        if (st.signingOff) cancelSignOff()
        _state.value = _state.value.copy(offAirAt = if (minutes > 0) System.currentTimeMillis() + minutes * 60_000L else 0)
    }

    fun extend(minutes: Int) {
        val st = _state.value
        if (!st.on) return
        if (st.signingOff) cancelSignOff()
        val from = maxOf(System.currentTimeMillis(), _state.value.offAirAt)
        _state.value = _state.value.copy(offAirAt = if (st.offAirAt == 0L) 0 else from + minutes * 60_000L)
    }

    private fun cancelSignOff() {
        _state.value = _state.value.copy(signingOff = false)
        Engine.fade = 1f
    }

    private fun stopProgramme() {
        programme?.cancel()
        programme = null
        programmeOf = null
        c.narrator.stop()
        Engine.metronome.stop()
        Engine.duck = false
    }

    private suspend fun say(text: String, gain: Float, lang: String) {
        lineGain = gain
        if (!c.narrator.has(text, lang)) {
            // Not recorded: leave the air to the station for about as long as the line would take.
            delay(1500L + text.length * 55L)
            return
        }
        Engine.duck = true
        try {
            c.narrator.say(text, lang)
        } finally {
            Engine.duck = false
        }
    }

    private suspend fun evening(context: Context) {
        val settings = c.settings.now()
        val ru = context.localized().ru
        val lang = if (ru) "ru" else "en"
        val script = Script.of(ru)
        val broadcast = Broadcast(script, Engine.metronome) { t, g -> say(t, g, lang) }
        var lastTick = SystemClock.elapsedRealtime()
        var lastSky = lastTick
        val found = HashSet(settings.found)
        while (scope.isActive) {
            delay(200)
            val now = SystemClock.elapsedRealtime()
            val dt = now - lastTick
            lastTick = now
            if (now - lastSky > 60_000) {
                Engine.sky = Sky(LocalDateTime.now(), BuildConfig.DEMO)
                lastSky = now
            }
            val station = Engine.station
            val signal = Engine.signal
            if (station != null && signal > 0.5f) {
                heard[station.id] = (heard[station.id] ?: 0) + dt
                if (station.rare && station.id !in found) {
                    found += station.id
                    c.settings.found(station.id)
                }
            }

            // The spoken programme follows the needle: tune in and it starts, tune away and it stops.
            val st = _state.value
            if (!st.signingOff) {
                if (station?.programme != null && signal > 0.55f && station.id != programmeOf) {
                    stopProgramme()
                    programmeOf = station.id
                    val p = station.programme
                    programme = scope.launch { broadcast.run(p) }
                } else if (programmeOf != null && (station?.id != programmeOf || signal < 0.2f)) {
                    stopProgramme()
                }
            }
            c.narrator.setVolume(if (muted) 0f else signal * lineGain * Engine.fade)

            if (!st.signingOff) {
                // Back from a cancelled sign-off: the needle settles back onto the station.
                if (Engine.drift != 0f) Engine.drift = if (kotlin.math.abs(Engine.drift) < 0.02f) 0f else Engine.drift * 0.85f
                if (st.offAirAt > 0) {
                    val left = st.offAirAt - System.currentTimeMillis()
                    val total = st.offAirAt - st.startedAt
                    val length = (settings.signOff * 60_000L).coerceAtMost(total / 3).coerceAtLeast(30_000L)
                    if (left <= length) beginSignOff(length, settings.goodNight, script.signOff, lang)
                }
            } else {
                val p = (System.currentTimeMillis() - signOffStart).toFloat() / signOffMs
                Engine.drift = direction * Dial.REACH * 1.7f * smooth(p / 0.7f)
                Engine.fade = 1f - smooth((p - 0.55f) / 0.45f)
                if (p >= 1f) {
                    offTheAir(context)
                    return
                }
            }
        }
    }

    private fun beginSignOff(length: Long, goodNight: Boolean, line: String, lang: String) {
        stationAtSignOff = Engine.station?.takeIf { Engine.signal > 0.5f }?.id
        signOffStart = System.currentTimeMillis()
        signOffMs = length
        direction = if (Random.nextBoolean()) 1f else -1f
        _state.value = _state.value.copy(signingOff = true)
        stopProgramme()
        if (goodNight) programme = scope.launch { delay(1500); say(line, 0.55f, lang) }
    }

    private suspend fun offTheAir(context: Context) {
        val st = _state.value
        val stationId = stationAtSignOff ?: heard.maxByOrNull { it.value }?.key
        val end = System.currentTimeMillis()
        val minutes = ((end - st.startedAt) / 60_000L).toInt()
        if (stationId != null && minutes >= 5) {
            val s = Dial.byId(stationId)!!
            c.db.dao().add(
                Reception(stationId = s.id, khz = s.khz, startedAt = st.startedAt, endedAt = end, minutes = minutes, callsign = c.settings.now().callsign),
            )
        }
        session = null
        scope.launch(Dispatchers.Main) { switchOff(context) }
    }

    private fun smooth(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return t * t * (3 - 2 * t)
    }
}
