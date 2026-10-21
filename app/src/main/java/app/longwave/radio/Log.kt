package app.longwave.radio

import app.longwave.data.Reception

/** One station in "your wave": how many nights it played you to sleep, and how many of them fast. */
data class WaveStat(val station: Station, val nights: Int, val fast: Int, val slow: Int)

object Log {
    /** How long a card may wait for its morning question: the next day's evening is too late. */
    const val CARD_WAITS_MS = 20 * 3_600_000L

    /** The card that came in the night and hasn't been asked about yet, if any. */
    fun pending(rs: List<Reception>, now: Long): Reception? =
        rs.filter { it.rating == null && now - it.endedAt in 0..CARD_WAITS_MS }.maxByOrNull { it.endedAt }

    /** Stations that have sent at least one card, in dial order. */
    fun collected(rs: List<Reception>): List<Station> {
        val ids = rs.map { it.stationId }.toSet()
        return Dial.STATIONS.filter { it.id in ids }
    }

    /**
     * The stations you fall asleep to fastest. A station needs two answered nights to count, and
     * a share of fast nights is evened out towards a half, so one lucky night can't top the list.
     */
    fun wave(rs: List<Reception>): List<WaveStat> =
        rs.groupBy { it.stationId }.mapNotNull { (id, nights) ->
            val station = Dial.byId(id) ?: return@mapNotNull null
            val fast = nights.count { it.rating == Reception.FAST }
            val slow = nights.count { it.rating == Reception.SLOW }
            WaveStat(station, nights.size, fast, slow).takeIf { fast + slow >= 2 }
        }.sortedWith(compareByDescending<WaveStat> { (it.fast + 1.0) / (it.fast + it.slow + 2.0) }.thenByDescending { it.nights })
}
