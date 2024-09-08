package com.saiferwp.currencyexchange.exchange.model

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
internal data class CurrencyRates(
    val base: String,
    val rates: Map<String, BigDecimal>
)