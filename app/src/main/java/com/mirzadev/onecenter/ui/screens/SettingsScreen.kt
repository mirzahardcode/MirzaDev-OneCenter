package com.mirzadev.onecenter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mirzadev.onecenter.NativeAuth
import com.mirzadev.onecenter.NativeCore
import com.mirzadev.onecenter.NativeLibraryVersions // <-- Import NativeLibraryVersions
import com.mirzadev.onecenter.NativeSecurity
import com.mirzadev.onecenter.SecurityInitState
import com.mirzadev.onecenter.SignatureStatus
import com.mirzadev.onecenter.state.SettingsState
import com.mirzadev.onecenter.state.deviceId
import com.mirzadev.onecenter.ui.components.GlassCard
import com.mirzadev.onecenter.ui.components.ScreenHeader
import com.mirzadev.onecenter.ui.design.GlassMode
import com.mirzadev.onecenter.ui.design.GlassTokens

@Composable
fun SettingsScreen(
    state: SettingsState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLogVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(GlassTokens.SpaceM)
    ) {
        ScreenHeader(
            overline = "EKSPERIMENTAL",
            title = "Pengaturan",
            subtitle = "Kustomisasi visual"
        )

        // Glass Surface Mode
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GlassTokens.ScreenGutter),
            contentPadding = PaddingValues(GlassTokens.SpaceL)
        ) {
            Column {
                Text(
                    text = "Mode Permukaan Kaca",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(GlassTokens.SpaceXs))
                Text(
                    text = "Pilih level efek transparansi dan blur.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(GlassTokens.SpaceL))

                GlassSegmentedSelector(
                    selectedMode = state.glassMode,
                    onModeSelected = state::updateGlassMode
                )
            }
        }

        // Notes
        Text(
            text = "Catatan: Mode Liquid perlu Perangkat keras yg mendukung buat blur waktu nyata ygy.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = GlassTokens.ScreenGutter)
        )

        // Library Versions
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GlassTokens.ScreenGutter),
            contentPadding = PaddingValues(GlassTokens.SpaceL)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Versi Library",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LibraryVersionRow(
                    name = "NativeCore",
                    version = NativeLibraryVersions.NATIVE_CORE
                )

                LibraryVersionRow(
                    name = "NativeSecurity",
                    version = NativeLibraryVersions.NATIVE_SECURITY
                )

                LibraryVersionRow(
                    name = "NativeAuth",
                    version = NativeLibraryVersions.NATIVE_AUTH
                )

                LibraryVersionRow(
                    name = "NativeLogger",
                    version = NativeLibraryVersions.NATIVE_LOGGER
                )
            }
        }

        // Debug Logcat
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GlassTokens.ScreenGutter),
            contentPadding = PaddingValues(GlassTokens.SpaceL)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GlassTokens.SpaceXs)) {
                Text(
                    text = "Logcat Debug",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(GlassTokens.SpaceXs))

                Text(
                    text = if (isLogVisible) "Sembunyikan Log ^" else "Tampilkan Log V",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isLogVisible = !isLogVisible }
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                )

                AnimatedVisibility(visible = isLogVisible) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(GlassTokens.SpaceXs),
                        modifier = Modifier.padding(top = GlassTokens.SpaceS)
                    ) {
                        ColoredLogText("Device ID: ${deviceId(context)}")
                        ColoredLogText("Checksum: ${NativeCore.calculateChecksum("MirzaDev Center")}")
                        ColoredLogText("Valid: ${NativeCore.validateInput("Mirza")}")
                        ColoredLogText("Valid: ${NativeCore.validateInput("abc")}")
                        ColoredLogText("Platform: ${NativeCore.getNativePlatform()}")
                        ColoredLogText("Version test: ${NativeCore.compareVersions("1.4.0", "1.3.9")}")
                        ColoredLogText("Version test: ${NativeCore.compareVersions("1.4.0", "1.4.0")}")
                        ColoredLogText("Version test: ${NativeCore.compareVersions("1.2.9", "1.10.0")}")
                        ColoredLogText("64-bit: ${NativeCore.isNative64Bit()}")
                        ColoredLogText("Android API: ${NativeCore.getAndroidApiLevel()}")
                        NativeCore.getDiagnostics().lines().forEach { line ->
                            if (line.isNotBlank()) {
                                ColoredLogText(line)
                            }
                        }
                        ColoredLogText("Device ABIs: ${NativeCore.getDeviceAbis()}")
                        ColoredLogText("ABI compatibility: ${if (NativeCore.checkAbiCompatibility()) "OK" else "MISMATCH"}")
                        ColoredLogText("Native library: ${NativeCore.getNativeLibraryPath()}")
                        ColoredLogText("Auth email valid: ${NativeAuth.validateEmail("mirza@gmail.com")}")
                        ColoredLogText("Auth email invalid: ${NativeAuth.validateEmail("mirza")}")
                        ColoredLogText("Auth password valid: ${NativeAuth.validatePassword("123456")}")
                        ColoredLogText("Auth password invalid: ${NativeAuth.validatePassword("12345")}")
                        ColoredLogText("Auth credentials test 0: ${NativeAuth.validateCredentials("mirza@gmail.com", "123456")}")
                        ColoredLogText("Auth credentials test 1: ${NativeAuth.validateCredentials("mirza", "123456")}")
                        ColoredLogText("Auth credentials test 2: ${NativeAuth.validateCredentials("mirza@gmail.com", "12345")}")
                        ColoredLogText("Auth credentials test 3: ${NativeAuth.validateCredentials("mirza", "12345")}")
                        ColoredLogText("Sanitized email: ${NativeAuth.sanitizeEmail("  mirza@gmail.com  ")}")
                        ColoredLogText("Sanitized email: ${NativeAuth.sanitizeEmail("mirza@gmail.com")}")
                        ColoredLogText("Security equals 1: ${NativeSecurity.constantTimeEquals("abc", "abc")}")
                        ColoredLogText("Security equals 2: ${NativeSecurity.constantTimeEquals("abc", "abd")}")
                        ColoredLogText("Security equals 3: ${NativeSecurity.constantTimeEquals("abc", "ab")}")
                        ColoredLogText("Security equals 4: ${NativeSecurity.constantTimeEquals("", "")}")

                        val actualDigest = NativeSecurity.getCurrentSigningDigest(context)
                        val currentStatus = NativeSecurity.getSignatureStatus(context)
                        val initState = NativeSecurity.getSecurityInitState(context)
                        val testA = NativeSecurity.getSignatureStatus(context, "")
                        val testB = if (actualDigest != null) NativeSecurity.getSignatureStatus(context, actualDigest) else SignatureStatus.NOT_CONFIGURED
                        val testC = NativeSecurity.getSignatureStatus(context, "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00")

                        ColoredLogText("Signature status: $currentStatus")
                        ColoredLogText("Security initialization: $initState")

                        if (initState == SecurityInitState.BLOCKED) {
                            ColoredLogText("Reason: SIGNATURE_MISMATCH")
                        }

                        ColoredLogText("App signature test A (Not Configured): $testA")
                        ColoredLogText("App signature test B (Match): $testB")
                        ColoredLogText("App signature test C (Mismatch): $testC")
                    }
                }
            }
        }
    }
}

@Composable
private fun ColoredLogText(
    text: String,
    modifier: Modifier = Modifier
) {
    val whiteColor = Color.White        // Label (Putih)
    val blueColor = Color(0xFF64B5F6)   // Plaintext sisa (Biru)
    val greenColor = Color(0xFF81C784)  // True / MATCH / OK / ACTIVE (Hijau)
    val redColor = Color(0xFFE57373)    // False / MISMATCH / INACTIVE / BLOCKED (Merah)
    val yellowColor = Color(0xFFFFD54F) // Numeric / NOT_CONFIGURED (Kuning)
    val pathColor = Color(0xFFE0E0E0)   // Path File /data/app/... (Abu-abu agak putih)

    fun getOutputColor(valueStr: String): Color {
        val trimmed = valueStr.trim()
        return when {
            // Path -> Abu-abu agak putih
            trimmed.startsWith("/") -> pathColor

            // True / MATCH / OK / ACTIVE -> Hijau
            trimmed.equals("true", ignoreCase = true) ||
                    trimmed.equals("MATCH", ignoreCase = true) ||
                    trimmed.equals("OK", ignoreCase = true) ||
                    trimmed.equals("ACTIVE", ignoreCase = true) ||
                    trimmed.contains("VALID", ignoreCase = true) -> greenColor

            // False / MISMATCH / INACTIVE / BLOCKED -> Merah
            trimmed.equals("false", ignoreCase = true) ||
                    trimmed.contains("MISMATCH", ignoreCase = true) ||
                    trimmed.equals("INACTIVE", ignoreCase = true) ||
                    trimmed.contains("BLOCKED", ignoreCase = true) ||
                    trimmed.contains("INVALID", ignoreCase = true) -> redColor

            // NOT_CONFIGURED -> Kuning
            trimmed.contains("NOT_CONFIGURED", ignoreCase = true) -> yellowColor

            // Numeric (angka seperti 1, 0, -1) -> Kuning
            trimmed.toIntOrNull() != null ||
                    trimmed.toLongOrNull() != null ||
                    trimmed.toDoubleOrNull() != null ||
                    trimmed.matches(Regex("^-?\\d+$")) -> yellowColor

            // Plaintext -> Biru
            else -> blueColor
        }
    }

    val annotatedString = buildAnnotatedString {
        if (text.contains(": ")) {
            val parts = text.split(": ", limit = 2)
            val label = parts[0]
            val value = parts[1]

            // Label -> Putih
            withStyle(SpanStyle(color = whiteColor)) {
                append("$label: ")
            }

            // Value -> Sesuai Aturan Pewarnaan
            val valueColor = getOutputColor(value)
            withStyle(SpanStyle(color = valueColor, fontWeight = FontWeight.SemiBold)) {
                append(value)
            }
        } else {
            val color = getOutputColor(text)
            withStyle(SpanStyle(color = color)) {
                append(text)
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

@Composable
private fun GlassSegmentedSelector(
    selectedMode: GlassMode,
    onModeSelected: (GlassMode) -> Unit
) {
    val modes = GlassMode.entries
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        modes.forEach { mode ->
            val isSelected = selectedMode == mode
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                },
                label = "modeBackground"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "modeText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(backgroundColor)
                    .clickable { onModeSelected(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (mode) {
                        GlassMode.OFF -> "Off"
                        GlassMode.FROSTED -> "Frosted"
                        GlassMode.LIQUID -> "Liquid"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LibraryVersionRow(
    name: String,
    version: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = version,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}