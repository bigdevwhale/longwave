package app.longwave.data

import app.longwave.AppContainer
import app.longwave.BuildConfig
import app.longwave.radio.Dial
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

/**
 * The demo build starts a month into listening: a log of nights with QSL cards, a few rare
 * stations found, and one card that came in last night still waiting for its morning question.
 */
object DemoSeed {
    suspend fun seedIfEmpty(c: AppContainer, ru: Boolean, force: Boolean = BuildConfig.DEMO) {
        if (!force) return
        if (c.db.dao().receptions().first().isNotEmpty()) return
        val call = if (ru) "Лис-7" else "Fox-7"
        c.settings.welcome(call)
        c.settings.setKhz(171f)
        c.settings.setTimer(45)
        c.settings.found("storm")
        c.settings.found("owl")

        // days ago, station, bedtime (h, m), minutes, rating
        val nights = listOf(
            Triple(29, "rain", 23 to 10) to (38 to Reception.SLOW),
            Triple(27, "rain", 23 to 40) to (31 to Reception.FAST),
            Triple(25, "train", 0 to 5) to (44 to Reception.SLOW),
            Triple(23, "forecast", 23 to 20) to (27 to Reception.FAST),
            Triple(21, "lottery", 23 to 55) to (19 to Reception.FAST),
            Triple(19, "storm", 22 to 50) to (33 to Reception.FAST),
            Triple(17, "hearth", 23 to 30) to (41 to Reception.DONT_KNOW),
            Triple(15, "forecast", 23 to 15) to (24 to Reception.FAST),
            Triple(13, "owl", 2 to 10) to (52 to Reception.SLOW),
            Triple(11, "lottery", 23 to 5) to (22 to Reception.FAST),
            Triple(9, "metronome", 23 to 45) to (29 to Reception.SLOW),
            Triple(7, "crickets", 22 to 40) to (36 to Reception.DONT_KNOW),
            Triple(5, "forecast", 23 to 25) to (21 to Reception.FAST),
            Triple(3, "light", 23 to 50) to (34 to Reception.FAST),
            Triple(2, "lottery", 0 to 20) to (26 to Reception.FAST),
        )
        val zone = ZoneId.systemDefault()
        for ((night, end) in nights) {
            val (daysAgo, id, at) = night
            val (minutes, rating) = end
            // Times after midnight belong to the morning after that evening.
            val day = LocalDate.now().minusDays(daysAgo.toLong()).let { if (at.first < 12) it.plusDays(1) else it }
            val start = day.atTime(at.first, at.second).atZone(zone).toInstant().toEpochMilli()
            c.db.dao().add(
                Reception(stationId = id, khz = Dial.byId(id)!!.khz, startedAt = start, endedAt = start + minutes * 60_000L, minutes = minutes, callsign = call, rating = rating),
            )
        }
        // Last night: the Sea Forecast, still waiting for the morning question.
        val end = System.currentTimeMillis() - 9 * 3_600_000L
        c.db.dao().add(Reception(stationId = "forecast", khz = 171f, startedAt = end - 25 * 60_000L, endedAt = end, minutes = 25, callsign = call))
    }
}
