package app.longwave.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** One sample at a time, roughly -1..1. */
fun interface Gen {
    fun next(): Float
}

object Gens {
    private const val R = Engine.RATE.toFloat()
    private const val TAU = 2f * PI.toFloat()

    fun coef(hz: Float) = 1f - exp(-TAU * hz / R)

    /** Paul Kellet's economy pink filter. */
    fun pink(rnd: Random): Gen {
        var b0 = 0f; var b1 = 0f; var b2 = 0f
        return Gen {
            val w = rnd.nextFloat() * 2 - 1
            b0 = 0.99765f * b0 + w * 0.0990460f
            b1 = 0.96300f * b1 + w * 0.2965164f
            b2 = 0.57000f * b2 + w * 1.0526913f
            (b0 + b1 + b2 + w * 0.1848f) * 0.22f
        }
    }

    fun brown(rnd: Random): Gen {
        var b = 0f
        return Gen {
            b = (b + 0.02f * (rnd.nextFloat() * 2 - 1)) / 1.02f
            b * 3.5f
        }
    }

    fun Gen.lowpass(hz: Float): Gen {
        val a = coef(hz)
        var y = 0f
        return Gen { y += a * (next() - y); y }
    }

    fun Gen.highpass(hz: Float): Gen {
        val a = coef(hz)
        var y = 0f
        return Gen { val x = next(); y += a * (x - y); x - y }
    }

    fun Gen.gain(g: Float) = Gen { next() * g }

    /** A value that wanders smoothly between 0 and 1, retargeting every [minS]..[maxS] seconds. */
    class Drift(private val rnd: Random, private val minS: Float, private val maxS: Float, smoothHz: Float) {
        private val a = coef(smoothHz)
        private var target = rnd.nextFloat()
        private var left = 0
        var value = target
            private set

        fun next(): Float {
            if (--left <= 0) {
                target = rnd.nextFloat()
                left = ((minS + rnd.nextFloat() * (maxS - minS)) * R).toInt()
            }
            value += a * (target - value)
            return value
        }
    }

    /** The deep, soft hiss of the night: brown noise with the top taken off. */
    fun deep(rnd: Random): Gen = brown(rnd).lowpass(900f).gain(1.1f)

    /**
     * The air between stations: a band of hiss that breathes, with the odd crackle of a far
     * thunderstorm, the way long wave sounds at night.
     */
    fun static(rnd: Random): Gen {
        val hiss = pink(rnd).highpass(250f).lowpass(3800f)
        val rumble = brown(rnd).lowpass(180f)
        val breath = Drift(rnd, 1.5f, 5f, 0.6f)
        var crackle = 0f
        var crackleDecay = 0f
        val clp = coef(2200f)
        var cy = 0f
        return Gen {
            if (crackle < 0.002f && rnd.nextFloat() < 2.4f / R) {
                crackle = 0.25f + rnd.nextFloat() * 0.55f
                crackleDecay = exp(-1f / (R * (0.002f + rnd.nextFloat() * 0.01f)))
            }
            val c = crackle * (rnd.nextFloat() * 2 - 1)
            crackle *= crackleDecay
            cy += clp * (c - cy)
            hiss.next() * (0.55f + 0.45f * breath.next()) * 1.4f + rumble.next() * 0.5f + cy
        }
    }

    /** A warm, slowly breathing chord on A, the hum of valves warmed through. */
    fun hum(rnd: Random): Gen {
        val freqs = floatArrayOf(110f, 220f, 220.7f, 330f, 440f, 441.2f, 660f, 881f)
        val amps = floatArrayOf(0.14f, 0.2f, 0.13f, 0.13f, 0.09f, 0.07f, 0.04f, 0.02f)
        val phases = FloatArray(freqs.size)
        val swell = Array(freqs.size) { Drift(rnd, 5f, 14f, 0.12f) }
        val air = pink(rnd).lowpass(900f)
        return Gen {
            var x = 0f
            for (i in freqs.indices) {
                phases[i] += TAU * freqs[i] / R
                if (phases[i] > TAU) phases[i] -= TAU
                x += sin(phases[i]) * amps[i] * (0.35f + 0.65f * swell[i].next())
            }
            x * 1.1f + air.next() * 0.05f
        }
    }

    /**
     * A music box for the moon station: unhurried notes from a pentatonic scale, so no two ever
     * clash, each a soft bell with a long fade, and a little echo for the room.
     */
    fun musicBox(rnd: Random): Gen {
        val scale = floatArrayOf(392f, 440f, 523.25f, 587.33f, 659.26f, 783.99f, 880f, 1046.5f, 1174.66f)
        val max = 10
        val ph = FloatArray(max * 3)
        val step = FloatArray(max * 3)
        val amp = FloatArray(max)
        val dec = FloatArray(max)
        var n = 0
        var wait = (R * 0.8f).toInt()
        var last = 4
        val parts = floatArrayOf(1f, 2.76f, 5.4f)
        val partAmp = floatArrayOf(1f, 0.28f, 0.08f)
        fun note(i: Int, a: Float) {
            if (n == max) return
            for (p in 0 until 3) {
                ph[n * 3 + p] = 0f
                step[n * 3 + p] = TAU * scale[i] * parts[p] / R
            }
            amp[n] = a
            dec[n] = exp(-1f / (R * (1.6f + rnd.nextFloat() * 1.2f)))
            n++
        }
        val d1 = FloatArray((R * 0.41f).toInt())
        var i1 = 0
        var lp = 0f
        return Gen {
            if (--wait <= 0) {
                last = (last + rnd.nextInt(-2, 3)).coerceIn(0, scale.size - 1)
                note(last, 0.12f + rnd.nextFloat() * 0.06f)
                if (rnd.nextFloat() < 0.2f) note((last - 3).coerceAtLeast(0), 0.06f)
                // A slow tune that sometimes stops to think.
                wait = ((0.7f + rnd.nextFloat() * (if (rnd.nextFloat() < 0.15f) 4f else 1.4f)) * R).toInt()
            }
            var x = 0f
            var k = 0
            while (k < n) {
                var v = 0f
                for (p in 0 until 3) {
                    v += sin(ph[k * 3 + p]) * partAmp[p]
                    ph[k * 3 + p] += step[k * 3 + p]
                }
                x += v * amp[k]
                amp[k] *= dec[k]
                if (amp[k] < 0.0004f) {
                    n--
                    for (p in 0 until 3) { ph[k * 3 + p] = ph[n * 3 + p]; step[k * 3 + p] = step[n * 3 + p] }
                    amp[k] = amp[n]; dec[k] = dec[n]
                } else k++
            }
            val e = d1[i1]
            lp += 0.2f * (e - lp)
            d1[i1] = x + lp * 0.5f
            i1 = (i1 + 1) % d1.size
            (x + lp * 0.5f) * 1.5f
        }
    }

    /** The whistle you hear tuning past a station: higher the further off, gone once locked. */
    class Whistle {
        private var ph = 0f
        fun next(hz: Float): Float {
            ph += TAU * hz / R
            if (ph > TAU) ph -= TAU
            return sin(ph)
        }
    }
}
