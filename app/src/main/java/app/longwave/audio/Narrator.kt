package app.longwave.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlin.coroutines.resume

/**
 * The voices of the stations: every line was recorded ahead of time by a neural voice
 * (tools/narrate.py) and ships in assets/voice/<lang>/<hash>.webm, the hash taken from the exact
 * text. Its loudness follows the tuning, so a voice sinks into the static like the rest of the
 * station when the needle moves off it.
 */
class Narrator(context: Context) {
    private val app = context.applicationContext
    private val index = HashMap<String, Set<String>>()
    @Volatile private var player: MediaPlayer? = null
    @Volatile private var volume = 1f

    private fun available(lang: String): Set<String> = synchronized(index) {
        index.getOrPut(lang) { runCatching { app.assets.list("voice/$lang")?.toSet() }.getOrNull().orEmpty() }
    }

    fun has(text: String, lang: String): Boolean = "${key(text)}.webm" in available(lang)

    /** Plays the recording of [text] and suspends until it ends; false when there is none. */
    suspend fun say(text: String, lang: String): Boolean {
        val name = "${key(text)}.webm"
        if (name !in available(lang)) return false
        val fd = withContext(Dispatchers.IO) { runCatching { app.assets.openFd("voice/$lang/$name") }.getOrNull() } ?: return false
        stop()
        val mp = MediaPlayer()
        player = mp
        try {
            suspendCancellableCoroutine { cont ->
                val ok = runCatching {
                    mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    fd.close()
                    mp.setOnCompletionListener { if (cont.isActive) cont.resume(Unit) }
                    mp.setOnErrorListener { _, _, _ -> if (cont.isActive) cont.resume(Unit); true }
                    mp.prepare()
                    mp.setVolume(volume, volume)
                    mp.start()
                }.isSuccess
                if (!ok && cont.isActive) cont.resume(Unit)
                cont.invokeOnCancellation { runCatching { mp.stop() } }
            }
        } finally {
            runCatching { mp.release() }
            if (player === mp) player = null
        }
        return true
    }

    /** 0..1, applied at once to whatever is being said. */
    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        player?.let { runCatching { it.setVolume(volume, volume) } }
    }

    fun stop() {
        player?.let { runCatching { it.stop() } }
    }

    companion object {
        /** Must match tools/narrate.py: first 16 hex digits of SHA-1 over the trimmed UTF-8 text. */
        fun key(text: String): String =
            MessageDigest.getInstance("SHA-1").digest(text.trim().toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
    }
}
