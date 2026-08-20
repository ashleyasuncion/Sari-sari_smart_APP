package com.example.tindago.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.compositionLocalOf

/**
 * Shared holder that bridges scroll state between screen composables and the
 * TutorialOverlay. The overlay is composed outside individual screens' composition
 * trees, so it cannot read [LocalScreenScrollState] or [LocalScreenLazyListState]
 * provided inside screens. This holder is provided at the NavGraph level and
 * updated by screens when they compose — the overlay reads from it independently.
 */
class TutorialScrollStateHolder {
    /** Current screen's vertical ScrollState (for Column + verticalScroll screens). */
    @Transient
    private var _scrollState: ScrollState? = null

    /** Current screen's LazyListState (for LazyColumn screens). */
    @Transient
    private var _lazyListState: LazyListState? = null

    val scrollState: ScrollState? get() = _scrollState
    val lazyListState: LazyListState? get() = _lazyListState

    /** Register a regular scroll state. */
    fun updateScrollState(state: ScrollState?) {
        _scrollState = state
        _lazyListState = null
    }

    /** Register a lazy list scroll state. */
    fun updateLazyListState(state: LazyListState?) {
        _lazyListState = state
        _scrollState = null
    }

    /** Clear all scroll state references. */
    fun clear() {
        _scrollState = null
        _lazyListState = null
    }
}

/** CompositionLocal providing the shared [TutorialScrollStateHolder] at NavGraph level. */
val LocalTutorialScrollStateHolder = compositionLocalOf { TutorialScrollStateHolder() }
