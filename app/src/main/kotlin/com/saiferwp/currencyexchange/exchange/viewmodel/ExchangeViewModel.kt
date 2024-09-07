package com.saiferwp.currencyexchange.exchange.viewmodel

import androidx.lifecycle.viewModelScope
import com.saiferwp.currencyexchange.common.BaseViewModel
import com.saiferwp.currencyexchange.common.ViewEvent
import com.saiferwp.currencyexchange.common.ViewState
import com.saiferwp.currencyexchange.exchange.data.FeesRepository
import com.saiferwp.currencyexchange.exchange.model.ExchangeFeeRule
import com.saiferwp.currencyexchange.exchange.usecase.CurrenciesResult
import com.saiferwp.currencyexchange.exchange.usecase.FetchCurrenciesUseCase
import com.saiferwp.currencyexchange.utils.divideAndScaleRates
import com.saiferwp.currencyexchange.utils.multiplyAndScaleToCents
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import java.math.BigDecimal

internal class ExchangeViewModel(
    private val fetchCurrenciesUseCase: FetchCurrenciesUseCase,
    private val feesRepository: FeesRepository
) : BaseViewModel<ExchangeUiState, ExchangeEvent>() {

    override fun setInitialState() = ExchangeUiState(
        isLoading = false,
        accounts = mutableMapOf(
            "EUR" to BigDecimal(1000)
        ),
        baseCurrency = "EUR",
        rates = mutableMapOf(),
        availableCurrenciesForReceive = emptyList(),
        selectedCurrencyForSell = "EUR",
        selectedCurrencyForReceive = "EUR",
        sellAmount = BigDecimal.ZERO,
        receiveAmount = BigDecimal.ZERO,
        exchangeFee = BigDecimal.ZERO
    )

    override fun handleEvents(event: ExchangeEvent) {
        when (event) {
            ExchangeEvent.FetchRates -> requestRates()
            is ExchangeEvent.SellInputChanged -> {
                setState {
                    copy(
                        sellAmount =
                        if (event.input.isNotBlank()) {
                            BigDecimal(event.input)
                        } else {
                            BigDecimal.ZERO
                        }
                    )
                }
                updateReceiveValue()
            }

            is ExchangeEvent.SellCurrencySelected -> {
                setState {
                    copy(
                        availableCurrenciesForReceive = rates.keys.minus(
                            selectedCurrencyForSell
                        ).toList(),
                        selectedCurrencyForSell = accounts.keys.toList()[event.id]
                    )
                }
            }

            is ExchangeEvent.ReceiveCurrencySelected -> {
                setState {
                    copy(
                        selectedCurrencyForReceive = availableCurrenciesForReceive[event.id]
                    )
                }
                updateReceiveValue()
            }

            ExchangeEvent.SubmitExchange -> doExchange()
        }
    }

    private fun requestRates() {
        fetchCurrenciesUseCase.invoke(Unit)
            .onStart {
                setState {
                    copy(
                        isLoading = true
                    )
                }
            }
            .onEach { result ->
                when (result) {
                    is CurrenciesResult.Success -> {
                        val availableCurrencies = viewState.value.accounts.keys
                        val allAvailableCurrencies = result.rates.keys.toList()
                        val availableCurrenciesForReceive = allAvailableCurrencies.minus(
                            availableCurrencies
                        )

                        setState {
                            copy(
                                isLoading = false,
                                baseCurrency = result.baseCurrency,
                                rates = result.rates,
                                availableCurrenciesForReceive = availableCurrenciesForReceive,
                                exchangeFee = feesRepository.calculateFee(
                                    ExchangeFeeRule.Params(
                                        baseCurrency = result.baseCurrency,
                                        amount = viewState.value.sellAmount
                                    )
                                )
                            )
                        }
                    }

                    CurrenciesResult.Failed -> TODO()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun updateReceiveValue() {
        val receiveAmount = calculateReceiveAmount()

        // todo make checks before exchange
        // enable/disable button
        setState {
            copy(
                receiveAmount = receiveAmount,
                exchangeFee = feesRepository.calculateFee(
                    ExchangeFeeRule.Params(
                        baseCurrency = viewState.value.selectedCurrencyForSell,
                        amount = viewState.value.sellAmount
                    )
                )
            )
        }
    }

    private fun calculateReceiveAmount(): BigDecimal {
        val state = viewState.value

        return if (state.selectedCurrencyForSell == state.baseCurrency) {
            val exchangeRate =
                state.rates[state.selectedCurrencyForReceive] ?: return BigDecimal.ZERO
            multiplyAndScaleToCents(state.sellAmount, exchangeRate)
        } else {
            val sellExchangeRate = state.rates[state.selectedCurrencyForSell] ?: BigDecimal.ZERO
            val receiveExchangeRate =
                state.rates[state.selectedCurrencyForReceive] ?: BigDecimal.ZERO

            multiplyAndScaleToCents(
                state.sellAmount,
                divideAndScaleRates(receiveExchangeRate, sellExchangeRate)
            )
        }
    }

    private fun doExchange() {
        val receiveAmount = calculateReceiveAmount()
        val mutableAccounts = viewState.value.accounts.toMutableMap()

        viewState.value.accounts[viewState.value.selectedCurrencyForSell]?.let {
            val fee = feesRepository.calculateFee(
                ExchangeFeeRule.Params(
                    baseCurrency = viewState.value.selectedCurrencyForSell,
                    amount = viewState.value.sellAmount
                )
            )
            val newBalance = it.minus(viewState.value.sellAmount).minus(fee)

            if (newBalance < BigDecimal.ZERO) {
                return
            }

            mutableAccounts[viewState.value.selectedCurrencyForSell] = newBalance
            mutableAccounts[viewState.value.selectedCurrencyForReceive] =
                mutableAccounts[viewState.value.selectedCurrencyForReceive]?.plus(receiveAmount)
                    ?: receiveAmount
        }

        feesRepository.markSuccessfulExchange()

        setState {
            copy(
                accounts = mutableAccounts.filter {
                    value -> value.value > BigDecimal.ZERO
                }
            )
        }
    }
}

internal data class ExchangeUiState(
    val isLoading: Boolean,
    val accounts: Map<String, BigDecimal>,
    val baseCurrency: String,
    val rates: Map<String, BigDecimal>,
    val availableCurrenciesForReceive: List<String>,
    val selectedCurrencyForSell: String,
    val selectedCurrencyForReceive: String,
    val sellAmount: BigDecimal,
    val receiveAmount: BigDecimal,
    val exchangeFee: BigDecimal
) : ViewState

internal sealed class ExchangeEvent : ViewEvent {
    internal data object FetchRates : ExchangeEvent()
    internal data class SellInputChanged(val input: String) : ExchangeEvent()
    internal data class SellCurrencySelected(val id: Int) : ExchangeEvent()
    internal data class ReceiveCurrencySelected(val id: Int) : ExchangeEvent()
    internal data object SubmitExchange : ExchangeEvent()
}

