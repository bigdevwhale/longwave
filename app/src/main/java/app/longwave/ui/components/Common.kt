package app.longwave.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.longwave.R
import app.longwave.ui.theme.LocalPalette
import app.longwave.ui.theme.LocalType

/** A quiet top bar for the screens away from the radio: back, and a serif title. */
@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), tint = p.muted) }
        Text(title, style = LocalType.current.title, color = p.ink, modifier = Modifier.padding(start = 4.dp))
    }
}

/** A warm, rounded button: [strong] fills it with the dial's amber. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, strong: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (strong) p.accent else p.surface)
            .border(1.dp, if (strong) p.accent else p.line, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = LocalType.current.body, color = if (strong) p.woodDark else p.ink, textAlign = TextAlign.Center)
    }
}

/** A small uppercase heading over a group. */
@Composable
fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = LocalType.current.label, color = LocalPalette.current.faint, modifier = modifier.padding(top = 22.dp, bottom = 8.dp))
}
