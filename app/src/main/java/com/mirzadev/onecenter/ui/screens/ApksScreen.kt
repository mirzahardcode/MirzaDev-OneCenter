package com.mirzadev.onecenter.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mirzadev.onecenter.data.GitHubApk
import com.mirzadev.onecenter.state.ApksState
import com.mirzadev.onecenter.ui.components.EmptyState
import com.mirzadev.onecenter.ui.components.GlassCard
import com.mirzadev.onecenter.ui.components.IconBadge
import com.mirzadev.onecenter.ui.components.InlineMessage
import com.mirzadev.onecenter.ui.components.LoadingBlock
import com.mirzadev.onecenter.ui.components.RefreshAction
import com.mirzadev.onecenter.ui.components.ScreenHeader
import com.mirzadev.onecenter.ui.components.SectionHeader
import com.mirzadev.onecenter.ui.components.StatusDot
import com.mirzadev.onecenter.ui.design.GlassTokens

@Composable
fun ApksScreen(
    state: ApksState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        state.load(scope)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(GlassTokens.SpaceM)
    ) {

        // Same component, same spacing, same overline treatment as Home —
        // which is what makes the two screens feel like one app.
        item(key = "header") {
            ScreenHeader(
                overline = "Daftar Versi Crack",
                title = "Aplikasi",
                subtitle = "Aplikasi Crack yang tersedia"
            )
        }

        item(key = "count_header") {
            Column(
                modifier = Modifier.padding(horizontal = GlassTokens.ScreenGutter)
            ) {
                SectionHeader(
                    title = if (state.apks.isEmpty()) {
                        "Versi yg tersedia"
                    } else {
                        "${state.apks.size} Versi yg tersedia"
                    },
                    action = {
                        RefreshAction(
                            isLoading = state.isLoading,
                            onClick = { state.load(scope, forceRefresh = true) }
                        )
                    }
                )
                InlineMessage(message = state.errorMessage, isError = true)
            }
        }

        if (state.isLoading && state.apks.isEmpty()) {
            item(key = "loading") { LoadingBlock() }
        }

        items(
            items = state.apks,
            key = { apk -> apk.downloadUrl.ifBlank { "${apk.name}_${apk.version}" } },
            contentType = { "apk" }
        ) { apk ->
            ApkCard(
                apk = apk,
                onDownload = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(apk.downloadUrl))
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = GlassTokens.ScreenGutter)
            )
        }

        if (!state.isLoading && state.apks.isEmpty() && state.errorMessage == null) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Default.Folder,
                    title = "Belum ada aplikasi",
                    subtitle = "Tidak ada aplikasi crack yang tersedia.",
                    modifier = Modifier.padding(horizontal = GlassTokens.ScreenGutter)
                )
            }
        }
    }
}

/**
 * cbtcloud gets its real artwork; everything else falls back to the generic
 * Android glyph inside the usual tinted badge. Same 40dp footprint either
 * way, so rows stay aligned regardless of which branch renders.
 */
@Composable
private fun ApkIcon(
    apk: GitHubApk,
    modifier: Modifier = Modifier
) {
    val logoUrl = apkLogoUrl(apk)

    if (logoUrl != null) {
        AsyncImage(
            model = logoUrl,
            contentDescription = apk.name,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentScale = ContentScale.Fit
        )
    } else {
        Icon(
            imageVector = Icons.Default.Android,
            contentDescription = null
        )
    }
}

@Composable
private fun ApkCard(
    apk: GitHubApk,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(GlassTokens.SpaceL)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ApkIcon(apk = apk)
            Spacer(Modifier.width(GlassTokens.SpaceM))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = apk.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${apk.version} · ${apk.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(GlassTokens.SpaceS))
                StatusDot(
                    label = "Terverifikasi",
                    dotColor = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(GlassTokens.SpaceM))

            FilledTonalIconButton(onClick = onDownload) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Unduh ${apk.name}",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun apkLogoUrl(apk: GitHubApk): String? {
    return when (apk.version) {
        "ExamBrowserCBT" ->
            "https://raw.githubusercontent.com/mirzahardcode/cracking-exam-app/main/assets/cbtexambro.png"

        "CloudCBT" ->
            "https://raw.githubusercontent.com/mirzahardcode/cracking-exam-app/main/assets/cbtcloud.png"

        "SemiofflineCBT" ->
            "https://raw.githubusercontent.com/mirzahardcode/cracking-exam-app/main/assets/cbtsemioffline.png"

        "OnlineCBT" ->
            "https://raw.githubusercontent.com/mirzahardcode/cracking-exam-app/main/assets/cbtonline.png"

        "ExamManTemanggung" ->
            "https://raw.githubusercontent.com/mirzahardcode/cracking-exam-app/main/assets/mantemanggung.png"

        "ExamSMan1Cigombong" ->
            "https://raw.githubusercontent.com/mirzahardcode/cracking-exam-app/main/assets/smancigombong.png"

        else -> null
    }
}