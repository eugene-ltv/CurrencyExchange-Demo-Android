package com.saiferwp.currencyexchange.exchange.model

import com.saiferwp.currencyexchange.utils.multiplyAndScaleToCents
import java.math.BigDecimal

interface ExchangeFeeRule {

    fun applyFee(params: Params, numberOfSuccessfulExchanges: Int): BigDecimal

    data class Params(
        // we can extend for more params
        val baseCurrency: String,
        val amount: BigDecimal
    )
}

internal class FromSixthExchangeIs0dot7PercentFeeRule : ExchangeFeeRule {

    private val feePercent = BigDecimal("0.007")
    private val maxNumberOfFreeExchanges = 5

    override fun applyFee(params: ExchangeFeeRule.Params, numberOfSuccessfulExchanges: Int): BigDecimal {
        return if (numberOfSuccessfulExchanges > maxNumberOfFreeExchanges) {
            multiplyAndScaleToCents(params.amount, feePercent)
        } else {
            BigDecimal.ZERO
        }
    }
}