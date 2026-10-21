package app.longwave.radio

import app.longwave.audio.Metronome
import app.longwave.content.Script
import kotlinx.coroutines.delay

/**
 * What each spoken station puts on the air, line by line. [say] speaks one line at a gain and
 * returns when it has been said. Programmes that could go on all night (the forecast, the
 * lottery, the inventory, the small-hours station) do; the others end and leave their sound on.
 */
class Broadcast(
    private val script: Script,
    private val metronome: Metronome,
    private val say: suspend (text: String, gain: Float) -> Unit,
) {
    private suspend fun pause(seconds: Double) = delay((seconds * 1000).toLong())

    suspend fun run(p: Programme) {
        pause(1.5)
        when (p) {
            Programme.FORECAST -> forecast()
            Programme.ROLL_CALL -> rollCall()
            Programme.LOTTERY -> lottery()
            Programme.METRONOME -> metronome()
            Programme.INVENTORY -> inventory()
            Programme.COUNTDOWN -> countdown()
            Programme.OWL -> owl()
        }
    }

    private suspend fun intro(lines: List<String>, gap: Double = 1.6) {
        lines.forEach { say(it, 1f); pause(gap) }
    }

    private suspend fun forecast() {
        intro(script.forecastIntro)
        pause(1.5)
        while (true) {
            for (sea in script.seas.shuffled()) {
                say(sea, 1f); pause(1.4)
                say(script.winds.random(), 1f); pause(1.0)
                say(script.seaStates.random(), 1f); pause(1.0)
                say(script.weather.random(), 1f); pause(1.0)
                say(script.visibility.random(), 1f); pause(5.5)
            }
            say(script.forecastOutro, 1f)
            pause(75.0)
        }
    }

    private suspend fun rollCall() {
        intro(script.rollIntro, 3.0)
        pause(3.0)
        script.roll.forEachIndexed { i, part ->
            // Quieter as the roll goes on: by the end it is almost a whisper.
            say(part, 1f - 0.35f * i / script.roll.size)
            pause(9.0)
            if (i % 6 == 5) {
                say(script.rollSettle[(i / 6) % script.rollSettle.size], 0.75f)
                pause(7.0)
            }
        }
        say(script.rollOutro, 0.6f)
    }

    private suspend fun lottery() {
        intro(script.lotteryIntro, 2.2)
        pause(4.0)
        while (true) {
            for (w in script.words.shuffled()) {
                say(w, 0.9f); pause(3.5)
                say(w, 0.7f); pause(9.0)
            }
        }
    }

    private suspend fun metronome() {
        intro(script.metronomeIntro, 2.0)
        pause(2.0)
        metronome.breathe(4)
        while (!metronome.done) delay(200)
        pause(2.5)
        say(script.metronomeFree, 0.8f)
        pause(1.5)
        metronome.slow()
        while (!metronome.done) delay(200)
    }

    private suspend fun inventory() {
        intro(script.inventoryIntro, 2.0)
        pause(2.0)
        while (true) {
            for (item in script.items.shuffled()) {
                say(item, 1f); pause(6.0)
            }
            say(script.inventoryOutro, 1f)
            pause(90.0)
        }
    }

    private suspend fun countdown() {
        intro(script.countdownIntro, 2.0)
        pause(3.0)
        script.numbers.forEachIndexed { i, n ->
            say(n, 1f - 0.6f * i / (script.numbers.size - 1))
            pause(5.0)
        }
    }

    private suspend fun owl() {
        intro(script.owlIntro, 2.5)
        while (true) {
            for (line in script.owl.shuffled()) {
                pause(35.0)
                say(line, 0.85f)
            }
        }
    }
}
