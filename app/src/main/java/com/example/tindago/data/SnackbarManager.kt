package com.example.tindago.data

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * CompositionLocal to provide SnackbarHostState globally.
 * Set up in MainScaffold, consumed by any screen composable.
 * Falls back to defaults so preview composables don't crash.
 */
val LocalSnackbarHost = compositionLocalOf { SnackbarHostState() }

/** Fallback scope for previews so they don't crash */
private val fallbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
val LocalSnackbarScope = compositionLocalOf<CoroutineScope> { fallbackScope }
