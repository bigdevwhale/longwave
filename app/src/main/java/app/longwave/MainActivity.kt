package app.longwave

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.longwave.data.RadioSettings
import app.longwave.ui.Root
import app.longwave.ui.theme.LongwaveTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A dark bedroom: light icons on dark bars.
        val style = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        setContent {
            val settings: RadioSettings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            LongwaveTheme {
                // Wait for settings so the welcome doesn't flash for someone already here.
                settings?.let { Root(it) }
            }
        }
    }
}
