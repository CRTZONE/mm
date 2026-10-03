package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowMode {
    MINIMIZED_BUBBLE, // Small floating logo bubble on screen
    FLOATING_WINDOW,  // Resizable & draggable floating window
    FULLSCREEN        // Maximized full screen view
}

enum class WindowSizePreset(val label: String, val widthDp: Dp, val heightDp: Dp) {
    COMPACT("S", 310.dp, 440.dp),
    BALANCED("M", 360.dp, 540.dp),
    LARGE("L", 400.dp, 640.dp)
}

class FloatingWindowState {
    var windowMode by mutableStateOf(WindowMode.FLOATING_WINDOW)

    // Position of floating window (in pixels relative to parent)
    var windowOffsetX by mutableFloatStateOf(40f)
    var windowOffsetY by mutableFloatStateOf(80f)

    // Size of floating window in Dp
    var windowWidth by mutableStateOf(340.dp)
    var windowHeight by mutableStateOf(500.dp)

    // Position of minimized floating logo bubble
    var bubbleOffsetX by mutableFloatStateOf(40f)
    var bubbleOffsetY by mutableFloatStateOf(200f)

    fun applyPreset(preset: WindowSizePreset) {
        windowWidth = preset.widthDp
        windowHeight = preset.heightDp
        windowMode = WindowMode.FLOATING_WINDOW
    }

    fun openFloatingWindow() {
        windowMode = WindowMode.FLOATING_WINDOW
    }

    fun minimizeToBubble() {
        windowMode = WindowMode.MINIMIZED_BUBBLE
    }

    fun toggleFullscreen() {
        windowMode = if (windowMode == WindowMode.FULLSCREEN) {
            WindowMode.FLOATING_WINDOW
        } else {
            WindowMode.FULLSCREEN
        }
    }
}
