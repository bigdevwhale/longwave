package app.longwave

import app.longwave.audio.Narrator
import app.longwave.content.Script
import app.longwave.data.Reception
import app.longwave.radio.Dial
import app.longwave.radio.Log
import app.longwave.radio.Rarity
import app.longwave.radio.Sky
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

class RadioTest {
    private val evening = Sky(LocalDateTime.of(2026, 10, 21, 22, 0))

    @Test fun stationsStayApartOnTheDial() {
        val sorted = Dial.STATIONS.sortedBy { it.khz }
        sorted.zipWithNext().forEach { (a, b) -> assertTrue("${a.id} and ${b.id} overlap", b.khz - a.khz > Dial.REACH) }
        assertTrue(sorted.all { it.khz > Dial.MIN && it.khz < Dial.MAX })
        assertEquals(Dial.STATIONS.size, Dial.STATIONS.map { it.call }.toSet().size)
    }

    @Test fun signalIsClearOnTheStationAndGoneAtTheEdge() {
        val (s, sig) = Dial.at(153f, evening)!!
        assertEquals("rain", s.id)
        assertEquals(1f, sig, 0.001f)
        assertTrue(Dial.at(154.5f, evening)!!.second in 0.1f..0.9f)
        assertNull(Dial.at(153f + Dial.REACH + 0.1f, evening))
    }

    @Test fun rareStationsAreSilentOnOrdinaryNights() {
        val owl = Dial.byId("owl")!!
        assertFalse(evening.onAir(owl))
        assertTrue(Sky(LocalDateTime.of(2026, 10, 22, 2, 30)).onAir(owl))
        // Off the air, the owl's spot on the dial is just static between its neighbours.
        assertNull(Dial.at(owl.khz, evening))
    }

    @Test fun aNightBelongsToItsEvening() {
        assertEquals(LocalDate.of(2026, 10, 21), Sky(LocalDateTime.of(2026, 10, 22, 3, 0)).night)
        assertEquals(LocalDate.of(2026, 10, 21), Sky(LocalDateTime.of(2026, 10, 21, 23, 0)).night)
    }

    @Test fun moonIsFullOnAKnownFullMoon() {
        // Full moon: 25 January 2024, 17:54 UTC.
        assertTrue(abs(Sky.moonAge(LocalDateTime.of(2024, 1, 25, 17, 54)) - Sky.SYNODIC / 2) < 1.0)
        assertTrue(Sky.moonAge(LocalDateTime.of(2024, 1, 11, 11, 57)) < 1.0 || Sky.moonAge(LocalDateTime.of(2024, 1, 11, 11, 57)) > Sky.SYNODIC - 1.0)
    }

    @Test fun stormsComeAboutOneNightInFive() {
        val start = LocalDate.of(2026, 1, 1)
        val n = (0 until 365).count { Sky.stormNight(start.plusDays(it.toLong())) }
        assertTrue("$n storm nights", n in 50..95)
    }

    @Test fun snapFindsTheNearestStation() {
        assertEquals(171f, Dial.snap(170.2f, evening), 0f)
        assertEquals(175.5f, Dial.snap(175.5f, evening), 0f)
    }

    private fun r(id: String, rating: Int?, ended: Long = 0) = Reception(stationId = id, khz = 0f, startedAt = 0, endedAt = ended, minutes = 20, callsign = "", rating = rating)

    @Test fun yourWaveNeedsTwoAnsweredNights() {
        val rs = listOf(
            r("rain", Reception.FAST), r("rain", Reception.FAST), r("rain", Reception.SLOW),
            r("forecast", Reception.FAST), r("forecast", Reception.FAST),
            r("train", Reception.FAST),
        )
        val wave = Log.wave(rs)
        assertEquals(listOf("forecast", "rain"), wave.map { it.station.id })
    }

    @Test fun onlyLastNightsCardWaits() {
        val now = 100 * 3_600_000L
        val rs = listOf(r("rain", null, now - 30 * 3_600_000L), r("light", null, now - 8 * 3_600_000L), r("train", Reception.FAST, now - 2 * 3_600_000L))
        assertEquals("light", Log.pending(rs, now)?.stationId)
    }

    @Test fun everyLineIsRecorded() {
        val assets = File("src/main/assets/voice").takeIf { it.exists() } ?: File("app/src/main/assets/voice")
        for (ru in listOf(false, true)) {
            val s = Script.of(ru)
            val lines = s.forecastIntro + s.seas + s.winds + s.seaStates + s.weather + s.visibility + s.forecastOutro +
                s.rollIntro + s.roll + s.rollSettle + s.rollOutro + s.lotteryIntro + s.words + s.metronomeIntro + s.metronomeFree +
                s.inventoryIntro + s.items + s.inventoryOutro + s.countdownIntro + s.numbers + s.owlIntro + s.owl + s.signOff
            val dir = File(assets, if (ru) "ru" else "en")
            val missing = lines.filter { !File(dir, Narrator.key(it) + ".webm").exists() }
            assertTrue("not recorded: ${missing.take(5)}", missing.isEmpty())
        }
        assertEquals(100, Script.of(true).numbers.size)
    }

    @Test fun everyRareKindHasAStation() {
        Rarity.entries.forEach { r -> assertNotNull(Dial.STATIONS.firstOrNull { it.rarity == r }) }
    }
}
