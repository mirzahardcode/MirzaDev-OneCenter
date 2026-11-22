/*
 * Adapted from Kyant0/AndroidLiquidGlass catalog
 */
package com.mirzadev.onecenter.ui.testnav

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest

val LocalLiquidBottomTabScale = staticCompositionLocalOf<() -> Float> { { 1f } }

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Float,
    onTabSelected: (Int) -> Unit,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    var rowWidthPx by remember { mutableFloatStateOf(0f) }
    val tabWidthPx = if (tabsCount > 0 && rowWidthPx > 0f) rowWidthPx / tabsCount else 0f

    // Logging di paling atas body LiquidBottomTabs
    Log.d("TabTest", "LiquidBottomTabs composed, tabWidth=$tabWidthPx") // TODO remove

    val animationScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragTarget by remember { mutableFloatStateOf(selectedTabIndex()) }
    val latestTabWidthPx by rememberUpdatedState(tabWidthPx)
    val latestSelectedTabIndex by rememberUpdatedState(selectedTabIndex)
    val latestOnTabSelected by rememberUpdatedState(onTabSelected)

    val lastIndex = (tabsCount - 1).coerceAtLeast(0)

    val dampedDragAnimation = remember(animationScope, lastIndex) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedTabIndex(),
            valueRange = 0f..lastIndex.toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.08f,
            onDragStarted = {
                dragTarget = targetValue
                isDragging = true
            },
            onDragStopped = {
                isDragging = false
                val target = dragTarget.roundToInt().coerceIn(0, lastIndex)
                updateValue(target.toFloat())
                latestOnTabSelected(target)
            },
            onDragCancel = {
                isDragging = false
                val target = dragTarget.roundToInt().coerceIn(0, lastIndex)
                updateValue(target.toFloat())
                latestOnTabSelected(target)
            },
            onDrag = { _, dragAmount ->
                val step = latestTabWidthPx
                if (step > 0f) {
                    dragTarget = (dragTarget + dragAmount.x / step)
                        .coerceIn(0f, lastIndex.toFloat())
                    updateValue(dragTarget)
                }
            }
        )
    }

    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { latestSelectedTabIndex() }.collectLatest { p ->
            if (!isDragging) {
                val t = p.coerceIn(0f, lastIndex.toFloat())
                if (dampedDragAnimation.targetValue != t) {
                    dampedDragAnimation.updateValue(t)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    Log.d("TabTest", "PILL box got touch") // TODO remove
                }
            }
            .then(dampedDragAnimation.modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { rowWidthPx = it.width.toFloat() }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }

            if (tabWidthPx > 0f) {
                val widthDp = with(density) { tabWidthPx.toDp() }
                Box(
                    Modifier
                        .offset { IntOffset((dampedDragAnimation.value * tabWidthPx).roundToInt(), 0) }
                        .width(widthDp)
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                        }
                        .drawBehind {
                            drawRoundRect(
                                color = Color.DarkGray.copy(alpha = 0.15f + 0.1f * dampedDragAnimation.pressProgress),
                                cornerRadius = CornerRadius(24.dp.toPx())
                            )
                        }
                )
            }
        }
    }
}

@Composable
fun RowScope.LiquidBottomTab(
    index: Int,
    selected: Boolean,
    label: String,
    icon: ImageVector
) {
    val scale = LocalLiquidBottomTabScale.current().invoke()
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
