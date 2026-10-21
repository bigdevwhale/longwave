package app.longwave

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import app.longwave.data.DemoSeed
import app.longwave.data.RadioSettings
import app.longwave.data.Reception
import app.longwave.radio.Dial
import app.longwave.radio.Log
import app.longwave.ui.card.MorningScreen
import app.longwave.ui.card.QslCard
import app.longwave.ui.log.LogScreen
import app.longwave.ui.onboarding.Welcome
import app.longwave.ui.radio.RadioScreen
import app.longwave.ui.settings.SettingsScreen
import app.longwave.ui.theme.LongwaveTheme
import app.longwave.ui.theme.Night
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every screen over the demo log, drawn to PNGs (run with `-Prender`): a look without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    private fun data(): Pair<RadioSettings, List<Reception>> = runBlocking {
        val app = ApplicationProvider.getApplicationContext<LongwaveApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(c, app.ru, force = true)
        c.settings.now() to c.db.dao().receptions().first()
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { LongwaveTheme { content() } }
        rule.mainClock.advanceTimeBy(4_000)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(4_000)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun welcome() = shot("0-welcome") { Welcome() }

    @Test fun radio() {
        val (s, _) = data()
        shot("1-radio") { RadioScreen(s.copy(khz = 171f), hasCard = false, onLog = {}, onSettings = {}, still = true) }
    }

    @Test fun radioRain() {
        val (s, _) = data()
        shot("2-radio-rain") { RadioScreen(s.copy(khz = 153f, timer = 30), hasCard = false, onLog = {}, onSettings = {}, still = true) }
    }

    @Test fun radioBetween() {
        val (s, _) = data()
        shot("3-radio-between") { RadioScreen(s.copy(khz = 211.5f), hasCard = false, onLog = {}, onSettings = {}, still = true) }
    }

    @Test fun morning() {
        val (_, rs) = data()
        val r = Log.pending(rs, System.currentTimeMillis())!!
        shot("4-morning") { MorningScreen(r, onDone = {}, still = true) }
    }

    @Test fun log() {
        val (_, rs) = data()
        shot("5-log") { LogScreen(rs, onBack = {}) }
    }

    @Test fun settings() {
        val (s, _) = data()
        shot("6-settings") { SettingsScreen(s, onBack = {}) }
    }
}

class RenderScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderScreensRu : ScreensBase() {
    override val lang = "ru"
}

/** Every station's QSL card, six to a sheet, to check the emblems. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w411dp-h1700dp-xxhdpi")
class RenderCards {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun sheet(n: Int) {
        val out = File(System.getProperty("render.out") ?: "build/art", "cards").apply { mkdirs() }
        rule.setContent {
            LongwaveTheme {
                androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().background(Night.bg).padding(12.dp)) {
                    Dial.STATIONS.drop(n * 6).take(6).forEachIndexed { i, st ->
                        val r = Reception(id = i.toLong(), stationId = st.id, khz = st.khz, startedAt = 1_760_000_000_000L, endedAt = 1_760_001_800_000L, minutes = 30, callsign = "Лис-7")
                        QslCard(r, Modifier.fillMaxWidth().padding(4.dp))
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "cards-$n.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun sheet0() = sheet(0)
    @Test fun sheet1() = sheet(1)
    @Test fun sheet2() = sheet(2)
}

/** The launcher icon on its walnut square, for the README and the Stillcraft page. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderLogo {
    @Test fun logo() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val size = 512
        val image = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(image)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4A2A18.toInt() }
        canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), size * 0.22f, size * 0.22f, paint)
        // The foreground is drawn for a 108dp canvas with a 72dp visible area.
        val fg = androidx.core.content.ContextCompat.getDrawable(ctx, R.drawable.ic_launcher_foreground)!!
        val pad = (size * 18f / 72f).toInt()
        fg.setBounds(-pad, -pad, size + pad, size + pad)
        fg.draw(canvas)
        val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }
        File(out, "logo.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
