package com.mirzadev.onecenter.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mirzadev.onecenter.state.HomeState
import com.mirzadev.onecenter.ui.components.GlassCard
import com.mirzadev.onecenter.ui.components.IconBadge
import com.mirzadev.onecenter.ui.components.InlineMessage
import com.mirzadev.onecenter.ui.components.RefreshAction
import com.mirzadev.onecenter.ui.components.ScreenHeader
import com.mirzadev.onecenter.ui.components.SectionHeader
import com.mirzadev.onecenter.ui.components.StatusDot
import com.mirzadev.onecenter.ui.design.GlassTokens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Immutable
data class AdminContact(
    val title: String,
    val usernameOrAddress: String,
    val url: String,
    val icon: ImageVector,
)

@Composable
fun HomeScreen(
    state: HomeState,
    apkCount: Int,
    contentPadding: PaddingValues,
    onApksClick: () -> Unit,
    onPrivateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dateFormat = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("id-ID"))
    }
    val today = remember(dateFormat) { dateFormat.format(Date()) }
    val greeting = remember { greetingForNow() }

    LaunchedEffect(Unit) {
        state.load(scope)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(GlassTokens.SpaceL),
    ) {

        // ── Header ──────────────────────────────────────────────────
        //
        // The date moved above the greeting. It is metadata, and metadata
        // reads better as a quiet line leading into the title than as a third
        // line trailing off the bottom — the eye lands on the greeting either
        // way, but the block now has a clear top and bottom. If you prefer the
        // original order, drop `overline` and add the date back as a Text
        // below the subtitle; nothing else depends on it.
        item(key = "header") {
            ScreenHeader(
                overline = today,
                title = "$greeting, 👋",
                subtitle = "Jangan Lupa Beribadah Mpruy😹.",
            )
        }

        item(key = "identity") {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GlassTokens.ScreenGutter),
                contentPadding = PaddingValues(GlassTokens.SpaceL),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Default.Android)
                    Spacer(Modifier.width(GlassTokens.SpaceM))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MirzaDev Center",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Pusat Aplikasi Bermanfaat Dan Bergiji.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(GlassTokens.SpaceS))
                    StatusDot(
                        label = if (state.isOnline) "Online" else "Offline",
                        dotColor = if (state.isOnline) {
                            Color(0xFF34C759)
                        } else {
                            Color(0xFFFF3B30)
                        },
                    )
                }
            }
        }

        // ── Latest information ──────────────────────────────────────
        item(key = "info_header") {
            SectionHeader(
                title = "Informasi Terbaru",
                modifier = Modifier.padding(horizontal = GlassTokens.ScreenGutter),
                action = {
                    RefreshAction(
                        isLoading = state.isLoading,
                        onClick = { state.load(scope, forceRefresh = true) },
                    )
                },
            )
        }

        item(key = "info_card") {
            Column(
                modifier = Modifier.padding(horizontal = GlassTokens.ScreenGutter),
            ) {
                InlineMessage(message = state.notice)

                val info = state.information

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(GlassTokens.SpaceL),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        IconBadge(
                            icon = Icons.Default.Info,
                            size = 36.dp,
                            iconSize = 18.dp,
                        )
                        Spacer(Modifier.width(GlassTokens.SpaceM))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = info?.title?.takeIf { it.isNotBlank() }
                                    ?: "Belum ada informasi",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(GlassTokens.SpaceXs))
                            Text(
                                text = info?.content?.takeIf { it.isNotBlank() }
                                    ?: "Informasi terbaru akan muncul di sini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            if ((info != null) && (info.updatedAt > 0L)) {
                                Spacer(Modifier.height(GlassTokens.SpaceM))
                                Text(
                                    text = "MirzaDev Center · " +
                                            dateFormat.format(Date(info.updatedAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Contact Admin ───────────────────────────────────────────
        item(key = "contact_header") {
            SectionHeader(
                title = "Contact Admin",
                modifier = Modifier.padding(horizontal = GlassTokens.ScreenGutter),
            )
        }

        items(
            items = listOf(
                AdminContact(
                    title = "GitHub Gua",
                    usernameOrAddress = "mirzahardcode",
                    url = "https://github.com/mirzahardcode",
                    icon = Icons.Default.Code,
                ),
                AdminContact(
                    title = "Kontak Saya",
                    usernameOrAddress = "instagram: @mrrrzza",
                    url = "",
                    icon = Icons.Default.SupportAgent,
                ),
            ),
            key = { contact -> contact.title },
        ) { contact ->
            ContactCard(
                contact = contact,
                onClick = {
                    if (contact.url.isNotBlank()) {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, contact.url.toUri()),
                            )
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = GlassTokens.ScreenGutter),
            )
        }

        // ── Quick access ────────────────────────────────────────────
        item(key = "quick_header") {
            SectionHeader(
                title = "Akses Cepat",
                modifier = Modifier.padding(horizontal = GlassTokens.ScreenGutter),
            )
        }

        item(key = "quick_cards") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GlassTokens.ScreenGutter)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(GlassTokens.SpaceM),
            ) {
                QuickAccessCard(
                    icon = Icons.Default.Folder,
                    title = "Aplikasi",
                    subtitle = if (apkCount > 0) {
                        "$apkCount terverifikasi"
                    } else {
                        "Aplikasi Ujian Cracked"
                    },
                    onClick = onApksClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                QuickAccessCard(
                    icon = Icons.Default.Lock,
                    title = "Bocoran",
                    subtitle = "Akses Bocoran Soal",
                    onClick = onPrivateClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun ContactCard(
    contact: AdminContact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = if (contact.url.isNotBlank()) onClick else null,
        contentPadding = PaddingValues(GlassTokens.SpaceL),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = contact.icon, size = 40.dp, iconSize = 20.dp)
            Spacer(Modifier.width(GlassTokens.SpaceM))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = contact.usernameOrAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (contact.url.isNotBlank()) {
                Spacer(Modifier.width(GlassTokens.SpaceM))
                FilledTonalIconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Buka ${contact.title}",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(GlassTokens.SpaceL),
    ) {
        IconBadge(icon = icon, size = 36.dp, iconSize = 18.dp)
        Spacer(Modifier.height(GlassTokens.SpaceM))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun greetingForNow(): String =
    when (Calendar.getInstance()[Calendar.HOUR_OF_DAY]) {
        in 0..10 -> "Selamat Pagi"
        in 11..17 -> "Selamat Sore"
        else -> "Selamat Malam"
    }
