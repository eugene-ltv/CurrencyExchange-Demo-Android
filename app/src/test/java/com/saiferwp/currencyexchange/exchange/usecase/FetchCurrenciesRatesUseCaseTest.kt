package com.saiferwp.currencyexchange.exchange.usecase

import com.saiferwp.currencyexchange.api.Api
import com.saiferwp.currencyexchange.exchange.model.CurrencyRates
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.math.BigDecimal

class FetchCurrenciesRatesUseCaseTest {

    private lateinit var useCase: FetchCurrenciesRatesUseCase
    private lateinit var api: Api

    @Before
    fun setUp() {
        api = mockk()
        useCase = FetchCurrenciesRatesUseCase(api)
    }

    @Test
    fun `invoke returns Success when API call is successful`() = runBlocking {
        // Arrange
        val rates = mapOf("EUR" to BigDecimal("0.85"), "GBP" to BigDecimal("0.72"))
        val response = Response.success(CurrencyRates("USD", rates))
        coEvery { api.getCurrenciesRates() } returns response

        // Act
        val result = useCase.invoke(Unit).first()

        // Assert
        assertEquals(
            CurrenciesResult.Success("USD", rates),
            result
        )
        coVerify { api.getCurrenciesRates() }
    }

    @Test
    fun `invoke returns ServerError when API call is unsuccessful`() = runBlocking {
        // Arrange
        val response = Response.error<CurrencyRates>(400, "".toResponseBody(null))
        coEvery { api.getCurrenciesRates() } returns response

        // Act
        val result = useCase.invoke(Unit).first()

        // Assert
        assertEquals(
            CurrenciesResult.Failed,
            result
        )
        coVerify { api.getCurrenciesRates() }
    }

    @Test
    fun `invoke returns NetworkError when IOException occurs`() = runBlocking {
        // Arrange
        coEvery { api.getCurrenciesRates() } throws IOException()

        // Act
        val result = useCase.invoke(Unit).first()

        // Assert
        assertEquals(
            CurrenciesResult.Failed,
            result
        )
        coVerify { api.getCurrenciesRates() }
    }
}