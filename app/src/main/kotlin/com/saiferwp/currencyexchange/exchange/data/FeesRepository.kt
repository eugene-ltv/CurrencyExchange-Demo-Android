package com.saiferwp.currencyexchange.exchange.data

import android.content.Context
import com.saiferwp.currencyexchange.exchange.model.ExchangeFeeRule
import com.saiferwp.currencyexchange.exchange.model.FromSixthExchangeIs0dot7PercentFeeRule
import java.math.BigDecimal

internal class FeesRepository(
    private val context: Context
) {
    private var numberOfSuccessfulExchanges = 0

    private val currentFeeRule: ExchangeFeeRule
        // factory here to init other rules
        get() = FromSixthExchangeIs0dot7PercentFeeRule(context)

    internal fun applyFee(
        params: ExchangeFeeRule.Params
    ): BigDecimal {
        return currentFeeRule.applyFee(params, numberOfSuccessfulExchanges)
    }

    internal fun markSuccessfulExchange() {
        numberOfSuccessfulExchanges++
    }

    fun getStringRepresentation(
        params: ExchangeFeeRule.Params
    ): String {
        return currentFeeRule.getStringRepresentation(params, numberOfSuccessfulExchanges)
    }
}

