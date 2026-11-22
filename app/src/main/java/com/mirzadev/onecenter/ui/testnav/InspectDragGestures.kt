/*
 * Adapted from Kyant0/AndroidLiquidGlass catalog
 */
package com.mirzadev.onecenter.ui.testnav

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange

fun Modifier.inspectDragGestures(
    key1: Any? = null,
    key2: Any? = null,
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit = { _, _ -> }
): Modifier = this.pointerInput(key1, key2) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onDragStart(down.position)
        val dragged = drag(down.id) { change ->
            onDrag(change, change.positionChange())
        }
        if (dragged) {
            onDragEnd()
        } else {
            onDragCancel()
        }
    }
}
