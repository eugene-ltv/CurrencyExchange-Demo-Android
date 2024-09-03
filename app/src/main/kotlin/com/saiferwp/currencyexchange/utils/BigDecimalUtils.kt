package com.saiferwp.currencyexchange.utils

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.math.BigDecimal

internal class BigDecimalsTypeAdapter {
    @FromJson
    fun fromJson(value: Any) = BigDecimal(value.toString())

    @ToJson
    fun toJson(value: BigDecimal) = value.toFloat()
}
