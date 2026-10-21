package app.longwave.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * A night the radio went off the air by itself — so, most likely, a night you fell asleep to it.
 * Each becomes a QSL card from the station that was playing. [rating] is the morning's answer:
 * null until asked, then [FAST], [SLOW] or [DONT_KNOW].
 */
@Entity(tableName = "receptions")
data class Reception(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stationId: String,
    val khz: Float,
    val startedAt: Long,
    val endedAt: Long,
    /** Minutes the radio played before it went off the air. */
    val minutes: Int,
    /** The listener's call sign as it was that night. */
    val callsign: String,
    val rating: Int? = null,
) {
    companion object {
        const val FAST = 0
        const val SLOW = 1
        const val DONT_KNOW = 2
    }
}

@Dao
interface LogDao {
    @Query("SELECT * FROM receptions ORDER BY endedAt DESC")
    fun receptions(): Flow<List<Reception>>

    @Query("SELECT * FROM receptions WHERE id = :id")
    suspend fun reception(id: Long): Reception?

    @Insert
    suspend fun add(r: Reception): Long

    @Query("UPDATE receptions SET rating = :rating WHERE id = :id")
    suspend fun rate(id: Long, rating: Int)

    @Query("DELETE FROM receptions")
    suspend fun clear()
}

@Database(entities = [Reception::class], version = 1, exportSchema = true)
abstract class LongwaveDatabase : RoomDatabase() {
    abstract fun dao(): LogDao

    companion object {
        fun create(context: Context) = Room.databaseBuilder(context, LongwaveDatabase::class.java, "longwave.db").build()
    }
}
