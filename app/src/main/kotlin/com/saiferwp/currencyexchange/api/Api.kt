package com.saiferwp.currencyexchange.api

import com.saiferwp.currencyexchange.exchange.model.CurrencyRates
import retrofit2.Response
import retrofit2.http.GET

internal interface Api {

    @GET("currency-exchange-rates")
    suspend fun getCurrenciesRates(): Response<CurrencyRates>
}
