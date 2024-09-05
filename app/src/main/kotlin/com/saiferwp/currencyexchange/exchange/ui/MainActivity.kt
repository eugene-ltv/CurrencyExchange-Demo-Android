package com.saiferwp.currencyexchange.exchange.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.saiferwp.currencyexchange.R
import com.saiferwp.currencyexchange.databinding.ActivityMainBinding
import com.saiferwp.currencyexchange.exchange.viewmodel.ExchangeEvent
import com.saiferwp.currencyexchange.exchange.viewmodel.ExchangeUiState
import com.saiferwp.currencyexchange.exchange.viewmodel.ExchangeViewModel
import com.saiferwp.currencyexchange.utils.launchAndRepeatOnLifecycleStarted
import com.saiferwp.currencyexchange.utils.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val exchangeViewModel: ExchangeViewModel by viewModel()

    private val mainBinding by viewBinding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(mainBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.exchange_main_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        exchangeViewModel.sendEvent(ExchangeEvent.FetchRates)

        subscribeToViewState()
    }

    private fun subscribeToViewState() {
        launchAndRepeatOnLifecycleStarted {
            exchangeViewModel.viewState.collect { state ->
                when (state) {
                    ExchangeUiState.Initial -> {
                        // do nothing
                    }
                    ExchangeUiState.Loading -> {
                        showLoading()
                    }

                    is ExchangeUiState.Loaded -> {
                        hideLoading()
                        setupSelectors(state)
                    }
                }
            }
        }
    }

    private fun showLoading() {
        mainBinding.exchangeContentLayout.visibility = View.GONE
        mainBinding.exchangeLoadingProgress.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        mainBinding.exchangeLoadingProgress.visibility = View.GONE
        mainBinding.exchangeContentLayout.visibility = View.VISIBLE
    }

    private fun setupSelectors(state: ExchangeUiState.Loaded) {
        mainBinding.exchangeSellCurrencySelector.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            state.availableAccounts
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        mainBinding.exchangeReceiveCurrencySelector.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            state.availableCurrencies
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }
}
