package com.saiferwp.currencyexchange.api

import com.saiferwp.currencyexchange.API_HOST
import com.saiferwp.currencyexchange.BuildConfig
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

private const val API_REQUEST_TIME_OUT_SECONDS = 10L

private val okHttpClient = OkHttpClient().newBuilder()
    .addInterceptor(createLoggingInterceptor())
    .connectTimeout(API_REQUEST_TIME_OUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(API_REQUEST_TIME_OUT_SECONDS, TimeUnit.SECONDS)
    .build()

internal fun buildRemoteApiService(
    moshi: Moshi
): Api = Retrofit.Builder()
    .client(okHttpClient)
    .baseUrl(API_HOST)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()
    .create(Api::class.java)

internal fun createLoggingInterceptor(): HttpLoggingInterceptor =
    HttpLoggingInterceptor()
        .apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
