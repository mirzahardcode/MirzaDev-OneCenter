package com.mirzadev.onecenter.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * The app's only background.
 *
 * Screens never set their own background — they draw transparent content on
 * top of this.
 */
object BackgroundConfig {

    @DrawableRes
    val wallpaperRes: Int? = null

    const val ScrimAlpha = 0.55f

    /** Subtle ambient light used behind glass surfaces. */
    const val AmbientAlpha = 0.10f
}

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(HazeState) -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary

    val hazeState = remember { HazeState() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
    ) {
        // Background visual (gradient)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primary.copy(
                                alpha = BackgroundConfig.AmbientAlpha
                            ),
                            Color.Transparent
                        ),
                        radius = 900f
                    )
                )
        )

        content(hazeState)
    }
}