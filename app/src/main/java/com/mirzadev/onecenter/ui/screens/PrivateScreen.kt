package com.mirzadev.onecenter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mirzadev.onecenter.data.PrivateDocument
import com.mirzadev.onecenter.state.PrivateState
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrivateScreen(
    state: PrivateState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    if (state.isLoggedIn) {
        PrivateDocuments(state = state, contentPadding = contentPadding, modifier = modifier)
    } else {
        PrivateLogin(state = state, contentPadding = contentPadding, modifier = modifier)
    }
}

// ─────────────────────────────────────────────
// Login
// ─────────────────────────────────────────────

/**
 * Sits on the shared app background and uses the same GlassCard as every other
 * surface, so it reads as a locked room inside the app rather than a separate
 * authentication screen bolted on.
 */
@Composable
private fun PrivateLogin(
    state: PrivateState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            // imePadding keeps the button above the keyboard; the scroll keeps
            // short screens usable instead of clipping the fields.
            .imePadding()
            .padding(contentPadding)
            .padding(horizontal = GlassTokens.ScreenGutter),
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(GlassTokens.SpaceXl))

        IconBadge(icon = Icons.Default.Lock, size = 44.dp, iconSize = 22.dp)

        Spacer(Modifier.height(GlassTokens.SpaceL))

        Text(
            text = "Bocoran",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(GlassTokens.SpaceXs))
        Text(
            text = "Masuk untuk mengakses Bocoran Soal.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(GlassTokens.SpaceXl))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(GlassTokens.SpaceL)
        ) {
            OutlinedTextField(
                value = state.username,
                onValueChange = { state.username = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Username / Email") },
                enabled = !state.isAuthenticating,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(Modifier.height(GlassTokens.SpaceM))

            OutlinedTextField(
                value = state.password,
                onValueChange = { state.password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Kata Sandi") },
                enabled = !state.isAuthenticating,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { state.login(scope) }
                )
            )

            InlineMessage(
                message = state.errorMessage,
                isError = true,
                modifier = Modifier.padding(top = GlassTokens.SpaceM)
            )

            Spacer(Modifier.height(GlassTokens.SpaceL))

            Button(
                onClick = { state.login(scope) },
                modifier = Modifier
                    .fillMaxWidth()
                    // Fixed height so swapping the label for a spinner does
                    // not resize the button.
                    .height(48.dp),
                enabled = !state.isAuthenticating
            ) {
                if (state.isAuthenticating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Buka")
                }
            }
        }

        Spacer(Modifier.height(GlassTokens.SpaceL))

        Text(
            text = "Perangkat ini harus diizinkan untuk masuk akun kamu!.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(GlassTokens.SpaceXl))
    }
}

// ─────────────────────────────────────────────
// Documents
// ─────────────────────────────────────────────

@Composable
private fun PrivateDocuments(
    state: PrivateState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val dateFormat = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("id-ID"))
    }

    LaunchedEffect(state.isLoggedIn) {
        state.loadDocuments(scope)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(GlassTokens.SpaceM)
    ) {

        item(key = "header") {
            ScreenHeader(
                overline = "bocoran soal di update setiap jam 6-9 malam dari sebelum ujian",
                title = "Bocoran",
                subtitle = "Soal Ujian"
            )
        }

        // Session card: states that the area is protected without theatrics.
        item(key = "session") {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GlassTokens.ScreenGutter),
                contentPadding = PaddingValues(
                    start = GlassTokens.SpaceL,
                    end = GlassTokens.SpaceS,
                    top = GlassTokens.SpaceM,
                    bottom = GlassTokens.SpaceM
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Default.Lock, size = 36.dp, iconSize = 18.dp)
                    Spacer(Modifier.width(GlassTokens.SpaceM))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sesi aman",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        StatusDot(
                            label = "Perangkat terotorisasi",
                            dotColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = { state.lock() }) {
                        Text("Kunci")
                    }
                }
            }
        }

        item(key = "docs_header") {
            Column(
                modifier = Modifier.padding(horizontal = GlassTokens.ScreenGutter)
            ) {
                SectionHeader(
                    title = if (state.documents.isEmpty()) {
                        "Dokumen"
                    } else {
                        "${state.documents.size} dokumen"
                    },
                    action = {
                        RefreshAction(
                            isLoading = state.isRefreshing,
                            onClick = { state.loadDocuments(scope, forceRefresh = true) }
                        )
                    }
                )
                InlineMessage(message = state.errorMessage, isError = true)
            }
        }

        if (state.isRefreshing && state.documents.isEmpty()) {
            item(key = "loading") { LoadingBlock() }
        }

        items(
            items = state.documents,
            key = { it.id },
            contentType = { "document" }
        ) { document ->
            PrivateDocumentCard(
                document = document,
                formattedDate = if (document.updatedAt > 0L) {
                    dateFormat.format(Date(document.updatedAt))
                } else {
                    null
                },
                modifier = Modifier.padding(horizontal = GlassTokens.ScreenGutter)
            )
        }

        if (!state.isRefreshing && state.documents.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Default.Lock,
                    title = "Belum ada dokumen",
                    subtitle = "Dokumen privat akan muncul di sini.",
                    modifier = Modifier.padding(horizontal = GlassTokens.ScreenGutter)
                )
            }
        }
    }
}

@Composable
private fun PrivateDocumentCard(
    document: PrivateDocument,
    formattedDate: String?,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable(document.id) { mutableStateOf(false) }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { isExpanded = !isExpanded },
        contentPadding = PaddingValues(GlassTokens.SpaceL)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = document.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(GlassTokens.SpaceS))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Tutup dokumen" else "Buka dokumen",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(GlassTokens.SpaceS))
                Text(
                    text = document.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (formattedDate != null) {
                    Spacer(Modifier.height(GlassTokens.SpaceM))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
