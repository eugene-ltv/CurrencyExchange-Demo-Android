package com.saiferwp.currencyexchange.exchange.model

import android.content.Context
import com.saiferwp.currencyexchange.R
import com.saiferwp.currencyexchange.utils.multiplyAndScaleToCents
import java.math.BigDecimal
import java.util.Locale

interface ExchangeFeeRule {

    fun applyFee(params: Params, numberOfSuccessfulExchanges: Int): BigDecimal
    fun getStringRepresentation(params: Params, numberOfSuccessfulExchanges: Int): String

    data class Params(
        // we can extend for more params
        val baseCurrency: String,
        val amount: BigDecimal
    )
}

internal class FromSixthExchangeIs0dot7PercentFeeRule(
    private val context: Context
) : ExchangeFeeRule {

    private val feePercent = BigDecimal(0.007)
    private val maxNumberOfFreeExchanges = 5

    override fun applyFee(params: ExchangeFeeRule.Params, numberOfSuccessfulExchanges: Int): BigDecimal {
        return if (numberOfSuccessfulExchanges > maxNumberOfFreeExchanges) {
            multiplyAndScaleToCents(params.amount, feePercent)
        } else {
            BigDecimal.ZERO
        }
    }

    override fun getStringRepresentation(
        params: ExchangeFeeRule.Params,
        numberOfSuccessfulExchanges: Int
    ): String {
        return if (numberOfSuccessfulExchanges > maxNumberOfFreeExchanges) {
            String.format(Locale.getDefault(), "%.2f " + params.baseCurrency,
                multiplyAndScaleToCents(params.amount, feePercent)
            )
        } else {
            context.getString(R.string.fee_not_applicable)
        }
    }
}