package com.saiferwp.currencyexchange.utils

import com.saiferwp.currencyexchange.DECIMAL_PLACES_FOR_ROUNDING
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.math.BigDecimal
import java.math.RoundingMode

internal class BigDecimalsTypeAdapter {
    @FromJson
    fun fromJson(value: Any) = BigDecimal(value.toString())

    @ToJson
    fun toJson(value: BigDecimal) = value.toFloat()
}

private const val SCALE_FOR_COMBINING_RATES = 15

internal fun multiplyAndScaleToCents(amount: BigDecimal, exchangeRate: BigDecimal): BigDecimal {
    val convertedAmount = amount.multiply(exchangeRate)
    return convertedAmount.setScale(DECIMAL_PLACES_FOR_ROUNDING, RoundingMode.CEILING)
}

internal fun divideAndScaleRates(rateReceive: BigDecimal, rateSell: BigDecimal): BigDecimal {
    return rateReceive.divide(rateSell, SCALE_FOR_COMBINING_RATES, RoundingMode.FLOOR)
}