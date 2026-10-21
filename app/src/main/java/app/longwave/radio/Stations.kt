package app.longwave.radio

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.abs

/** What a station is made of: a recording from assets/ambience, or a sound synthesised on the fly. */
enum class Sound { RAIN, OCEAN, TRAIN, FIRE, CRICKETS, BOAT, STREAM, WIND, STORM, BROWN, MUSIC, HUM }

/** One layer of a station's sound and how loud it sits. */
data class Layer(val sound: Sound, val gain: Float)

/** A spoken programme on top of the sound. */
enum class Programme { FORECAST, ROLL_CALL, LOTTERY, METRONOME, INVENTORY, COUNTDOWN, OWL }

/** Rare stations only come on the air some nights. */
enum class Rarity { ALWAYS, STORM_NIGHTS, SMALL_HOURS, FULL_MOON }

/**
 * A station on the long-wave dial: a made-up place broadcasting one kind of night. [call] is the
 * three-letter call sign printed on its QSL card; [hue] colours the card.
 */
data class Station(
    val id: String,
    val khz: Float,
    val call: String,
    val en: String,
    val ru: String,
    val aboutEn: String,
    val aboutRu: String,
    val layers: List<Layer>,
    val hue: Long,
    val programme: Programme? = null,
    val rarity: Rarity = Rarity.ALWAYS,
) {
    fun name(ru: Boolean) = if (ru) this.ru else en
    fun about(ru: Boolean) = if (ru) aboutRu else aboutEn
    val rare: Boolean get() = rarity != Rarity.ALWAYS
}

object Dial {
    const val MIN = 145f
    const val MAX = 285f

    /** How far either side of a station it can still be heard, in kHz. Stations sit 9 apart. */
    const val REACH = 3f

    val STATIONS = listOf(
        Station("rain", 153f, "RFD", "Rainford", "Дождевск", "Rain on the roofs of Rainford", "Дождь над крышами Дождевска",
            listOf(Layer(Sound.RAIN, 1f)), 0xFF5B7A99),
        Station("storm", 157.5f, "STM", "Storm over Rainford", "Гроза над Дождевском", "Some nights only: far thunder, heavy rain", "Только в иные ночи: дальний гром и ливень",
            listOf(Layer(Sound.STORM, 1f), Layer(Sound.RAIN, 0.35f)), 0xFF4B5677, rarity = Rarity.STORM_NIGHTS),
        Station("light", 162f, "QLT", "Quiet Light", "Маяк Тихий", "The sea under the lighthouse", "Море под маяком",
            listOf(Layer(Sound.OCEAN, 1f)), 0xFF3F7F86),
        Station("owl", 166.5f, "OWL", "For Those Awake", "Для тех, кто не спит", "Between half past one and half past four", "С половины второго до половины пятого",
            listOf(Layer(Sound.WIND, 0.45f), Layer(Sound.BROWN, 0.5f)), 0xFF7A6A94, Programme.OWL, Rarity.SMALL_HOURS),
        Station("forecast", 171f, "SEA", "Sea Forecast", "Морская сводка", "Weather for seas that aren't on any map", "Погода для морей, которых нет на картах",
            listOf(Layer(Sound.BROWN, 0.55f), Layer(Sound.OCEAN, 0.25f)), 0xFF2F5D7C, Programme.FORECAST),
        Station("train", 180f, "NXP", "Night Express", "Ночной скорый", "A sleeper carriage, somewhere past midnight", "Спальный вагон где-то после полуночи",
            listOf(Layer(Sound.TRAIN, 1f)), 0xFF8A5A44),
        Station("rollcall", 189f, "RCL", "Roll Call", "Перекличка", "The body answers, part by part: here", "Тело отвечает по частям: здесь",
            listOf(Layer(Sound.HUM, 0.5f), Layer(Sound.BROWN, 0.45f)), 0xFF6B7F5A, Programme.ROLL_CALL),
        Station("hearth", 198f, "HTH", "Hearth", "Печка", "A stove crackling in a log house", "Печка трещит в избе",
            listOf(Layer(Sound.FIRE, 1f)), 0xFFB0603A),
        Station("lottery", 207f, "LTO", "Word Lottery", "Лото слов", "Slow random words to picture, one by one", "Медленные случайные слова — представляйте каждое",
            listOf(Layer(Sound.RAIN, 0.35f), Layer(Sound.BROWN, 0.3f)), 0xFFA0794A, Programme.LOTTERY),
        Station("crickets", 216f, "CRK", "Cricket Meadow", "Сверчково", "A summer night at the edge of a meadow", "Летняя ночь на краю луга",
            listOf(Layer(Sound.CRICKETS, 1f)), 0xFF55794B),
        Station("metronome", 225f, "MTR", "Metronome", "Метроном", "Breathe to the ticks: in for four, hold for seven, out for eight", "Дыхание под щелчки: вдох на четыре, задержка на семь, выдох на восемь",
            listOf(Layer(Sound.BROWN, 0.5f)), 0xFF8C4A4A, Programme.METRONOME),
        Station("harbour", 234f, "HBR", "Calm Harbour", "Порт Безветрие", "A boat rocking at the pier", "Лодка качается у причала",
            listOf(Layer(Sound.BOAT, 1f)), 0xFF3E6A8A),
        Station("stores", 243f, "INV", "Lighthouse Stores", "Склад маяка", "An inventory, read aloud very slowly", "Опись склада, прочитанная очень медленно",
            listOf(Layer(Sound.WIND, 0.3f), Layer(Sound.BROWN, 0.4f)), 0xFF7D6B4F, Programme.INVENTORY),
        Station("moon", 247.5f, "MON", "Moon Station", "Лунная", "On the air for three nights around the full moon", "В эфире три ночи вокруг полнолуния",
            listOf(Layer(Sound.MUSIC, 0.9f), Layer(Sound.CRICKETS, 0.25f)), 0xFF8E8A6A, rarity = Rarity.FULL_MOON),
        Station("river", 252f, "RVR", "Pine River", "Ручей", "A stream running through the pines", "Ручей бежит среди сосен",
            listOf(Layer(Sound.STREAM, 1f)), 0xFF4A7A68),
        Station("countdown", 261f, "CNT", "Countdown", "Отсчёт", "From a hundred down, a little quieter each time", "От ста вниз, с каждым числом чуть тише",
            listOf(Layer(Sound.BROWN, 0.55f)), 0xFF6A6A84, Programme.COUNTDOWN),
        Station("pass", 270f, "PSS", "High Pass", "Перевал", "Wind over the mountain pass", "Ветер над перевалом",
            listOf(Layer(Sound.WIND, 1f)), 0xFF6E7C8C),
        Station("deep", 279f, "DPA", "Deep Air", "Глубокий эфир", "Nothing but the soft hiss of the night", "Только мягкий шорох ночного эфира",
            listOf(Layer(Sound.BROWN, 1f)), 0xFF4D4A63),
    )

    fun byId(id: String) = STATIONS.firstOrNull { it.id == id }

    /** The station [khz] is closest to, if it can be heard there tonight, and how clearly (0..1). */
    fun at(khz: Float, sky: Sky): Pair<Station, Float>? {
        val s = STATIONS.filter { sky.onAir(it) }.minByOrNull { abs(it.khz - khz) } ?: return null
        val d = abs(s.khz - khz)
        if (d >= REACH) return null
        return s to signal(d)
    }

    /** A clean lock near the centre, falling off smoothly into static at the edge of [REACH]. */
    fun signal(detune: Float): Float {
        val x = (abs(detune) / REACH).coerceIn(0f, 1f)
        val t = ((x - 0.15f) / 0.85f).coerceIn(0f, 1f)
        return 1f - t * t * (3 - 2 * t)
    }

    /** The station on air nearest to [khz]: where the dial comes to rest after a drag or a fling. */
    fun nearest(khz: Float, sky: Sky): Float =
        STATIONS.filter { sky.onAir(it) }.minByOrNull { abs(it.khz - khz) }?.khz ?: khz

    /** The next station on air above ([dir] = 1) or below (-1) [from]; [from] itself at either end of the band. */
    fun next(from: Float, dir: Int, sky: Sky): Float {
        val on = STATIONS.filter { sky.onAir(it) }.map { it.khz }
        return if (dir > 0) on.filter { it > from + 0.1f }.minOrNull() ?: from else on.filter { it < from - 0.1f }.maxOrNull() ?: from
    }

    /** The nearest station on air to snap to after a fling, within [within] kHz. */
    fun snap(khz: Float, sky: Sky, within: Float = 1.2f): Float =
        STATIONS.filter { sky.onAir(it) }.minByOrNull { abs(it.khz - khz) }?.takeIf { abs(it.khz - khz) <= within }?.khz ?: khz
}

/**
 * Tonight's sky: which rare stations are on the air. A night belongs to the evening it started on,
 * so three in the morning still counts as last night.
 */
class Sky(private val now: LocalDateTime, private val everything: Boolean = false) {
    val night: LocalDate = if (now.toLocalTime() < LocalTime.NOON) now.toLocalDate().minusDays(1) else now.toLocalDate()

    fun onAir(s: Station): Boolean = everything || when (s.rarity) {
        Rarity.ALWAYS -> true
        Rarity.STORM_NIGHTS -> stormNight(night)
        Rarity.SMALL_HOURS -> now.toLocalTime().let { it >= LocalTime.of(1, 30) && it < LocalTime.of(4, 30) }
        Rarity.FULL_MOON -> moonAge(night.atTime(23, 0)).let { abs(it - SYNODIC / 2) <= 1.5 }
    }

    companion object {
        const val SYNODIC = 29.530588853

        /** About one night in five has a storm over Rainford; the same for everyone on that date. */
        fun stormNight(d: LocalDate): Boolean {
            var h = d.toEpochDay() * 2654435761L
            h = h xor (h ushr 15)
            return Math.floorMod(h, 5L) == 0L
        }

        /** Days since the last new moon (0 = new, ~14.77 = full), from the new moon of 6 Jan 2000, 18:14 UTC. */
        fun moonAge(t: LocalDateTime): Double {
            val ref = LocalDateTime.of(2000, 1, 6, 18, 14).toEpochSecond(ZoneOffset.UTC)
            val days = (t.toEpochSecond(ZoneOffset.UTC) - ref) / 86400.0
            return ((days % SYNODIC) + SYNODIC) % SYNODIC
        }
    }
}

/** 153 kHz, 157.5 кГц. */
fun khzLabel(v: Float, ru: Boolean): String {
    val n = if (v % 1f == 0f) v.toInt().toString() else (if (ru) "%.1f".format(java.util.Locale("ru"), v) else "%.1f".format(java.util.Locale.US, v))
    return if (ru) "$n кГц" else "$n kHz"
}
