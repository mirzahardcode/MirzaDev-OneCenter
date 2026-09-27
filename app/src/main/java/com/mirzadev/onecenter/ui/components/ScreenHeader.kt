package com.mirzadev.onecenter.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mirzadev.onecenter.ui.design.GlassTokens
import java.util.Locale

/**
 * Spacing owned by the header rather than by each screen.
 *
 * This is the point of the component: consistency across Home, APKs and
 * Private comes from them all calling the same thing, not from three screens
 * happening to use matching dp values that drift apart on the next edit.
 */
object ScreenHeaderDefaults {

    /**
     * Breathing room between the status bar and the title.
     *
     * The old layout gave the header 16dp below the system inset, which is
     * why it read as pinned to the corner. 28dp lets the title sit as its own
     * block instead of hanging off the top edge.
     */
    val TopSpace: Dp = 28.dp

    /** Separation before the first content item. */
    val BottomSpace: Dp = GlassTokens.SpaceS

    /**
     * Overline tracking. The only metric override in the app — small uppercase
     * text needs wider tracking to stay legible, and Material 3's label scale
     * assumes mixed case. Size and weight still come from the type scale.
     */
    val OverlineTracking = 1.4.sp
}

/**
 * Screen header: optional overline, title, optional subtitle, optional
 * trailing slot.
 *
 * Horizontal gutter and top spacing are applied here, so screens pass this as
 * a LazyColumn item with no padding of their own.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    subtitle: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GlassTokens.ScreenGutter)
    ) {
        Spacer(Modifier.height(ScreenHeaderDefaults.TopSpace))

        Row(verticalAlignment = Alignment.Top) {

            Column(modifier = Modifier.weight(1f)) {

                if (overline != null) {
                    Text(
                        text = overline.uppercase(Locale.ROOT),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = ScreenHeaderDefaults.OverlineTracking
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(GlassTokens.SpaceS))
                }

                Text(
                    text = title,
                    style = titleStyle,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (subtitle != null) {
                    Spacer(Modifier.height(GlassTokens.SpaceXs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (trailing != null) {
                Spacer(Modifier.width(GlassTokens.SpaceM))
                trailing()
            }
        }

        Spacer(Modifier.height(ScreenHeaderDefaults.BottomSpace))
    }
}
