package app.longwave

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.longwave.audio.Engine
import app.longwave.audio.Narrator
import app.longwave.data.DemoSeed
import app.longwave.data.LongwaveDatabase
import app.longwave.data.SettingsStore
import app.longwave.radio.Radio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(val context: Context) {
    val db = LongwaveDatabase.create(context)
    val settings = SettingsStore(context)
    val narrator = Narrator(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }
}

class LongwaveApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Engine.init(this)
        Radio.init(container)
        container.scope.launch { DemoSeed.seedIfEmpty(container, localized().ru) }
    }
}

val Context.container: AppContainer get() = (applicationContext as LongwaveApp).container

/** Resources in the in-app language, if the app speaks one on this Android. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
