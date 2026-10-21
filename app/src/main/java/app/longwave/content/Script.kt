package app.longwave.content

/**
 * Everything the stations say, in one language. Lines wrapped in [a] are read by the announcer
 * (a lower, even radio voice), lines in [s] by the soft night voice; tools/narrate.py finds them
 * by those wrappers and records each one ahead of time.
 */
class Script(
    val forecastIntro: List<String>,
    val seas: List<String>,
    val winds: List<String>,
    val seaStates: List<String>,
    val weather: List<String>,
    val visibility: List<String>,
    val forecastOutro: String,
    val rollIntro: List<String>,
    val roll: List<String>,
    /** Said every few parts while the roll call goes on. */
    val rollSettle: List<String>,
    val rollOutro: String,
    val lotteryIntro: List<String>,
    val words: List<String>,
    val metronomeIntro: List<String>,
    val metronomeFree: String,
    val inventoryIntro: List<String>,
    val items: List<String>,
    val inventoryOutro: String,
    val countdownIntro: List<String>,
    /** One hundred down to one. */
    val numbers: List<String>,
    val owlIntro: List<String>,
    val owl: List<String>,
    /** Said quietly as the radio begins to go off the air. */
    val signOff: String,
) {
    companion object {
        fun of(ru: Boolean) = if (ru) ScriptRu.script else ScriptEn.script
    }
}

/** Announcer. */
internal fun a(t: String) = t

/** Soft night voice. */
internal fun s(t: String) = t

enum class Speaker { ANNOUNCER, SOFT }
