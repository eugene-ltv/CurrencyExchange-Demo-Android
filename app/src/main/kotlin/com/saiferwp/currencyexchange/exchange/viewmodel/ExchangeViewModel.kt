package com.saiferwp.currencyexchange.exchange.viewmodel

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.viewModelScope
import com.saiferwp.currencyexchange.common.BaseViewModel
import com.saiferwp.currencyexchange.common.ViewEvent
import com.saiferwp.currencyexchange.common.ViewSideEffect
import com.saiferwp.currencyexchange.common.ViewState
import com.saiferwp.currencyexchange.exchange.data.FeesRepository
import com.saiferwp.currencyexchange.exchange.model.ExchangeFeeRule
import com.saiferwp.currencyexchange.exchange.usecase.CurrenciesResult
import com.saiferwp.currencyexchange.exchange.usecase.FetchCurrenciesRatesUseCase
import com.saiferwp.currencyexchange.utils.divideAndScaleRates
import com.saiferwp.currencyexchange.utils.multiplyAndScaleToCents
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.math.BigDecimal

private const val FETCH_RATES_DELAY = 5 * 1000L // 5 seconds

internal class ExchangeViewModel(
    private val fetchCurrenciesRatesUseCase: FetchCurrenciesRatesUseCase,
    private val feesRepository: FeesRepository
) : BaseViewModel<ExchangeUiState, ExchangeEvent, ExchangeEffect>(), DefaultLifecycleObserver {

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
        exchangeFee = BigDecimal.ZERO,
        exchangeRate = BigDecimal.ZERO,
        buttonSubmitEnabled = false,
        showInsufficientBalance = false
    )

    override fun handleEvents(event: ExchangeEvent) {
        when (event) {
            ExchangeEvent.FetchRates -> requestRates()
            ExchangeEvent.ReFetchRates -> refreshRates()
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
                updateReceiveValues()
            }

            is ExchangeEvent.SellCurrencySelected -> {
                val selectedCurrencyForSell = viewState.value.accounts.keys.toList()[event.id]
                setState {
                    copy(
                        availableCurrenciesForReceive = rates.keys.minus(
                            selectedCurrencyForSell
                        ).toList(),
                        selectedCurrencyForSell = selectedCurrencyForSell,
                    )
                }
                updateReceiveValues()
            }

            is ExchangeEvent.ReceiveCurrencySelected -> {
                setState {
                    copy(
                        selectedCurrencyForReceive = availableCurrenciesForReceive[event.id],
                    )
                }
                updateReceiveValues()
            }

            ExchangeEvent.SubmitExchange -> doExchange()
        }
    }

    private fun requestRates() {
        fetchCurrenciesRatesUseCase.invoke(Unit)
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

                        startRatesRefreshTimer()
                    }

                    CurrenciesResult.Failed -> {
                        setEffect { ExchangeEffect.ApiFetchFailed }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun refreshRates() {
        fetchCurrenciesRatesUseCase.invoke(Unit)
            .onEach { result ->
                when (result) {
                    is CurrenciesResult.Success -> {
                        setState {
                            copy(
                                baseCurrency = result.baseCurrency,
                                rates = result.rates
                            )
                        }
                        setState {
                            copy(
                                exchangeRate = calculateReceiveRate()
                            )
                        }
                    }

                    CurrenciesResult.Failed -> {
                        setEffect { ExchangeEffect.ApiFetchFailed }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun updateReceiveValues() {
        val receiveAmount = calculateReceiveAmount()

        val availableBalance = viewState.value.accounts[viewState.value.selectedCurrencyForSell]
        var showInsufficientBalance = false
        val buttonSubmitEnabled: Boolean

        if (viewState.value.sellAmount > BigDecimal.ZERO
            && availableBalance != null
        ) {
            val fee = feesRepository.calculateFee(
                ExchangeFeeRule.Params(
                    baseCurrency = viewState.value.selectedCurrencyForSell,
                    amount = viewState.value.sellAmount
                )
            )
            val newBalance = availableBalance.minus(viewState.value.sellAmount).minus(fee)

            buttonSubmitEnabled = if (newBalance >= BigDecimal.ZERO) {
                true
            } else {
                showInsufficientBalance = true
                false
            }
        } else {
            buttonSubmitEnabled = false
        }

        setState {
            copy(
                receiveAmount = receiveAmount,
                exchangeFee = feesRepository.calculateFee(
                    ExchangeFeeRule.Params(
                        baseCurrency = viewState.value.selectedCurrencyForSell,
                        amount = viewState.value.sellAmount
                    )
                ),
                exchangeRate = calculateReceiveRate(),
                buttonSubmitEnabled = buttonSubmitEnabled,
                showInsufficientBalance = showInsufficientBalance
            )
        }
    }

    private fun calculateReceiveRate(): BigDecimal {
        val state = viewState.value

        return if (state.selectedCurrencyForSell == state.baseCurrency) {
                state.rates[state.selectedCurrencyForReceive] ?: return BigDecimal.ZERO
        } else {
            val sellExchangeRate = state.rates[state.selectedCurrencyForSell] ?: BigDecimal.ZERO
            val receiveExchangeRate =
                state.rates[state.selectedCurrencyForReceive] ?: BigDecimal.ZERO

            divideAndScaleRates(receiveExchangeRate, sellExchangeRate)
        }
    }

    private fun calculateReceiveAmount(): BigDecimal {
        return multiplyAndScaleToCents(viewState.value.sellAmount, calculateReceiveRate())
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

    private fun startRatesRefreshTimer() {
        viewModelScope.launch {
            delay(FETCH_RATES_DELAY)
            handleEvents(ExchangeEvent.ReFetchRates)
            startRatesRefreshTimer()
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
    val exchangeFee: BigDecimal,
    val exchangeRate: BigDecimal,
    val buttonSubmitEnabled: Boolean,
    val showInsufficientBalance: Boolean
) : ViewState

internal sealed class ExchangeEvent : ViewEvent {
    internal data object FetchRates : ExchangeEvent()
    internal data object ReFetchRates : ExchangeEvent()
    internal data class SellInputChanged(val input: String) : ExchangeEvent()
    internal data class SellCurrencySelected(val id: Int) : ExchangeEvent()
    internal data class ReceiveCurrencySelected(val id: Int) : ExchangeEvent()
    internal data object SubmitExchange : ExchangeEvent()
}

internal sealed class ExchangeEffect : ViewSideEffect {
    internal data object ApiFetchFailed : ExchangeEffect()
}

