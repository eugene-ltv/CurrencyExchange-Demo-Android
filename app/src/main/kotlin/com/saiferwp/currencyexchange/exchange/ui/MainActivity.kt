package com.saiferwp.currencyexchange.exchange.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.saiferwp.currencyexchange.DECIMAL_PLACES_FOR_ROUNDING
import com.saiferwp.currencyexchange.R
import com.saiferwp.currencyexchange.databinding.ActivityMainBinding
import com.saiferwp.currencyexchange.exchange.viewmodel.ExchangeEffect
import com.saiferwp.currencyexchange.exchange.viewmodel.ExchangeEvent
import com.saiferwp.currencyexchange.exchange.viewmodel.ExchangeUiState
import com.saiferwp.currencyexchange.exchange.viewmodel.ExchangeViewModel
import com.saiferwp.currencyexchange.utils.MoneyAmountInputFilter
import com.saiferwp.currencyexchange.utils.launchAndRepeatOnLifecycleStarted
import com.saiferwp.currencyexchange.utils.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.math.BigDecimal
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val exchangeViewModel: ExchangeViewModel by viewModel()

    private val mainBinding by viewBinding(ActivityMainBinding::inflate)

    private val accountsAdapter = AccountsAdapter()

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

        initViews()
    }

    private fun initViews() {
        mainBinding.exchangeSellCurrencySelector.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, id: Int, p3: Long) {
                    exchangeViewModel.sendEvent(ExchangeEvent.SellCurrencySelected(id))
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    // do nothing
                }
            }

        mainBinding.exchangeReceiveCurrencySelector.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, id: Int, p3: Long) {
                    exchangeViewModel.sendEvent(ExchangeEvent.ReceiveCurrencySelected(id))
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    // do nothing
                }
            }

        with(mainBinding.exchangeSellInput) {
            filters = arrayOf(MoneyAmountInputFilter())
            doOnTextChanged { text, _, _, _ ->
                exchangeViewModel.sendEvent(
                    ExchangeEvent.SellInputChanged(text.toString())
                )
            }
        }

        mainBinding.exchangeConfirmBtn.setOnClickListener {
            showSuccessDialog(state = exchangeViewModel.viewState.value)
            exchangeViewModel.sendEvent(ExchangeEvent.SubmitExchange)
            mainBinding.exchangeSellInput.text?.clear()
        }

        with(mainBinding.exchangeAccountsRecycler) {
            layoutManager = LinearLayoutManager(this.context)
            adapter = accountsAdapter
        }
    }

    private fun subscribeToViewState() {
        launchAndRepeatOnLifecycleStarted {
            exchangeViewModel.viewState.collect { state ->
                if (state.isLoading) {
                    showLoading()
                    return@collect
                } else {
                    hideLoading()
                }

                setupSelectors(state)

                accountsAdapter.setDataSource(state.accounts)

                mainBinding.exchangeReceiveInput.text =
                    String.format(Locale.getDefault(),
                        "%.${DECIMAL_PLACES_FOR_ROUNDING}f",
                        state.receiveAmount
                    )

                mainBinding.exchangeInsufficientBalanceError.visibility = if (state.showInsufficientBalance) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                if (state.sellAmount > BigDecimal.ZERO) {
                    mainBinding.exchangeFeeGroup.visibility = View.VISIBLE
                } else {
                    mainBinding.exchangeFeeGroup.visibility = View.GONE
                }
                mainBinding.exchangeFeeValue.text =
                    if (state.exchangeFee > BigDecimal.ZERO) {
                        String.format(
                            Locale.getDefault(),
                            "%f " + state.selectedCurrencyForSell,
                            state.exchangeFee
                        )
                    } else {
                        getString(R.string.fee_not_applicable)
                    }

                mainBinding.exchangeConversionRateValue.text =
                    getString(R.string.conversion_rate_value,
                        state.selectedCurrencyForSell,
                        "${state.selectedCurrencyForReceive} ${state.exchangeRate}"
                    )

                mainBinding.exchangeConfirmBtn.isEnabled = state.buttonSubmitEnabled
            }
        }

        launchAndRepeatOnLifecycleStarted {
            exchangeViewModel.effect.collect { effect->
                when (effect) {
                    ExchangeEffect.ApiFetchFailed -> showConnectionAlert()
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

    private fun setupSelectors(state: ExchangeUiState) {
        val sellSelectorListener = mainBinding.exchangeSellCurrencySelector.onItemSelectedListener
        mainBinding.exchangeSellCurrencySelector.onItemSelectedListener = null
        mainBinding.exchangeSellCurrencySelector.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            state.accounts.keys.toList()
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        mainBinding.exchangeSellCurrencySelector.setSelection(
            state.accounts.keys.toList().indexOf(state.selectedCurrencyForSell)
        )
        mainBinding.exchangeSellCurrencySelector.onItemSelectedListener = sellSelectorListener

        val receiveSelectorListener = mainBinding.exchangeReceiveCurrencySelector.onItemSelectedListener
        mainBinding.exchangeReceiveCurrencySelector.onItemSelectedListener = null
        mainBinding.exchangeReceiveCurrencySelector.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            state.availableCurrenciesForReceive
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        mainBinding.exchangeReceiveCurrencySelector.setSelection(
            state.availableCurrenciesForReceive.indexOf(state.selectedCurrencyForReceive)
        )
        mainBinding.exchangeReceiveCurrencySelector.onItemSelectedListener = receiveSelectorListener
    }

    private fun showSuccessDialog(state: ExchangeUiState) {
        val message = if (state.exchangeFee > BigDecimal.ZERO) {
            getString(
                R.string.conversion_result_message_with_fee,
                "${state.sellAmount} ${state.selectedCurrencyForSell}",
                "${state.receiveAmount} ${state.selectedCurrencyForReceive}",
                "${state.exchangeFee} ${state.selectedCurrencyForSell}"
            )
        } else {
            getString(
                R.string.conversion_result_message,
                "${state.sellAmount} ${state.selectedCurrencyForSell}",
                "${state.receiveAmount} ${state.selectedCurrencyForReceive}"
            )
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.success_alert_title)
        builder.setMessage(message)
        builder.setPositiveButton(R.string.button_done) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showConnectionAlert() {
        Toast.makeText(this, R.string.api_connection_error, Toast.LENGTH_SHORT).show()
    }
}
