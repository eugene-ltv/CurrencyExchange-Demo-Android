package com.saiferwp.currencyexchange.exchange.viewmodel

import androidx.lifecycle.viewModelScope
import com.saiferwp.currencyexchange.common.BaseViewModel
import com.saiferwp.currencyexchange.common.ViewEvent
import com.saiferwp.currencyexchange.common.ViewState
import com.saiferwp.currencyexchange.exchange.usecase.CurrenciesResult
import com.saiferwp.currencyexchange.exchange.usecase.FetchCurrenciesUseCase
import com.saiferwp.currencyexchange.utils.divideAndScaleRates
import com.saiferwp.currencyexchange.utils.multiplyAndScaleToCents
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import java.math.BigDecimal

internal class ExchangeViewModel(
    private val fetchCurrenciesUseCase: FetchCurrenciesUseCase
) : BaseViewModel<ExchangeUiState, ExchangeEvent>() {

    private var numberOfSuccessfulExchanges = 0

    private val accounts = mutableMapOf(
        "EUR" to BigDecimal(1000)
    )

    private var baseCurrency: String? = null
    private val rates: MutableMap<String, BigDecimal> = mutableMapOf()
    private var availableCurrenciesForReceive: List<String> = emptyList()

    // todo init gracefully
    private var selectedCurrencyForSell: String? = "EUR"
    private var selectedCurrencyForReceive: String? = null
    private var sellAmount: BigDecimal = BigDecimal.ZERO

    override fun setInitialState() = ExchangeUiState.Initial

    override fun handleEvents(event: ExchangeEvent) {
        when (event) {
            ExchangeEvent.FetchRates -> requestRates()
            is ExchangeEvent.SellInputChanged -> {
                val amount = BigDecimal(event.input)
                sellAmount = amount
                updateReceiveValue()
            }

            is ExchangeEvent.SellCurrencySelected -> {
                selectedCurrencyForSell = accounts.keys.toList()[event.id]
            }

            is ExchangeEvent.ReceiveCurrencySelected -> {
                selectedCurrencyForReceive = availableCurrenciesForReceive[event.id]
                updateReceiveValue()
            }

            ExchangeEvent.SubmitExchange -> doExchange()
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
                        baseCurrency = result.baseCurrency

                        val availableAccounts = accounts.keys
                        val allAvailableCurrencies = result.rates.keys.toList()
                        availableCurrenciesForReceive = allAvailableCurrencies.minus(
                            availableAccounts
                        )

                        setState {
                            ExchangeUiState.Loaded(
                                availableAccounts = availableAccounts.toList(),
                                availableCurrenciesForExchange = availableCurrenciesForReceive
                            )
                        }
                    }

                    CurrenciesResult.Failed -> TODO()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun updateReceiveValue() {
        if (selectedCurrencyForReceive == null) return

        val receiveAmount = calculateReceiveAmount()

        setState {
            ExchangeUiState.CalculatedReceiveAmount(
                receiveAmount = receiveAmount
            )
        }
    }

    private fun calculateReceiveAmount(): BigDecimal {
        return if (selectedCurrencyForSell == baseCurrency) {
            val exchangeRate = rates[selectedCurrencyForReceive] ?: return BigDecimal.ZERO
            multiplyAndScaleToCents(sellAmount, exchangeRate)
        } else {
            val sellExchangeRate = rates[selectedCurrencyForSell] ?: BigDecimal.ZERO
            val receiveExchangeRate = rates[selectedCurrencyForReceive] ?: BigDecimal.ZERO

            multiplyAndScaleToCents(
                sellAmount,
                divideAndScaleRates(receiveExchangeRate, sellExchangeRate)
            )
        }
    }

    private fun doExchange() {
        if (baseCurrency == null) return
        if (selectedCurrencyForSell == null) return
        if (selectedCurrencyForReceive == null) return

        val receiveAmount = calculateReceiveAmount()

        println(receiveAmount)
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
    internal data class SellCurrencySelected(val id: Int) : ExchangeEvent()
    internal data class ReceiveCurrencySelected(val id: Int) : ExchangeEvent()
    internal data object SubmitExchange : ExchangeEvent()
}

