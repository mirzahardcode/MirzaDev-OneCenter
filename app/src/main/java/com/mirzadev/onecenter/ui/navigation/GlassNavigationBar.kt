package com.mirzadev.onecenter.ui.navigation

import android.os.Build
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.mirzadev.onecenter.ui.design.DampedDragAnimation
import com.mirzadev.onecenter.ui.design.GlassMode
import com.mirzadev.onecenter.ui.design.GlassTokens
import com.mirzadev.onecenter.ui.design.LocalGlassMode
import com.mirzadev.onecenter.ui.design.glassSurface
import com.mirzadev.onecenter.ui.design.liquidBackdrop
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

data class NavItem(
    val label: String,
    val icon: ImageVector
)

/**
 * Metrics are public so screens can reserve exactly the right amount of
 * bottom padding. This is why no screen needs a magic `bottom = 100.dp`.
 */
object NavBarMetrics {
    val Radius: Dp = GlassTokens.RadiusNav
    val BarHeight: Dp = 62.dp
    val OuterHorizontal: Dp = GlassTokens.ScreenGutter
    val OuterVertical: Dp = 12.dp

    /** Space a scrolling screen must leave below its content. */
    fun contentClearance(systemBottomInset: Dp): Dp =
        BarHeight + (OuterVertical * 2) + systemBottomInset + GlassTokens.SpaceM
}

private val TabGap = 4.dp
private val PillRadius = GlassTokens.RadiusNav - 6.dp

@Composable
fun GlassNavigationBar(
    selectedIndex: Int,
    items: List<NavItem>,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    /** Posisi pecahan (1.4 = 40% menuju tab ke-3). Bikin pill ikut geseran pager. */
    position: () -> Float = { selectedIndex.toFloat() }
) {
    val barShape = remember { RoundedCornerShape(NavBarMetrics.Radius) }
    val glassMode = LocalGlassMode.current
    val useGlassPill = glassMode == GlassMode.LIQUID &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val primary = MaterialTheme.colorScheme.primary

    val density = LocalDensity.current
    val count = items.size
    val lastIndex = (count - 1).coerceAtLeast(0)

    val gapPx = with(density) { TabGap.toPx() }
    var rowWidthPx by remember { mutableFloatStateOf(0f) }
    val tabWidthPx =
        if (count > 0 && rowWidthPx > 0f) (rowWidthPx - gapPx * (count - 1)) / count else 0f
    val stepPx = tabWidthPx + gapPx

    // Test 1: Log composition & stepPx
    Log.d("Pill", "GlassNavigationBar composed, glass=$useGlassPill stepPx=$stepPx")

    val animationScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragTarget by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    val latestStepPx by rememberUpdatedState(stepPx)
    val latestPosition by rememberUpdatedState(position)
    val latestOnSelect by rememberUpdatedState(onSelect)

    val pill = remember(animationScope, lastIndex) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
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
                latestOnSelect(target)
            },
            onDragCancel = {
                isDragging = false
                val target = dragTarget.roundToInt().coerceIn(0, lastIndex)
                updateValue(target.toFloat())
                latestOnSelect(target)
            },
            onDrag = { _, dragAmount ->
                val step = latestStepPx
                if (step > 0f) {
                    dragTarget = (dragTarget + dragAmount.x / step)
                        .coerceIn(0f, lastIndex.toFloat())
                    updateValue(dragTarget)
                    Log.d("Pill", "drag=${dragAmount.x} target=$dragTarget")
                }
            }
        )
    }

    // Sinkron dari pager hanya saat jari tidak memegang pill
    LaunchedEffect(pill) {
        snapshotFlow { latestPosition() }.collectLatest { p ->
            if (!isDragging) {
                val t = p.coerceIn(0f, lastIndex.toFloat())
                if (pill.targetValue != t) pill.updateValue(t)
            }
        }
    }

    val activeIndex by remember {
        derivedStateOf { pill.value.roundToInt().coerceIn(0, lastIndex) }
    }

    // Tab direkam supaya pill kaca bisa membelokkan ikon dan labelnya.
    val tabsBackdrop = rememberLayerBackdrop()
    val pillBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = NavBarMetrics.OuterHorizontal,
                vertical = NavBarMetrics.OuterVertical
            )
            // Test 2: Bar touch spy
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    Log.d("Pill", "BAR got touch")
                }
            }
            .then(pill.modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(NavBarMetrics.BarHeight)
                .then(
                    if (glassMode == GlassMode.LIQUID) {
                        Modifier.liquidBackdrop(backdrop, NavBarMetrics.Radius)
                    } else {
                        Modifier
                    }
                )
                .glassSurface(shape = barShape, highlight = true)
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { rowWidthPx = it.width.toFloat() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(tabsBackdrop),
                    horizontalArrangement = Arrangement.spacedBy(TabGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        NavBarTab(
                            item = item,
                            selected = activeIndex == index,
                            onClick = { latestOnSelect(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (tabWidthPx > 0f) {
                    if (useGlassPill) {
                        GlassPill(
                            pill = pill,
                            stepPx = stepPx,
                            tabWidthPx = tabWidthPx,
                            backdrop = pillBackdrop,
                            tint = primary
                        )
                    } else {
                        PlainPill(
                            pill = pill,
                            stepPx = stepPx,
                            tabWidthPx = tabWidthPx,
                            color = primary
                        )
                    }
                }
            }
        }
    }
}

/** Pill polos untuk mode non-Liquid. */
@Composable
private fun PlainPill(
    pill: DampedDragAnimation,
    stepPx: Float,
    tabWidthPx: Float,
    color: Color
) {
    val widthDp = with(LocalDensity.current) { tabWidthPx.toDp() }

    Box(
        Modifier
            .offset { IntOffset((pill.value * stepPx).roundToInt(), 0) }
            .width(widthDp)
            .fillMaxHeight()
            // Test 3: Pill touch spy
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    Log.d("Pill", "PILL box got touch")
                }
            }
            .then(pill.modifier)
            .graphicsLayer {
                scaleX = pill.scaleX
                scaleY = pill.scaleY
            }
            .drawBehind {
                drawRoundRect(
                    color = color.copy(alpha = 0.12f + 0.06f * pill.pressProgress),
                    cornerRadius = CornerRadius(PillRadius.toPx())
                )
            }
    )
}

/** Pill kaca: membelokkan background + ikon tab, membesar saat disentuh. */
@Composable
private fun GlassPill(
    pill: DampedDragAnimation,
    stepPx: Float,
    tabWidthPx: Float,
    backdrop: Backdrop,
    tint: Color
) {
    val widthDp = with(LocalDensity.current) { tabWidthPx.toDp() }

    Box(
        Modifier
            .offset { IntOffset((pill.value * stepPx).roundToInt(), 0) }
            .width(widthDp)
            .fillMaxHeight()
            // Test 3: Pill touch spy
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    Log.d("Pill", "PILL box got touch")
                }
            }
            .then(pill.modifier)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(PillRadius) },
                effects = {
                    lens(12.dp.toPx(), 20.dp.toPx())
                },
                layerBlock = {
                    scaleX = pill.scaleX
                    scaleY = pill.scaleY
                    val v = pill.velocity / 10f
                    scaleX /= 1f - (v * 0.75f).coerceIn(-0.2f, 0.2f)
                    scaleY *= 1f - (v * 0.25f).coerceIn(-0.2f, 0.2f)
                },
                onDrawSurface = {
                    drawRect(tint.copy(alpha = 0.10f + 0.10f * pill.pressProgress))
                }
            )
    )
}

@Composable
private fun NavBarTab(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "navIconScale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) primary else inactive,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "navContentColor"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(20.dp)
                    .scale(iconScale)
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
