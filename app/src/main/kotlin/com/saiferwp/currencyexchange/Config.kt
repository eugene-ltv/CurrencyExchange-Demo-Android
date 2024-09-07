package com.saiferwp.currencyexchange

internal const val API_HOST = "https://developers.paysera.com/tasks/api/"

/**
 As API returns traditional currencies with cents rounding
 alongside digital currencies or precious metals we need to
 use maximum required decimal places for rounding (BTC as a base).
 Correct displaying requires more sophisticated rounding rules to be implemented.
 */
internal const val DECIMAL_PLACES_FOR_ROUNDING = 8
internal const val MAX_DIGITS_BEFORE_DECIMAL = 15
