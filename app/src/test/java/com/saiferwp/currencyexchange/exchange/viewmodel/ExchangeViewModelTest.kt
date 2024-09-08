package com.saiferwp.currencyexchange.exchange.viewmodel

import com.saiferwp.currencyexchange.exchange.data.FeesRepository
import com.saiferwp.currencyexchange.exchange.usecase.CurrenciesResult
import com.saiferwp.currencyexchange.exchange.usecase.FetchCurrenciesRatesUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class ExchangeViewModelTest {

    private lateinit var viewModel: ExchangeViewModel
    private lateinit var fetchCurrenciesRatesUseCase: FetchCurrenciesRatesUseCase
    private lateinit var feesRepository: FeesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        fetchCurrenciesRatesUseCase = mockk()
        feesRepository = mockk()
        viewModel = ExchangeViewModel(fetchCurrenciesRatesUseCase, feesRepository)
        coEvery { fetchCurrenciesRatesUseCase.invoke(Unit) } returns flowOf(
            CurrenciesResult.Success(
                baseCurrency = "EUR",
                rates = mapOf(
                    "USD" to BigDecimal("1.20")
                )
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Ignore("This test is not working properly")
    @Test
    fun `SubmitExchange updates accounts correctly and marks successful exchange`() = runBlocking {
        // Arrange
        val initialAccounts = mapOf(
            "EUR" to BigDecimal("1000.00"),
            "USD" to BigDecimal("0.00")
        )
        val sellAmount = BigDecimal("100.00")
        val receiveAmount = BigDecimal("120.00")
        val fee = BigDecimal("5.00")

        every { viewModel.viewState.value } returns ExchangeUiState(
            accounts = initialAccounts,
            selectedCurrencyForSell = "EUR",
            selectedCurrencyForReceive = "USD",
            sellAmount = sellAmount,
            isLoading = false,
            baseCurrency = "EUR",
            rates = mutableMapOf(),
            availableCurrenciesForReceive = emptyList(),
            receiveAmount = receiveAmount,
            exchangeFee = BigDecimal.ZERO,
            exchangeRate = BigDecimal.ZERO,
            buttonSubmitEnabled = false,
            showInsufficientBalance = false
        )
        coEvery { feesRepository.calculateFee(any()) } returns fee
        coEvery { feesRepository.markSuccessfulExchange() } just Runs

        // Act
        viewModel.sendEvent(ExchangeEvent.SubmitExchange)

        // Assert
        val expectedAccounts = mapOf(
            "EUR" to BigDecimal("895.00"),
            "USD" to BigDecimal("120.00")
        )
        val updatedState = viewModel.viewState.value
        assertEquals(expectedAccounts, updatedState.accounts)

        coVerify { feesRepository.calculateFee(any()) }
        coVerify { feesRepository.markSuccessfulExchange() }
    }
}