package com.example.dukkanapp.core.common.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * MVI base: the screen sends [I]ntents, renders [S]tate, and handles one-shot [E]ffects
 * (navigation, snackbars, opening other apps).
 */
abstract class AppViewModel<S, I, E>(initialState: S) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    // UNLIMITED: trySend never drops; receiveAsFlow delivers each effect to exactly one collector.
    private val _effect = Channel<E>(Channel.UNLIMITED)
    val effect: Flow<E> = _effect.receiveAsFlow()

    protected val currentState: S get() = _state.value

    /** Single entry point: the screen only sends intents. */
    abstract fun onIntent(intent: I)
    protected fun setState(newState: S.() -> S) = _state.update(newState)
    protected fun sendEffect(effect: E) {
        _effect.trySend(effect)
    }
}