package com.saiferwp.currencyexchange.exchange.data

import com.saiferwp.currencyexchange.exchange.model.ExchangeFeeRule
import com.saiferwp.currencyexchange.exchange.model.FromSixthExchangeIs0dot7PercentFeeRule
import java.math.BigDecimal

internal class FeesRepository {
    private var numberOfSuccessfulExchanges = 0

    private val currentFeeRule: ExchangeFeeRule
        // factory here to init other rules
        get() = FromSixthExchangeIs0dot7PercentFeeRule()

    internal fun calculateFee(
        params: ExchangeFeeRule.Params
    ): BigDecimal {
        return currentFeeRule.applyFee(params, numberOfSuccessfulExchanges)
    }

    internal fun markSuccessfulExchange() {
        numberOfSuccessfulExchanges++
    }
}

