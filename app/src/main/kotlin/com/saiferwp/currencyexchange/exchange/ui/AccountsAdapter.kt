package com.saiferwp.currencyexchange.exchange.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.saiferwp.currencyexchange.DECIMAL_PLACES_FOR_ROUNDING
import com.saiferwp.currencyexchange.R
import java.math.BigDecimal
import java.util.Locale

internal class AccountsAdapter :
    RecyclerView.Adapter<AccountsAdapter.ViewHolder>() {

    private var accounts: Map<String, BigDecimal> = emptyMap()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.item_currency)
        val numberView: TextView = itemView.findViewById(R.id.item_amount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account_layout, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = accounts.entries.toList()[position]
        holder.textView.text = item.key
        holder.numberView.text = String.format(
            Locale.getDefault(),
            "%.${DECIMAL_PLACES_FOR_ROUNDING}f",
            item.value
        )
    }

    internal fun setDataSource(accounts: Map<String, BigDecimal>) {
        this.accounts = accounts
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = accounts.size
}