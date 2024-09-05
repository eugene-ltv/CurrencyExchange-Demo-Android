package com.saiferwp.currencyexchange.exchange.viewmodel

import androidx.lifecycle.viewModelScope
import com.saiferwp.currencyexchange.common.BaseViewModel
import com.saiferwp.currencyexchange.common.ViewEvent
import com.saiferwp.currencyexchange.common.ViewState
import com.saiferwp.currencyexchange.exchange.usecase.CurrenciesResult
import com.saiferwp.currencyexchange.exchange.usecase.FetchCurrenciesUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import java.math.BigDecimal

internal class ExchangeViewModel(
    private val fetchCurrenciesUseCase: FetchCurrenciesUseCase
) : BaseViewModel<ExchangeUiState, ExchangeEvent>() {

    private val accounts = mapOf(
        "EUR" to BigDecimal(1000)
    )

    override fun setInitialState() = ExchangeUiState.Initial

    override fun handleEvents(event: ExchangeEvent) {
        when (event) {
            ExchangeEvent.FetchRates -> requestRates()
        }
    }

    private fun requestRates() {
        fetchCurrenciesUseCase.invoke(Unit)
            .onStart {
                setState { ExchangeUiState.Loading }
            }
            .onEach { result->
                when(result) {
                    is CurrenciesResult.Success -> {

                        val availableAccounts = accounts.keys
                        val availableCurrencies = result.availableCurrencies.subtract(
                            availableAccounts
                        )

                        setState {
                            ExchangeUiState.Loaded(
                                availableAccounts = availableAccounts.toList(),
                                availableCurrencies = availableCurrencies.toList()
                            )
                        }
                    }
                    CurrenciesResult.Failed -> TODO()
                }
            }
            .launchIn(viewModelScope)
    }


}

internal sealed class ExchangeUiState : ViewState {
    internal data object Initial : ExchangeUiState()
    internal data object Loading : ExchangeUiState()
    internal data class Loaded(
        val availableAccounts: List<String>,
        val availableCurrencies: List<String>
    ) : ExchangeUiState()
}

internal sealed class ExchangeEvent : ViewEvent {
    internal data object FetchRates : ExchangeEvent()
}

