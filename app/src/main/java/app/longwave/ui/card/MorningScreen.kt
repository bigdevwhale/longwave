package app.longwave.ui.card

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.longwave.R
import app.longwave.container
import app.longwave.data.Reception
import app.longwave.ui.components.Pill
import app.longwave.ui.theme.LocalPalette
import app.longwave.ui.theme.LocalType

/**
 * The morning after: a card came in the night from the station you fell asleep to. One
 * question, three answers, and over time the log learns which stations work for you.
 */
@Composable
fun MorningScreen(r: Reception, onDone: () -> Unit, still: Boolean = false) {
    val p = LocalPalette.current
    val type = LocalType.current
    val context = LocalContext.current
    // The card drops in like post through the door.
    val drop = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { drop.animateTo(1f, tween(900)) }
    fun answer(v: Int) {
        context.container.write { db.dao().rate(r.id, v) }
        onDone()
    }
    Column(
        Modifier.fillMaxSize().background(p.bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.5f))
        Text(stringResource(R.string.morning_kicker).uppercase(), style = type.label, color = p.accent)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.morning_title), style = type.display, color = p.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        QslCard(
            r,
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = (1f - drop.value) * -120.dp.toPx()
                    rotationZ = -3f + 2f * drop.value
                    alpha = drop.value
                },
        )
        Spacer(Modifier.height(44.dp))
        Text(stringResource(R.string.morning_question), style = type.title, color = p.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Pill(stringResource(R.string.answer_fast), Modifier.fillMaxWidth(), strong = true) { answer(Reception.FAST) }
            Pill(stringResource(R.string.answer_slow), Modifier.fillMaxWidth()) { answer(Reception.SLOW) }
            Pill(stringResource(R.string.answer_dont_know), Modifier.fillMaxWidth()) { answer(Reception.DONT_KNOW) }
        }
        Spacer(Modifier.weight(1f))
    }
}
