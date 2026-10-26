package com.vyd.bastion.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import com.vyd.bastion.AppEntry

/** Shared state for dragging an app icon around the home screen. Positions are window coordinates. */
class DragState {
    var entry by mutableStateOf<AppEntry?>(null); private set
    var fromSlot by mutableStateOf<Int?>(null); private set
    var pos by mutableStateOf(Offset.Zero)

    /** Drop targets: slot -> live coordinates (bounds are measured at hover/drop time). */
    val targets = HashMap<Int, LayoutCoordinates>()
    var removeZone: LayoutCoordinates? = null

    var onStart: (AppEntry, Int?) -> Unit = { _, _ -> }
    var onDrop: (AppEntry, Int?, Offset) -> Unit = { _, _, _ -> }

    val active get() = entry != null

    fun start(e: AppEntry, slot: Int?, at: Offset) {
        entry = e; fromSlot = slot; pos = at
        onStart(e, slot)
    }

    fun end() {
        val e = entry ?: return
        val from = fromSlot
        val at = pos
        entry = null; fromSlot = null
        onDrop(e, from, at)
    }

    fun slotAt(p: Offset): Int? =
        targets.entries.firstOrNull { it.value.isAttached && it.value.boundsInWindow().contains(p) }?.key
    fun removeAt(p: Offset): Boolean =
        removeZone?.takeIf { it.isAttached }?.boundsInWindow()?.inflate(40f)?.contains(p) == true
    fun hoverSlot(): Int? = slotAt(pos)
    fun overRemove(): Boolean = removeAt(pos)
}

val LocalDrag = staticCompositionLocalOf { DragState() }
