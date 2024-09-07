package com.saiferwp.currencyexchange

internal const val API_HOST = "https://developers.paysera.com/tasks/api/"

/**
 As API returns traditional currencies with cents rounding
 alongside digital currencies and precious metals.
 For better visual representation of currencies amount we will use rounding to cents.

 But for correct calculating and displaying currencies as BTC or precious metals
 up to 8 decimal places needed. And it requires more sophisticated rounding rules implemented
 for each currency.
 */
internal const val DECIMAL_PLACES_FOR_ROUNDING = 2
internal const val MAX_DIGITS_BEFORE_DECIMAL = 15
