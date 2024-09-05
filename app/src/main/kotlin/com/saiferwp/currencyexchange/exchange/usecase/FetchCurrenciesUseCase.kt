package com.saiferwp.currencyexchange.exchange.usecase

import com.saiferwp.currencyexchange.api.Api
import com.saiferwp.currencyexchange.common.FlowUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal

internal class FetchCurrenciesUseCase(
    private val api: Api
) : FlowUseCase<Unit, CurrenciesResult>() {

    override fun execute(parameters: Unit): Flow<CurrenciesResult> = flow {
        val response = api.getRates()
        val responseBody = response.body()
        val result = if (response.isSuccessful && responseBody != null) {
            CurrenciesResult.Success(
                baseCurrency = responseBody.base,
                rates = responseBody.rates
            )
        } else {
            CurrenciesResult.Failed
        }

        emit(result)
    }
}

internal sealed class CurrenciesResult {
    internal data class Success(
        val baseCurrency: String,
        val rates: Map<String, BigDecimal>
    ) : CurrenciesResult()

    internal data object Failed : CurrenciesResult()
}