package com.saiferwp.currencyexchange.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal interface ViewState
internal interface ViewEvent

internal abstract class BaseViewModel<UiState : ViewState, Event : ViewEvent> : ViewModel() {

    private val initialState: UiState by lazy { setInitialState() }
    abstract fun setInitialState(): UiState

    private val _event: MutableSharedFlow<Event> = MutableSharedFlow()
    protected val event: Flow<Event> = _event

    private val _viewState: MutableStateFlow<UiState> = MutableStateFlow(initialState)
    val viewState: StateFlow<UiState> = _viewState

    init {
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        viewModelScope.launch {
            _event.collect {
                handleEvents(it)
            }
        }
    }

    abstract fun handleEvents(event: Event)

    fun sendEvent(event: Event) {
        viewModelScope.launch { _event.emit(event) }
    }

    protected fun setState(reducer: UiState.() -> UiState) {
        val newState = viewState.value.reducer()
        _viewState.value = newState
    }
}