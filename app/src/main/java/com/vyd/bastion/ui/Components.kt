package com.vyd.bastion.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vyd.bastion.AppEntry
import com.vyd.bastion.AppRepository
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

val Violet = Color(0xFF9B7BFF)
val Cyan = Color(0xFF4FD1FF)
val Mint = Color(0xFF5BE3A8)
val Cream = Color(0xFFEDE9DA)
val Danger = Color(0xFFFF6B81)
val Glass = Color(0x33FFFFFF)
val GlassStroke = Color(0x22FFFFFF)
val SheetBg = Color(0xFF14111E)
val CardBg = Color(0xFF1F1B2C)

val TextShadow = Shadow(Color(0x99000000), Offset(0f, 2f), 6f)

@Composable
fun BastionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet, secondary = Cyan, tertiary = Mint,
            surface = SheetBg, background = Color.Transparent,
            onPrimary = Color.White, surfaceContainerLow = SheetBg,
        ),
        content = content,
    )
}

fun Modifier.glass(shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp)) =
    this.clip(shape).background(Glass).border(1.dp, GlassStroke, shape)

fun androidx.compose.ui.geometry.Rect.toAndroid() =
    android.graphics.Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

/**
 * Animated app icon. Tap = open, hold = options, hold then move = drag (when [canDrag]).
 * [dragSlot] is the home/dock slot it lives in (null when it's in the drawer).
 */
@Composable
fun AppIcon(
    entry: AppEntry,
    repo: AppRepository,
    modifier: Modifier = Modifier,
    size: Dp = 58.dp,
    showLabel: Boolean = true,
    locked: Boolean = false,
    enterIndex: Int = -1,
    canDrag: Boolean = false,
    dragSlot: Int? = null,
    onClick: (android.graphics.Rect?) -> Unit,
    onLongClick: () -> Unit,
) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val bmp by produceState(repo.cachedIcon(entry.key), entry.key) { value = repo.icon(entry, px) }
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(if (pressed) 0.84f else 1f, spring(0.4f, 700f), label = "press")
    val appear = remember { Animatable(if (enterIndex >= 0) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (enterIndex >= 0) {
            delay(enterIndex * 22L)
            appear.animateTo(1f, spring(0.55f, 260f))
        }
    }
    val haptic = LocalHapticFeedback.current
    val drag = LocalDrag.current
    val latestClick by rememberUpdatedState(onClick)
    val latestLong by rememberUpdatedState(onLongClick)
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    Column(
        modifier
            .graphicsLayer {
                val a = appear.value
                alpha = a.coerceIn(0f, 1f)
                val s = (0.5f + 0.5f * a) * press
                scaleX = s; scaleY = s
                translationY = (1f - a) * 36.dp.toPx()
                rotationZ = (1f - a) * -8f
            }
            .onGloballyPositioned { coords[0] = it }
            .pointerInput(entry.key, canDrag, dragSlot) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    pressed = true
                    val lp = awaitLongPressOrCancellation(down.id)
                    if (lp == null) {
                        pressed = false
                        val c = currentEvent.changes.firstOrNull { it.id == down.id }
                        if (c != null && c.changedToUpIgnoreConsumed()) {
                            c.consume()
                            latestClick(coords[0]?.takeIf { it.isAttached }?.boundsInWindow()?.toAndroid())
                        }
                        return@awaitEachGesture
                    }
                    lp.consume()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    var moved = 0f
                    var dragging = false
                    fun windowPos(local: Offset): Offset? =
                        coords[0]?.takeIf { it.isAttached }?.localToWindow(local)
                    while (true) {
                        val e = awaitPointerEvent()
                        val c = e.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) { c.consume(); break }
                        val d = c.positionChange()
                        c.consume()
                        moved += d.getDistance()
                        if (!dragging && canDrag && moved > viewConfiguration.touchSlop) {
                            dragging = true
                            pressed = false
                            drag.start(entry, dragSlot, windowPos(c.position) ?: Offset.Zero)
                        } else if (dragging) {
                            drag.pos = windowPos(c.position) ?: (drag.pos + d)
                        }
                    }
                    pressed = false
                    if (dragging) drag.end() else latestLong()
                }
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconImage(entry, bmp, size, locked)
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(
                entry.label, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                style = TextStyle(color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, shadow = TextShadow),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
fun IconImage(entry: AppEntry, bmp: androidx.compose.ui.graphics.ImageBitmap?, size: Dp, locked: Boolean) {
    Box {
        if (bmp != null) Image(bmp, entry.label, Modifier.size(size))
        else Box(Modifier.size(size).clip(RoundedCornerShape(18.dp)).background(Glass))
        if (locked) {
            Box(
                Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(22.dp)
                    .clip(CircleShape).background(Brush.linearGradient(listOf(Violet, Cyan)))
                    .border(2.dp, Color(0xFF14111E), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Lock, null, Modifier.size(12.dp), tint = Color.White) }
        }
    }
}

/** Slow-drifting aurora blobs layered over the wallpaper. */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "aurora")
    val phase by t.animateFloat(0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "phase")
    Canvas(modifier) {
        val w = size.width; val h = size.height
        fun blob(c: Color, cx: Float, cy: Float, r: Float) =
            drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
        blob(Violet.copy(alpha = 0.45f), w * (0.25f + 0.15f * cos(phase)), h * (0.2f + 0.08f * sin(phase)), w * 0.9f)
        blob(Cyan.copy(alpha = 0.30f), w * (0.8f + 0.12f * sin(phase * 1.3f)), h * (0.55f + 0.1f * cos(phase)), w * 0.8f)
        blob(Color(0xFFFF5FA2).copy(alpha = 0.22f), w * (0.4f + 0.2f * sin(phase * 0.7f)), h * (0.92f + 0.05f * cos(phase * 1.7f)), w * 0.7f)
    }
}

/** Pulsing shield with expanding radar rings. */
@Composable
fun ShieldPulse(size: Dp, active: Boolean, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val t = rememberInfiniteTransition(label = "pulse")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing)), label = "ring")
    val ring2 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, delayMillis = 1100, easing = FastOutSlowInEasing)), label = "ring2")
    val breathe by t.animateFloat(0.94f, 1.04f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "breathe")
    val tint = if (active) Mint else Violet
    Box(modifier.size(size * 2f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size * 2f)) {
            listOf(ring, ring2).forEach { p ->
                drawCircle(tint.copy(alpha = (1f - p) * 0.5f), radius = this.size.minDimension / 2f * (0.45f + 0.55f * p),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
            }
        }
        Box(
            Modifier.size(size).graphicsLayer { scaleX = breathe; scaleY = breathe }
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(tint, if (active) Cyan else Color(0xFF6C4BFF)))),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(size * 0.5f), tint = Color.White) }
    }
}

@Composable
fun SectionLabel(text: String) = Text(
    text.uppercase(), color = Violet.copy(alpha = 0.9f), fontSize = 12.sp, letterSpacing = 1.6.sp,
    fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 24.dp, top = 22.dp, bottom = 8.dp),
)

@Composable
fun Centered(content: @Composable () -> Unit) = Column(
    Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
) { content() }

/** Walks ContextWrappers (dialogs/sheets wrap the activity) to find the real activity. */
fun android.content.Context.findActivity(): androidx.fragment.app.FragmentActivity {
    var c: android.content.Context = this
    while (c is android.content.ContextWrapper) {
        if (c is androidx.fragment.app.FragmentActivity) return c
        c = c.baseContext
    }
    error("No activity")
}
