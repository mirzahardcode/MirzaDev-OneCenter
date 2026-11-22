package com.mirzadev.onecenter

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mirzadev.onecenter.state.AppState
import com.mirzadev.onecenter.state.rememberAppState
import com.mirzadev.onecenter.ui.design.AppBackground
import com.mirzadev.onecenter.ui.design.LocalGlassMode
import com.mirzadev.onecenter.ui.navigation.GlassNavigationBar
import com.mirzadev.onecenter.ui.navigation.NavBarMetrics
import com.mirzadev.onecenter.ui.navigation.NavItem
import com.mirzadev.onecenter.ui.screens.ApksScreen
import com.mirzadev.onecenter.ui.screens.HomeScreen
import com.mirzadev.onecenter.ui.screens.PrivateScreen
import com.mirzadev.onecenter.ui.screens.SettingsScreen
import com.mirzadev.onecenter.ui.theme.MirzaDevCenterTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NativeSecurity.initializeSecurity(applicationContext)
        enableEdgeToEdge()

        // Window configuration for a clean immersive look
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            MirzaDevCenterTheme {
                val appState = rememberAppState()

                // FLAG_SECURE tracks the private session root.
                val view = LocalView.current
                val isPrivateUnlocked = appState.private.isLoggedIn

                DisposableEffect(isPrivateUnlocked) {
                    val window = (view.context as? ComponentActivity)?.window
                    if (isPrivateUnlocked) {
                        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                    onDispose {
                        window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                CompositionLocalProvider(
                    LocalGlassMode provides appState.settings.glassMode
                ) {
                    MirzaDevCenter(appState = appState)
                }
            }
        }
    }
}

@Composable
fun MirzaDevCenter(
    appState: AppState,
    modifier: Modifier = Modifier
) {
    val navItems = remember {
        listOf(
            NavItem("Utama", Icons.Default.Home),
            NavItem("Aplikasi", Icons.Default.Folder),
            NavItem("Bocoran", Icons.Default.Lock),
            NavItem("Pengaturan", Icons.Default.Settings)
        )
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { navItems.size }
    )

    val scope = rememberCoroutineScope()

    // TODO remove: selectedIndex is a read of currentPage; log pager state changes.
    LaunchedEffect(pagerState) {
        var previousPage = pagerState.currentPage
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                Log.d(
                    "NavDbg",
                    "selectedIndex changed old=$previousPage new=$page " +
                        "source=pagerState.currentPage"
                )
                previousPage = page
            }
    }

    // TODO remove: record all programmatic pager navigation entry points.
    fun animateToPage(target: Int, caller: String) {
        scope.launch {
            val currentPage = pagerState.currentPage
            // TODO remove
            Log.d("NavDbg", "animateScrollToPage($target) mulai dari $currentPage caller=$caller")
            try {
                pagerState.animateScrollToPage(target)
                // TODO remove
                Log.d("NavDbg", "animateScrollToPage($target) selesai caller=$caller")
            } catch (cancellationException: CancellationException) {
                // TODO remove
                Log.d(
                    "NavDbg",
                    "animateScrollToPage($target) DIBATALKAN pada position=" +
                        "${pagerState.currentPage + pagerState.currentPageOffsetFraction} caller=$caller"
                )
                throw cancellationException
            }
        }
    }

    // Single source of truth for screen layout metrics
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val screenPadding = PaddingValues(
        top = topInset,
        bottom = NavBarMetrics.contentClearance(bottomInset)
    )

    AppBackground(
        modifier = modifier.fillMaxSize(),
        type = appState.settings.backgroundType,
        overlay = { backdrop ->
            GlassNavigationBar(
                selectedIndex = pagerState.currentPage,
                items = navItems,
                onSelect = { index ->
                    animateToPage(index, "GlassNavigationBar.onSelect")
                },
                backdrop = backdrop,
                position = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 0.dp,
            beyondViewportPageCount = 0
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    state = appState.home,
                    apkCount = appState.apks.apks.size,
                    contentPadding = screenPadding,
                    onApksClick = { animateToPage(1, "HomeScreen.onApksClick") },
                    onPrivateClick = { animateToPage(2, "HomeScreen.onPrivateClick") }
                )

                1 -> ApksScreen(
                    state = appState.apks,
                    contentPadding = screenPadding
                )

                2 -> PrivateScreen(
                    state = appState.private,
                    contentPadding = screenPadding
                )

                3 -> SettingsScreen(
                    state = appState.settings,
                    contentPadding = screenPadding
                )
            }
        }
    }
}
