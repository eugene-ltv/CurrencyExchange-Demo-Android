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

    private val rates: MutableMap<String, BigDecimal> = mutableMapOf()
    private var availableCurrenciesForExchange: List<String> = emptyList()
    private var selectedCurrencyForExchange: String? = null
    private var sellAmount: BigDecimal? = null

    override fun setInitialState() = ExchangeUiState.Initial

    override fun handleEvents(event: ExchangeEvent) {
        when (event) {
            ExchangeEvent.FetchRates -> requestRates()
            is ExchangeEvent.SellInputChanged -> {
                val amount = BigDecimal(event.input)
                sellAmount = amount
                calculateReceiveAmount(amount)
            }

            is ExchangeEvent.CurrencyForExchangeSelected -> {
                selectedCurrencyForExchange = availableCurrenciesForExchange[event.id]
                sellAmount?.let { calculateReceiveAmount(it) }
            }
        }
    }

    private fun requestRates() {
        fetchCurrenciesUseCase.invoke(Unit)
            .onStart {
                setState { ExchangeUiState.Loading }
            }
            .onEach { result ->
                when (result) {
                    is CurrenciesResult.Success -> {
                        rates.putAll(result.rates)

                        val availableAccounts = accounts.keys
                        val allAvailableCurrencies = result.rates.keys.toList()
                        availableCurrenciesForExchange = allAvailableCurrencies.minus(
                            availableAccounts
                        )

                        setState {
                            ExchangeUiState.Loaded(
                                availableAccounts = availableAccounts.toList(),
                                availableCurrenciesForExchange = availableCurrenciesForExchange
                            )
                        }
                    }

                    CurrenciesResult.Failed -> TODO()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun calculateReceiveAmount(sellAmount: BigDecimal) {
        if (selectedCurrencyForExchange != null) {
            val exchangeRate = rates[selectedCurrencyForExchange]
            if (exchangeRate != null) {
                val receiveAmount = sellAmount.multiply(exchangeRate)

                setState {
                    ExchangeUiState.CalculatedReceiveAmount(
                        receiveAmount = receiveAmount
                    )
                }
            }

        } else {
            // something went wrong
        }
    }
}

internal sealed class ExchangeUiState : ViewState {
    internal data object Initial : ExchangeUiState()
    internal data object Loading : ExchangeUiState()
    internal data class Loaded(
        val availableAccounts: List<String>,
        val availableCurrenciesForExchange: List<String>
    ) : ExchangeUiState()

    internal data class CalculatedReceiveAmount(
        val receiveAmount: BigDecimal
    ) : ExchangeUiState()
}

internal sealed class ExchangeEvent : ViewEvent {
    internal data object FetchRates : ExchangeEvent()
    internal data class SellInputChanged(val input: String) : ExchangeEvent()
    internal data class CurrencyForExchangeSelected(val id: Int) : ExchangeEvent()
}

