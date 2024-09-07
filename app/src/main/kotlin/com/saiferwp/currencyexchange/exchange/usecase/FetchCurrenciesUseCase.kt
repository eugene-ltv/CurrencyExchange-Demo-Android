package com.saiferwp.currencyexchange.exchange.usecase

import com.saiferwp.currencyexchange.api.Api
import com.saiferwp.currencyexchange.common.FlowUseCase
import com.saiferwp.currencyexchange.exchange.model.CurrencyRates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import java.io.IOException
import java.math.BigDecimal

internal class FetchCurrenciesUseCase(
    private val api: Api
) : FlowUseCase<Unit, CurrenciesResult>() {

    override fun execute(parameters: Unit): Flow<CurrenciesResult> = flow {
        val response: Response<CurrencyRates>
        try {
            response = api.getRates()
        } catch (ex: IOException) {
            emit(CurrenciesResult.Failed)
            return@flow
        }
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
