package com.saiferwp.currencyexchange.di

import com.saiferwp.currencyexchange.api.buildRemoteApiService
import com.saiferwp.currencyexchange.exchange.viewmodel.ExchangeViewModel
import com.saiferwp.currencyexchange.utils.BigDecimalsTypeAdapter
import com.squareup.moshi.Moshi
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single {
        Moshi
            .Builder()
            .add(BigDecimalsTypeAdapter())
            .build()
    }
    single { buildRemoteApiService(get()) }
}

val appModule = module {
    viewModel { ExchangeViewModel(get()) }
}