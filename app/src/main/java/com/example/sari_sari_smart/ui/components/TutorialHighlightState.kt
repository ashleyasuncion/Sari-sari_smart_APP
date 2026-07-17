package com.example.sari_sari_smart.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier

/**
 * State holder for tutorial highlight targets.
 * Each screen registers composables that can be highlighted during tutorials
 * by providing a target ID and their layout coordinates.
 */
class TutorialHighlightState {
    private val _targets = mutableStateMapOf<String, Rect>()

    /** Current bounds of all registered highlight targets, keyed by ID. */
    val targets: Map<String, Rect> = _targets

    /**
     * Register or update a highlight target's bounds.
     * Called from [Modifier.tutorialHighlight] via [onGloballyPositioned].
     */
    fun register(id: String, coordinates: LayoutCoordinates) {
        val pos = coordinates.localToRoot(Offset.Zero)
        val sz = coordinates.size
        _targets[id] = Rect(
            left = pos.x,
            top = pos.y,
            right = pos.x + sz.width,
            bottom = pos.y + sz.height
        )
    }

    /** Remove a previously registered target. */
    fun unregister(id: String) {
        _targets.remove(id)
    }

    /** Get the bounding rectangle of a registered target, or null if not found. */
    fun getBounds(id: String): Rect? = _targets[id]

    /** Clear all registered targets. */
    fun clear() {
        _targets.clear()
    }
}

/**
 * CompositionLocal that provides the [TutorialHighlightState] to descendant composables.
 * Set in NavGraph and read in individual screen composables.
 */
val LocalTutorialHighlightState = compositionLocalOf { TutorialHighlightState() }

/**
 * CompositionLocal that holds the currently active screen's vertical [ScrollState].
 * Screens with scrollable content provide their scroll state so the tutorial overlay
 * can auto-scroll to bring highlighted elements into view.
 */
val LocalScreenScrollState = compositionLocalOf<ScrollState?> { null }

/**
 * Modifier extension that registers this composable as a tutorial highlight target.
 *
 * Usage:
 * ```kotlin
 * val highlightState = LocalTutorialHighlightState.current
 * Text(
 *     "Good morning!",
 *     modifier = Modifier.tutorialHighlight("morningStockCard", highlightState)
 * )
 * ```
 *
 * The [id] must match the [TutorialStep.highlightTarget] in the tutorial step definition.
 */
fun Modifier.tutorialHighlight(id: String, state: TutorialHighlightState): Modifier = this.then(
    onGloballyPositioned { coordinates ->
        state.register(id, coordinates)
    }
)
