package com.mirzadev.onecenter.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.mirzadev.onecenter.ui.design.GlassMode
import com.mirzadev.onecenter.ui.design.GlassTokens
import com.mirzadev.onecenter.ui.design.LocalGlassMode
import com.mirzadev.onecenter.ui.design.glassSurface
import com.mirzadev.onecenter.ui.design.glassSurfaceColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.ExperimentalHazeApi

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

@OptIn(ExperimentalHazeApi::class)
@Composable
fun GlassNavigationBar(
    selectedIndex: Int,
    items: List<NavItem>,
    onSelect: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val barShape = remember { RoundedCornerShape(NavBarMetrics.Radius) }
    val glassMode = LocalGlassMode.current
    val surfaceColor = glassSurfaceColor()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = NavBarMetrics.OuterHorizontal,
                vertical = NavBarMetrics.OuterVertical
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NavBarMetrics.BarHeight)
                .graphicsLayer {
                    shape = barShape
                    clip = true
                }
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        tints = listOf(HazeTint(surfaceColor.copy(alpha = 0.25f))),
                        blurRadius = 80.dp,
                        fallbackTint = HazeTint(surfaceColor.copy(alpha = 0.85f))
                    )
                ) {
                    blurEnabled = glassMode == GlassMode.LIQUID
                    inputScale = HazeInputScale.Fixed(0.5f)
                }
                .glassSurface(shape = barShape, highlight = true)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                NavBarTab(
                    item = item,
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
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

    // One spec for every animated property keeps the tab feeling like a
    // single object rather than three things moving at different speeds.
    val spec = remember { tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing) }

    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spec,
        label = "navIndicatorAlpha"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = spec,
        label = "navIconScale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) primary else inactive,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "navContentColor"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pillShape = remember { RoundedCornerShape(GlassTokens.RadiusNav - 6.dp) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(pillShape)
            // Alpha-modulated tint rather than a sliding indicator: no layout
            // pass, no measurement work, just a colour blend per frame.
            .background(primary.copy(alpha = 0.12f * indicatorAlpha))
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
