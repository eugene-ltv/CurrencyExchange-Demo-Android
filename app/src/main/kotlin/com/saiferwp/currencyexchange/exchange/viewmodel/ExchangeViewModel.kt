package com.saiferwp.currencyexchange.exchange.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saiferwp.currencyexchange.api.Api
import kotlinx.coroutines.launch

internal class ExchangeViewModel(
    private val api: Api
) : ViewModel() {

    fun requestRates() {
        viewModelScope.launch {
            val rates = api.getRates()
            if (rates.isSuccessful) {
                println("Rates: ${rates.body()}")
            }
        }
    }
}