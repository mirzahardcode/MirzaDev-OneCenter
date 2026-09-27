package com.mirzadev.onecenter.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.staticCompositionLocalOf
import com.mirzadev.onecenter.data.PrefsCache

enum class GlassMode {
    OFF,
    FROSTED,
    LIQUID
}

val LocalGlassMode = staticCompositionLocalOf { GlassMode.OFF }

/**
 * Centralized Liquid Glass design system.
 *
 * All translucent surfaces use this file so the visual language can evolve
 * without touching individual screens.
 */
object GlassTokens {

    // ── Shape ───────────────────────────────────────────────────────────

    val RadiusCard: Dp = 20.dp
    val RadiusNav: Dp = 30.dp
    val RadiusSmall: Dp = 12.dp

    // ── Spacing ─────────────────────────────────────────────────────────

    val SpaceXs: Dp = 4.dp
    val SpaceS: Dp = 8.dp
    val SpaceM: Dp = 12.dp
    val SpaceL: Dp = 16.dp
    val SpaceXl: Dp = 24.dp

    val ScreenGutter: Dp = 20.dp

    // ── Glass ───────────────────────────────────────────────────────────

    /**
     * Lower opacity lets the background contribute more to the glass effect.
     */
    const val SurfaceAlphaLight = 0.58f
    const val SurfaceAlphaDark = 0.42f

    /**
     * Very subtle glass edge.
     */
    const val BorderAlphaLight = 0.18f
    const val BorderAlphaDark = 0.20f

    /**
     * Thin light reflection near the top of the surface.
     */
    const val HighlightAlpha = 0.10f

    /**
     * Very subtle lower shading gives the surface depth.
     */
    const val ShadowAlpha = 0.035f

    /**
     * Real backdrop blur can be enabled later.
     */
    const val BlurEnabled = true
}

@Composable
@ReadOnlyComposable
fun glassSurfaceColor(): Color {
    val alpha = if (isSystemInDarkTheme()) {
        GlassTokens.SurfaceAlphaDark
    } else {
        GlassTokens.SurfaceAlphaLight
    }

    return MaterialTheme.colorScheme.surface.copy(alpha = alpha)
}

@Composable
@ReadOnlyComposable
fun glassBorderColor(): Color {
    val alpha = if (isSystemInDarkTheme()) {
        GlassTokens.BorderAlphaDark
    } else {
        GlassTokens.BorderAlphaLight
    }

    return MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
}

/**
 * Applies the app's Liquid Glass surface.
 *
 * The effect is intentionally lightweight:
 *
 * 1. translucent surface
 * 2. subtle vertical light reflection
 * 3. subtle lower depth
 * 4. thin glass edge
 *
 * No real-time blur is used yet.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(GlassTokens.RadiusCard),
    highlight: Boolean = false
): Modifier {

    val surface = glassSurfaceColor()
    val border = glassBorderColor()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val glassMode = LocalGlassMode.current

    // Jika Frosted Glass ON, kita naikin opacity buat simulasi.
    // Jika Liquid, kita balik ke opacity rendah karena blur native yang bekerja.
    val baseAlpha = if (glassMode == GlassMode.FROSTED) 0.82f else surface.alpha
    val finalSurface = surface.copy(alpha = baseAlpha)

    val highlightAlpha =
        if (glassMode != GlassMode.OFF) 0.18f else GlassTokens.HighlightAlpha

    val shadowAlpha =
        if (glassMode != GlassMode.OFF) 0.08f else GlassTokens.ShadowAlpha

    val glassGradient = Brush.verticalGradient(
        colors = listOf(
            onSurface.copy(alpha = highlightAlpha),
            Color.Transparent,
            onSurface.copy(alpha = shadowAlpha)
        )
    )

    return this
        .clip(shape)
        .background(finalSurface)
        .background(glassGradient)
        .border(
            width = 1.dp,
            color = border,
            shape = shape
        )
}
