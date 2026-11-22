package com.mirzadev.onecenter.ui.design

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * Kaca nyata untuk mode LIQUID: vibrancy -> blur -> lens (urutan wajib).
 * Blur butuh Android 12+, lens butuh Android 13+. Di bawah itu tidak menggambar
 * apa-apa; tint dari glassSurface() yang menangani fallback-nya.
 */
fun Modifier.liquidBackdrop(backdrop: Backdrop, cornerRadius: Dp): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this

    val hasLens = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    // Aturan library: refraction height <= radius sudut.
    val lensHeight = minOf(16.dp, cornerRadius)

    return drawBackdrop(
        backdrop = backdrop,
        shape = { RoundedCornerShape(cornerRadius) },
        effects = {
            vibrancy()
            blur(8.dp.toPx())
            if (hasLens) {
                lens(lensHeight.toPx(), (lensHeight * 2).toPx())
            }
        }
    )
}
