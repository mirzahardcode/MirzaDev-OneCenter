package com.mirzadev.onecenter.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The app's only background.
 *
 * Screens never set their own background — they draw transparent content on
 * top of this.
 */
object BackgroundConfig {

    /** Optional wallpaper. Set to e.g. R.drawable.wallpaper to enable it. */
    @DrawableRes
    val wallpaperRes: Int? = null

    /** Opacity of the theme-colored scrim drawn over the wallpaper. */
    const val SCRIM_ALPHA = 0.55f

    /** Subtle ambient light used behind glass surfaces. */
    const val AMBIENT_ALPHA = 0.10f

    /** Opacity of the liquid blobs. */
    const val LIQUID_ALPHA_DARK = 0.55f
    const val LIQUID_ALPHA_LIGHT = 0.30f

    /** One full loop of the liquid motion. Higher = calmer. */
    const val LIQUID_LOOP_MILLIS = 22_000
}

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    type: BackgroundType = BackgroundType.Default,
    @DrawableRes wallpaperRes: Int? = BackgroundConfig.wallpaperRes,
    /** Chrome kaca di atas layar (navbar). Menerima backdrop yang harus di-sampling. */
    overlay: @Composable BoxScope.(Backdrop) -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val background = scheme.background
    val isDark = background.luminance() < 0.5f
    val blobAlpha =
        if (isDark) BackgroundConfig.LIQUID_ALPHA_DARK else BackgroundConfig.LIQUID_ALPHA_LIGHT

    // Lewat State supaya lambda rekam tidak basi saat tema berganti.
    val backgroundState = rememberUpdatedState(background)
    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundState.value)
        drawContent()
    }

    val liquidColors = if (isDark) {
        listOf(scheme.primaryContainer, scheme.tertiaryContainer, scheme.secondaryContainer)
    } else {
        listOf(scheme.primary, scheme.tertiary, scheme.secondary)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
    ) {
        // Semua yang boleh terlihat di balik kaca: background + layar.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            if (wallpaperRes != null) {
                Image(
                    painter = painterResource(wallpaperRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(background.copy(alpha = BackgroundConfig.SCRIM_ALPHA))
                )
            }

            // Ambient glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                scheme.primary.copy(alpha = BackgroundConfig.AMBIENT_ALPHA),
                                Color.Transparent
                            ),
                            radius = 900f
                        )
                    )
            )

            if (type == BackgroundType.Liquid) {
                LiquidBackground(
                    colors = liquidColors,
                    alpha = blobAlpha,
                    modifier = Modifier.fillMaxSize()
                )
            }

            content()
        }

        // Di luar layer rekam: kaca tidak boleh sampling dirinya sendiri.
        overlay(backdrop)
    }
}

private data class Blob(
    val cx: Float,       // base center X (fraction of width)
    val cy: Float,       // base center Y (fraction of height)
    val ax: Float,       // drift amplitude X
    val ay: Float,       // drift amplitude Y
    val fx: Int,         // integer frequency X (integer => seamless loop)
    val fy: Int,         // integer frequency Y
    val offset: Float,   // phase offset
    val radius: Float,   // fraction of the longest screen side
    val colorIndex: Int
)

private val Blobs = listOf(
    Blob(0.25f, 0.20f, 0.30f, 0.18f, 1, 2, 0.0f, 0.55f, 0),
    Blob(0.80f, 0.45f, 0.28f, 0.22f, 2, 1, 2.1f, 0.50f, 1),
    Blob(0.35f, 0.85f, 0.32f, 0.16f, 1, 1, 4.2f, 0.60f, 2),
    Blob(0.70f, 0.10f, 0.22f, 0.14f, 2, 2, 1.0f, 0.40f, 1),
)

@Composable
private fun LiquidBackground(
    colors: List<Color>,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "liquid")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(BackgroundConfig.LIQUID_LOOP_MILLIS, easing = LinearEasing)
        ),
        label = "liquidPhase"
    )

    Canvas(modifier = modifier) {
        // Read inside the draw phase: animation redraws only, no recomposition.
        val t = phase.value
        val maxDim = max(size.width, size.height)

        Blobs.forEach { blob ->
            val center = Offset(
                x = size.width * (blob.cx + blob.ax * sin(blob.fx * t + blob.offset)),
                y = size.height * (blob.cy + blob.ay * cos(blob.fy * t + blob.offset))
            )
            val radius = maxDim * blob.radius * (1f + 0.15f * sin(2f * t + blob.offset))
            val c = colors[blob.colorIndex % colors.size]

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        c.copy(alpha = alpha),
                        c.copy(alpha = alpha * 0.45f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
}
