package app.longwave.radio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.longwave.MainActivity
import app.longwave.R
import app.longwave.audio.Engine
import app.longwave.localized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Keeps the radio playing with the screen off: a media foreground service with a quiet
 * notification (what's on, when it goes off the air, +15 min, switch off). It gives way to calls
 * and other sounds, and holds the processor awake only until the timer would have ended anyway.
 */
class RadioService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wake: PowerManager.WakeLock? = null
    private var focus: AudioFocusRequest? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        channel(this)
        ServiceCompat.startForeground(this, ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        requestFocus()
        wake = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "longwave:radio").apply {
            setReferenceCounted(false)
            acquire(4 * 3_600_000L)
        }
        scope.launch {
            var shown = ""
            while (true) {
                val st = Radio.state.value
                if (!st.on) break
                val text = text()
                if (text != shown) {
                    shown = text
                    getSystemService(NotificationManager::class.java).notify(ID, notification())
                }
                delay(1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_EXTEND -> Radio.extend(15)
            ACTION_OFF -> Radio.switchOff(this)
        }
        // The radio doesn't come back on by itself after being killed in the night.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        focus?.let { (getSystemService(AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(it) }
        wake?.let { if (it.isHeld) it.release() }
        if (Radio.state.value.on) Radio.switchOff(this)
        super.onDestroy()
    }

    private fun requestFocus() {
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS -> Radio.switchOff(this)
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        Radio.muted = true
                        Engine.fade = 0f
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        Radio.muted = false
                        if (!Radio.state.value.signingOff) Engine.fade = 1f
                    }
                }
            }
            .build()
        focus = req
        (getSystemService(AUDIO_SERVICE) as AudioManager).requestAudioFocus(req)
    }

    private fun text(): String {
        val res = localized().resources
        val st = Radio.state.value
        val ru = localized().resources.configuration.locales[0].language == "ru"
        val station = Engine.station?.takeIf { Engine.signal > 0.4f }
        val on = station?.let { "${it.name(ru)} · ${khzLabel(it.khz, ru)}" } ?: res.getString(R.string.between_stations)
        return when {
            st.signingOff -> "$on — ${res.getString(R.string.going_off_air)}"
            st.offAirAt > 0 -> "$on — ${res.getString(R.string.off_air_at, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(st.offAirAt)))}"
            else -> on
        }
    }

    private fun notification(): Notification {
        val res = localized().resources
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        fun action(a: String) = PendingIntent.getService(this, a.hashCode(), Intent(this, RadioService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(res.getString(R.string.app_name))
            .setContentText(text())
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, res.getString(R.string.plus_15), action(ACTION_EXTEND))
            .addAction(0, res.getString(R.string.switch_off), action(ACTION_OFF))
            .build()
    }

    companion object {
        private const val ID = 1
        private const val CHANNEL = "radio"
        const val ACTION_EXTEND = "app.longwave.EXTEND"
        const val ACTION_OFF = "app.longwave.OFF"

        fun channel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val name = context.localized().getString(R.string.channel_radio)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, name, NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        }
    }
}
