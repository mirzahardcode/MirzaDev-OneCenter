package com.mirzadev.onecenter.ui.testnav

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

class NavTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val backdrop = rememberLayerBackdrop {
                    drawRect(Color.White)
                    drawContent()
                }

                var selected by remember { mutableIntStateOf(0) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                Log.d("TabTest", "ROOT got touch") // TODO remove
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .layerBackdrop(backdrop)
                            .padding(16.dp)
                    ) {
                        Text("Test Screen Background Content", style = MaterialTheme.typography.titleLarge)
                        repeat(20) { index ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .padding(vertical = 8.dp)
                                    .background(
                                        when (index % 4) {
                                            0 -> Color.Red.copy(alpha = 0.3f)
                                            1 -> Color.Green.copy(alpha = 0.3f)
                                            2 -> Color.Blue.copy(alpha = 0.3f)
                                            else -> Color.Yellow.copy(alpha = 0.3f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Colorful Box #$index")
                            }
                        }
                    }

                    Text(
                        text = "selected = $selected",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 96.dp)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White
                    )

                    LiquidBottomTabs(
                        selectedTabIndex = { selected.toFloat() },
                        onTabSelected = { selected = it },
                        tabsCount = 4,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        LiquidBottomTab(0, selected == 0, "Tab 1", Icons.Default.Home)
                        LiquidBottomTab(1, selected == 1, "Tab 2", Icons.Default.Search)
                        LiquidBottomTab(2, selected == 2, "Tab 3", Icons.Default.Favorite)
                        LiquidBottomTab(3, selected == 3, "Tab 4", Icons.Default.Settings)
                    }
                }
            }
        }
    }
}
