package app.longwave.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * The ticks of the Metronome station. First [breathe]: 4-7-8 breathing, high ticks to breathe in,
 * soft ones to hold, low ones to breathe out. Then [slow]: plain ticks that drift further apart
 * and quieter until they stop. [done] turns true when the current part has finished.
 */
class Metronome {
    private enum class Tick(val hz: Float, val amp: Float) { HIGH(1320f, 0.5f), SOFT(990f, 0.16f), LOW(660f, 0.5f), PLAIN(880f, 0.4f) }

    private val rnd = Random(7)
    @Volatile var done = true
        private set
    @Volatile private var plan: ArrayDeque<Tick> = ArrayDeque()
    @Volatile private var slowing = false
    private var gap = R.toInt()
    private var wait = 0
    private var slowAmp = 1f

    // The sound of the tick now ringing.
    private var hz = 0f
    private var amp = 0f
    private var ph = 0f
    private var click = 0f

    fun breathe(cycles: Int) {
        val p = ArrayDeque<Tick>()
        repeat(cycles) {
            repeat(4) { p.add(Tick.HIGH) }
            repeat(7) { p.add(Tick.SOFT) }
            repeat(8) { p.add(Tick.LOW) }
        }
        gap = R.toInt()
        wait = 0
        slowing = false
        plan = p
        done = false
    }

    fun slow() {
        gap = R.toInt()
        wait = 0
        slowAmp = 1f
        plan = ArrayDeque()
        slowing = true
        done = false
    }

    fun stop() {
        plan = ArrayDeque()
        slowing = false
        done = true
    }

    fun next(): Float {
        if (!done && --wait <= 0) {
            val t: Tick? = when {
                slowing -> Tick.PLAIN.takeIf { slowAmp > 0.12f }
                else -> plan.removeFirstOrNull()
            }
            if (t == null) {
                done = true
                slowing = false
            } else {
                hz = t.hz
                amp = t.amp * (if (slowing) slowAmp else 1f)
                ph = 0f
                click = amp * 0.6f
                if (slowing) {
                    // Each tick a little later and a little softer.
                    gap = (gap * 1.025f).toInt()
                    slowAmp *= 0.975f
                }
                wait = gap
            }
        }
        if (amp < 0.0005f) return 0f
        ph += 2f * PI.toFloat() * hz / R
        val v = sin(ph) * amp + click * (rnd.nextFloat() * 2 - 1)
        amp *= DECAY
        click *= CLICK_DECAY
        return v
    }

    private companion object {
        const val R = Engine.RATE.toFloat()
        val DECAY = exp(-1f / (R * 0.035f))
        val CLICK_DECAY = exp(-1f / (R * 0.002f))
    }
}
