package com.vyd.bastion.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Bottom panel drawn inside the launcher's own window (no separate dialog window).
 * Tap the dim background or drag the handle down to close.
 */
@Composable
fun Panel(
    visible: Boolean,
    onDismiss: () -> Unit,
    heightFraction: Float? = 0.94f,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                    .clickable(remember { MutableInteractionSource() }, null) { onDismiss() }
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(0.85f, 380f)) { it },
            exit = slideOutVertically { it },
        ) {
            var dragY by remember { mutableFloatStateOf(0f) }
            Column(
                Modifier.fillMaxWidth()
                    .statusBarsPadding()
                    .then(if (heightFraction != null) Modifier.fillMaxHeight(heightFraction) else Modifier)
                    .graphicsLayer { translationY = dragY.coerceAtLeast(0f) }
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(SheetBg)
                    .pointerInput(Unit) { detectTapGestures { } } // swallow taps on empty space (no semantics merge)
                    .navigationBarsPadding(),
            ) {
                Box(
                    Modifier.fillMaxWidth().height(26.dp).draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { dragY += it },
                        onDragStopped = { if (dragY > 220f) onDismiss(); dragY = 0f },
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(width = 40.dp, height = 4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                }
                content()
            }
        }
    }
}
