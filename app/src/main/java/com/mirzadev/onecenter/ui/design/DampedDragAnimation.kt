/*
 * Copyright (c) 2024 Kyant0
 * SPDX-License-Identifier: Apache-2.0
 */
package com.mirzadev.onecenter.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    initialValue: Float = 0f,
    private val valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    private val visibilityThreshold: Float = 0.01f,
    private val initialScale: Float = 1f,
    private val pressedScale: Float = 1.05f,
    private val onDragStarted: DampedDragAnimation.() -> Unit = {},
    private val onDragStopped: DampedDragAnimation.() -> Unit = {},
    private val onDragCancel: DampedDragAnimation.() -> Unit = {},
    private val onDrag: DampedDragAnimation.(change: PointerInputChange, dragAmount: Offset) -> Unit = { _, _ -> }
) {
    var value by mutableFloatStateOf(initialValue)
        private set

    var targetValue by mutableFloatStateOf(initialValue)
        private set

    var velocity by mutableFloatStateOf(0f)
        private set

    var pressProgress by mutableFloatStateOf(0f)
        private set

    var scaleX by mutableFloatStateOf(initialScale)
        private set

    var scaleY by mutableFloatStateOf(initialScale)
        private set

    var isDragging by mutableStateOf(false)
        private set

    private var animJob: Job? = null

    val modifier: Modifier
        get() = Modifier.inspectDragGestures(
            onDragStart = { _ ->
                animJob?.cancel()
                animJob = null
                isDragging = true
                animationScope.launch {
                    animatePress(1f)
                }
                onDragStarted()
            },
            onDragEnd = {
                isDragging = false
                animationScope.launch {
                    animatePress(0f)
                }
                onDragStopped()
            },
            onDragCancel = {
                isDragging = false
                animationScope.launch {
                    animatePress(0f)
                }
                onDragCancel()
            },
            onDrag = { change, dragAmount ->
                onDrag(change, dragAmount)
            }
        )

    fun updateValue(newValue: Float) {
        targetValue = newValue.coerceIn(valueRange)
        if (isDragging) {
            animJob?.cancel()
            animJob = null
            value = targetValue
        } else {
            animJob?.cancel()
            animationScope.launch {
                animateToTarget()
            }.also { animJob = it }
        }
    }

    fun snapTo(newValue: Float) {
        animJob?.cancel()
        animJob = null
        value = newValue.coerceIn(valueRange)
        targetValue = value
    }

    private suspend fun animateToTarget() {
        val anim = Animatable(value)
        anim.animateTo(
            targetValue = targetValue,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
                visibilityThreshold = visibilityThreshold
            )
        ) {
            this@DampedDragAnimation.value = this.value
            this@DampedDragAnimation.velocity = this.velocity
        }
    }

    private suspend fun animatePress(targetPress: Float) {
        val anim = Animatable(pressProgress)
        anim.animateTo(
            targetValue = targetPress,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ) {
            this@DampedDragAnimation.pressProgress = this.value
            this@DampedDragAnimation.scaleX = lerp(initialScale, pressedScale, pressProgress)
            this@DampedDragAnimation.scaleY = lerp(initialScale, pressedScale, pressProgress)
        }
    }
}
