package app.longwave.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import app.longwave.audio.Gens.gain
import app.longwave.audio.Gens.lowpass
import app.longwave.radio.Dial
import app.longwave.radio.Layer
import app.longwave.radio.Programme
import app.longwave.radio.Sky
import app.longwave.radio.Sound
import app.longwave.radio.Station
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * The receiver. One audio thread mixes whatever is on the dial at [tuned] kHz: the station's
 * sound, as clear as the tuning allows, the hiss and crackle of the air between stations, and
 * the whistle of a station just off tune. Long-wave fading makes a locked station slowly swell
 * and sink. The voice of a programme plays separately ([Narrator]) and the sound ducks under it.
 */
object Engine {
    const val RATE = 32000
    private const val CHUNK = 1024

    private lateinit var app: Context
    private val loader = Executors.newSingleThreadExecutor()
    private val beds = ConcurrentHashMap<String, Gen>()
    private val loading = ConcurrentHashMap.newKeySet<String>()

    @Volatile var tuned = 198f
    /** Pulls the tuning away from [tuned] as the radio goes off the air, in kHz. */
    @Volatile var drift = 0f
    @Volatile var sky = Sky(LocalDateTime.now())
    /** How loud the air between stations is, 0..1. */
    @Volatile var staticLevel = 0.6f
    /** Long-wave fading: a locked station slowly swells and sinks. */
    @Volatile var fading = true
    /** True while a voice is speaking over the station. */
    @Volatile var duck = false
    /** The off-the-air fade, 1 = full, 0 = silent. */
    @Volatile var fade = 1f

    /** What the needle is on right now, and how clearly; read by the dial and the voice. */
    @Volatile var station: Station? = null
        private set
    @Volatile var signal = 0f
        private set
    /** Loudness of the output, 0..1, for the tuning eye to flicker with. */
    @Volatile var level = 0f
        private set

    val metronome = Metronome()

    @Volatile private var worker: Worker? = null

    fun init(context: Context) {
        app = context.applicationContext
    }

    val running: Boolean get() = worker?.stopping == false

    fun start() {
        if (running) return
        worker = Worker().also { it.start() }
    }

    fun stop() {
        worker?.stopping = true
        worker = null
        signal = 0f
        level = 0f
    }

    /** Where [khz] lands tonight, including the sign-off drift. */
    fun at(khz: Float) = Dial.at(khz, sky)

    private fun bed(s: Station): Gen? {
        beds[s.id]?.let { return it }
        if (loading.add(s.id)) {
            loader.execute {
                runCatching { build(s.layers) }.getOrNull()?.let { g ->
                    // Only a few stay ready: the ones around the needle.
                    if (beds.size >= 4) beds.keys.firstOrNull { it != s.id }?.let { beds.remove(it) }
                    beds[s.id] = g
                }
                loading.remove(s.id)
            }
        }
        return null
    }

    private fun build(layers: List<Layer>): Gen {
        val rnd = Random(System.nanoTime())
        val parts = layers.map { l -> source(l.sound, rnd).gain(l.gain) }
        // A long-wave set never had the top end: soften everything a little.
        return Gen { var x = 0f; for (p in parts) x += p.next(); x }.lowpass(6500f)
    }

    private fun source(s: Sound, rnd: Random): Gen = when (s) {
        Sound.BROWN -> Gens.deep(rnd)
        Sound.HUM -> Gens.hum(rnd)
        Sound.MUSIC -> Gens.musicBox(rnd)
        else -> Recording.load(app, s.name.lowercase())?.let { Recording.endless(it, rnd) } ?: Gen { 0f }
    }

    private class Worker : Thread("longwave") {
        @Volatile var stopping = false

        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val min = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(maxOf(min, CHUNK * 2) * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull() ?: return
            val rnd = Random(System.nanoTime())
            val air = Gens.static(rnd)
            val whistle = Gens.Whistle()
            val qsb = Gens.Drift(rnd, 6f, 16f, 0.08f)
            val buf = ShortArray(CHUNK)
            // Valves take a moment to warm up; the sound comes in with them.
            var warm = 0f
            var duckLevel = 1f
            var fadeLevel = fade
            var sig = 0f
            // The station being left keeps playing while it fades, so a change is a crossfade
            // through the static, never a cut.
            var heldId: String? = null
            var held: Gen? = null
            var heldLevel = 0f
            var leaving: Gen? = null
            var leavingLevel = 0f
            var heard = 0f
            track.play()
            try {
                while (true) {
                    val f = tuned + drift
                    val hit = Dial.at(f, sky)
                    val st = hit?.first
                    val target = hit?.second ?: 0f
                    station = st
                    val bed = st?.let { bed(it) }
                    if (st?.id != heldId || (held == null && bed != null)) {
                        if (held != null && heldLevel > 0.001f) {
                            leaving = held
                            leavingLevel = heldLevel
                        }
                        held = bed
                        heldId = st?.id
                        heldLevel = 0f
                    }
                    val detune = st?.let { abs(it.khz - f) } ?: Dial.REACH
                    val metro = st?.programme == Programme.METRONOME
                    var energy = 0f
                    for (i in 0 until CHUNK) {
                        warm = if (stopping) warm - 1f / (RATE * 1.2f) else minOf(1f, warm + 1f / (RATE * 2.5f))
                        // Glide everything so turning the knob never clicks.
                        // About half a second to follow the needle, so tuning sounds like tuning.
                        sig += (target - sig) * 0.00007f
                        duckLevel += ((if (duck) 0.35f else 1f) - duckLevel) * 0.0004f
                        fadeLevel += (fade - fadeLevel) * 0.0002f
                        val swell = if (fading) 0.78f + 0.22f * qsb.next() else 1f
                        val clear = sig.pow(0.8f)
                        var x = 0f
                        heldLevel += (clear - heldLevel) * 0.00007f
                        held?.let { x += it.next() * heldLevel * swell * duckLevel }
                        leaving?.let {
                            leavingLevel *= 0.99996f
                            x += it.next() * leavingLevel * swell * duckLevel
                            if (leavingLevel < 0.001f) leaving = null
                        }
                        if (metro) x += metronome.next() * clear
                        // The air: loud between stations, a breath of it under a clear one, rising as a station sinks.
                        val hiss = staticLevel * (0.06f + 0.94f * (1f - clear).pow(1.4f) + (1f - swell) * 0.5f * clear)
                        x += air.next() * hiss * 0.55f
                        if (sig > 0.02f && sig < 0.97f) x += whistle.next(70f + detune * 320f) * 0.035f * staticLevel * sin(PI.toFloat() * sig)
                        val y = tanh(x * 1.1f) * warm.coerceAtLeast(0f) * fadeLevel
                        energy += y * y
                        buf[i] = (y * 0.85f * Short.MAX_VALUE).toInt().toShort()
                    }
                    signal = sig
                    heard += (sqrt(energy / CHUNK) * 3f - heard) * 0.3f
                    level = heard.coerceIn(0f, 1f)
                    track.write(buf, 0, CHUNK)
                    if (stopping && warm <= 0f) break
                }
            } finally {
                runCatching { track.stop() }
                track.release()
            }
        }
    }
}
