package app.longwave.ui.radio

import android.Manifest
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.atan2
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.longwave.BuildConfig
import app.longwave.R
import app.longwave.audio.Engine
import app.longwave.container
import app.longwave.data.RadioSettings
import app.longwave.radio.Dial
import app.longwave.radio.Radio
import app.longwave.radio.Sky
import app.longwave.radio.khzLabel
import app.longwave.ru
import app.longwave.ui.theme.LocalPalette
import app.longwave.ui.theme.LocalType
import app.longwave.ui.theme.Prata
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.LocalDateTime
import java.util.Date
import kotlin.math.abs
import kotlin.math.floor

private val TIMERS = listOf(15, 30, 45, 60, 90, 0)

/**
 * The radio on the nightstand. Drag the dial or the big knob to tune (with the eyes closed, the
 * phone ticks under the finger and knocks once on a station), press the left knob to switch on,
 * pick when it goes off the air. While it plays and no one touches it, the room goes dark.
 */
@Composable
fun RadioScreen(settings: RadioSettings, hasCard: Boolean, onLog: () -> Unit, onSettings: () -> Unit, still: Boolean = false) {
    val p = LocalPalette.current
    val type = LocalType.current
    val context = LocalContext.current
    val view = LocalView.current
    val ru = context.ru
    val scope = rememberCoroutineScope()
    val radio by Radio.state.collectAsStateWithLifecycle()

    val khz = remember { Animatable(settings.khz) }
    var sky by remember { mutableStateOf(Sky(LocalDateTime.now(), BuildConfig.DEMO)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            sky = Sky(LocalDateTime.now(), BuildConfig.DEMO)
        }
    }

    // What the engine is doing, read every frame while the radio plays.
    var drift by remember { mutableStateOf(0f) }
    var signal by remember { mutableStateOf(0f) }
    var level by remember { mutableStateOf(0f) }
    LaunchedEffect(radio.on) {
        if (still) {
            // A picture of the radio playing, for the screen renders.
            signal = Dial.at(khz.value, sky)?.second ?: 0f
            level = 0.6f
            return@LaunchedEffect
        }
        if (!radio.on) {
            drift = 0f; signal = 0f; level = 0f
            return@LaunchedEffect
        }
        while (true) {
            withFrameNanos { }
            drift = Engine.drift
            signal = Engine.signal
            level = Engine.level
        }
    }
    val glow by animateFloatAsState(if (radio.on || still) 1f else 0f, tween(if (radio.on) 2600 else 900), label = "valves")

    // Ticks under the finger, a knock on a station.
    val haptics by rememberUpdatedState(settings.haptics)
    LaunchedEffect(Unit) {
        var lastK = floor(khz.value).toInt()
        var locked = Dial.at(khz.value, sky)?.second?.let { it > 0.97f } ?: false
        snapshotFlow { khz.value }.collect { v ->
            Radio.tune(v)
            if (!haptics) return@collect
            val k = floor(v).toInt()
            if (k != lastK) {
                lastK = k
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            val now = Dial.at(v, sky)?.let { abs(it.first.khz - v) < 0.3f } ?: false
            if (now && !locked) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            locked = now
        }
    }

    // Where the dial is heading: always a station, so a drag, a fling, a tap or a click of the
    // knob all land on one and never strand the needle in the static.
    var target by remember { mutableStateOf(Dial.nearest(settings.khz, sky)) }
    val ppk = DP_PER_KHZ * view.resources.displayMetrics.density

    fun goTo(to: Float, velocity: Float = 0f) {
        target = to
        scope.launch {
            // Unhurried, like a hand on a real knob: the needle glides through the static between.
            if (velocity == 0f) khz.animateTo(to, tween(1200, easing = FastOutSlowInEasing))
            else khz.animateTo(to, spring(dampingRatio = 1f, stiffness = 40f), initialVelocity = velocity)
        }
        context.container.write { this.settings.setKhz(to) }
    }

    fun step(dir: Int) {
        // From where the dial is heading if it's on its way, otherwise from where it stands.
        val from = if (khz.isRunning) target else khz.value
        val to = Dial.next(from, dir, sky)
        if (to != from || abs(khz.value - to) > 0.01f) goTo(to)
    }

    // The finger's own idea of the dial, kept in step with every move, so the release below never
    // reads a position the animation hasn't caught up with yet.
    var dragAt by remember { mutableStateOf(settings.khz) }

    fun dragBy(dx: Float) {
        dragAt = (dragAt - dx / ppk).coerceIn(Dial.MIN, Dial.MAX)
        val v = dragAt
        scope.launch { khz.snapTo(v) }
    }

    /** After a drag: carry on the way the finger was flicking for a moment, then settle on a station. */
    fun release(pxPerSecond: Float) {
        val v = (-pxPerSecond / ppk).takeIf { it.isFinite() }?.coerceIn(-60f, 60f) ?: 0f
        val projected = (dragAt + v * 0.3f).coerceIn(Dial.MIN, Dial.MAX)
        goTo(Dial.nearest(projected, sky), v)
    }

    var knobAngle by remember { mutableStateOf((settings.khz - Dial.MIN) * 4f) }

    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { Radio.switchOn(context) }
    fun power() {
        if (radio.on) Radio.switchOff(context)
        else if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notify.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else Radio.switchOn(context)
    }

    // The lamps go out when nobody has touched the screen for a while.
    var touched by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var dark by remember { mutableStateOf(false) }
    LaunchedEffect(radio.on, touched) {
        dark = false
        if (!radio.on || still) return@LaunchedEffect
        delay(25_000)
        dark = true
    }

    val here = Dial.at(khz.value + drift, sky)
    val station = here?.first?.takeIf { here.second > 0.35f }

    Box(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    touched = System.currentTimeMillis()
                    // A touch while the station drifts away means someone is still awake.
                    if (Radio.state.value.signingOff) Radio.extend(15)
                }
            },
    ) {
        // A dim lamp somewhere above the nightstand.
        Canvas(Modifier.fillMaxSize()) {
            drawRect(androidx.compose.ui.graphics.Brush.radialGradient(listOf(p.accent.copy(alpha = 0.05f + 0.07f * glow), Color.Transparent), center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height * 0.33f), radius = size.width))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = onLog) { Icon(Icons.Outlined.MenuBook, stringResource(R.string.log), tint = p.muted) }
                    if (hasCard) Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(8.dp).clip(CircleShape).background(p.accent))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings), tint = p.muted) }
            }

            // The set itself.
            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Canvas(Modifier.matchParentSize()) { drawWood(p, 26.dp.toPx()) }
                Column(Modifier.padding(14.dp)) {
                    val plate = rememberTextMeasurer()
                    // The speaker takes whatever height the phone has to spare.
                    Canvas(Modifier.fillMaxWidth().weight(1f).heightIn(min = 72.dp)) {
                        drawCloth(p, 14.dp.toPx())
                        val w = 196.dp.toPx()
                        val h = 30.dp.toPx()
                        drawRoundRect(
                            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(p.brass, p.brassDark)),
                            topLeft = androidx.compose.ui.geometry.Offset(center.x - w / 2, center.y - h / 2),
                            size = androidx.compose.ui.geometry.Size(w, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        )
                        drawCentred(plate, if (ru) "ДЛИННАЯ ВОЛНА" else "LONGWAVE", TextStyle(fontFamily = Prata, fontSize = 15.sp, letterSpacing = 3.sp, color = p.woodDark), center)
                    }
                    Spacer(Modifier.height(12.dp))
                    DialGlass(
                        khz = khz.value, needle = drift, glow = glow, sky = sky, found = settings.found, ru = ru,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .semantics { contentDescription = context.getString(R.string.dial_description) }
                            .pointerInput(Unit) {
                                val tracker = VelocityTracker()
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        tracker.resetTracking()
                                        dragAt = khz.value
                                    },
                                    onDragEnd = { release(tracker.calculateVelocity().x) },
                                    onDragCancel = { release(0f) },
                                ) { ch, dx ->
                                    ch.consume()
                                    tracker.addPosition(ch.uptimeMillis, ch.position)
                                    dragBy(dx)
                                }
                            }
                            .pointerInput(Unit) {
                                // A tap on the glass tunes to the station painted there.
                                detectTapGestures { at ->
                                    val near = Dial.nearest(khz.value + (at.x - size.width / 2f) / ppk, sky)
                                    goTo(near)
                                }
                            },
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val lamp by animateFloatAsState(if (radio.on || still) 1f else 0f, tween(400), label = "lamp")
                            // The pilot lamp above the power knob.
                            Canvas(Modifier.size(18.dp)) {
                                drawCircle(Color(0xFF2A0F08), radius = 4.dp.toPx())
                                drawCircle(Color(0xFFFF7A3A).copy(alpha = lamp), radius = 3.dp.toPx())
                                drawCircle(Color(0xFFFF7A3A).copy(alpha = 0.3f * lamp), radius = 9.dp.toPx())
                            }
                            Canvas(
                                Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .semantics { contentDescription = context.getString(if (radio.on) R.string.switch_off else R.string.switch_on) }
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { power() },
                            ) { drawKnob(p, if (radio.on) 40f else -40f) }
                            Text(stringResource(R.string.knob_power), style = type.label.copy(fontSize = 9.sp), color = p.brass.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Canvas(Modifier.size(54.dp)) { drawEye(p, signal, level, glow) }
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Canvas(
                                Modifier
                                    .size(84.dp)
                                    .semantics { contentDescription = context.getString(R.string.knob_tuning) }
                                    .pointerInput(Unit) {
                                        // Turned round like a real knob: clockwise goes up the band,
                                        // one station for every click of about 35 degrees.
                                        var turned = 0f
                                        detectDragGestures(onDragStart = { turned = 0f }) { ch, _ ->
                                            ch.consume()
                                            val c = Offset(size.width / 2f, size.height / 2f)
                                            val a0 = atan2(ch.previousPosition.y - c.y, ch.previousPosition.x - c.x)
                                            val a1 = atan2(ch.position.y - c.y, ch.position.x - c.x)
                                            var d = Math.toDegrees((a1 - a0).toDouble()).toFloat()
                                            if (d > 180f) d -= 360f
                                            if (d < -180f) d += 360f
                                            turned += d
                                            knobAngle += d
                                            while (turned >= 35f) { turned -= 35f; step(1) }
                                            while (turned <= -35f) { turned += 35f; step(-1) }
                                        }
                                    },
                            ) { drawKnob(p, knobAngle) }
                            Text(stringResource(R.string.knob_tuning), style = type.label.copy(fontSize = 9.sp), color = p.brass.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            // What's on.
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp)) {
                val kind = when {
                    station == null -> stringResource(R.string.kind_static)
                    station.rare -> stringResource(R.string.kind_rare)
                    station.programme != null -> stringResource(R.string.kind_programme)
                    else -> stringResource(R.string.kind_sound)
                }
                Text("${khzLabel(Math.round((khz.value + drift) * 2) / 2f, ru)} · $kind".uppercase(), style = type.label, color = if (station?.rare == true) Color(0xFFB79AD6) else p.accent.copy(alpha = 0.85f))
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(station?.name(ru) ?: stringResource(R.string.between_stations), style = type.display.copy(fontSize = 26.sp, lineHeight = 34.sp), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { step(-1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.station_prev), tint = p.muted) }
                    IconButton(onClick = { step(1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.station_next), tint = p.muted) }
                }
                Text(station?.about(ru) ?: stringResource(R.string.between_about), style = type.body, color = p.muted, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }

            // When it goes off the air.
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 18.dp)) {
                Text(stringResource(R.string.off_air).uppercase(), style = type.label, color = p.faint, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    TIMERS.forEach { m ->
                        val on = settings.timer == m
                        Box(
                            Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (on) p.accent.copy(alpha = 0.18f) else p.surface)
                                .border(1.dp, if (on) p.accent.copy(alpha = 0.7f) else p.line, RoundedCornerShape(20.dp))
                                .clickable {
                                    context.container.write { this.settings.setTimer(m) }
                                    Radio.setTimer(m)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (m == 0) "∞" else m.toString(), style = type.body.copy(fontSize = 15.sp), color = if (on) p.ink else p.muted, textAlign = TextAlign.Center)
                        }
                    }
                }
                val status = when {
                    !radio.on -> stringResource(if (settings.timer == 0) R.string.timer_none else R.string.timer_minutes, settings.timer)
                    radio.signingOff -> stringResource(R.string.signing_off)
                    radio.offAirAt > 0 -> stringResource(R.string.off_air_at, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(radio.offAirAt)))
                    else -> stringResource(R.string.timer_none)
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(status, style = type.small, color = if (radio.signingOff) p.accent else p.muted, modifier = Modifier.weight(1f))
                    if (radio.on && radio.offAirAt > 0) {
                        Text(
                            stringResource(R.string.plus_15),
                            style = type.small,
                            color = p.accent,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { Radio.extend(15) }.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        // Lamps out: the screen goes almost black, the dial's last glow under it.
        AnimatedVisibility(dark, enter = fadeIn(tween(4000)), exit = fadeOut(tween(300))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.93f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { touched = System.currentTimeMillis() },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(stringResource(R.string.tap_to_wake), style = type.small, color = p.faint.copy(alpha = 0.5f), modifier = Modifier.navigationBarsPadding().padding(bottom = 28.dp))
            }
        }
    }
}
