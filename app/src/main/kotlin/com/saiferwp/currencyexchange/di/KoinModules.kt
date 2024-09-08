package com.saiferwp.currencyexchange.di

import com.saiferwp.currencyexchange.api.buildRemoteApiService
import com.saiferwp.currencyexchange.exchange.data.FeesRepository
import com.saiferwp.currencyexchange.exchange.usecase.FetchCurrenciesRatesUseCase
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
    single { buildRemoteApiService(moshi = get()) }
}

val appModule = module {
    factory { FetchCurrenciesRatesUseCase(api = get()) }
    viewModel { ExchangeViewModel(
        fetchCurrenciesRatesUseCase = get(),
        feesRepository = get()
    ) }
    single { FeesRepository() }
}