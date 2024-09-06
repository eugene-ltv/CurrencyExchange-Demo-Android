package com.saiferwp.currencyexchange.exchange.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.saiferwp.currencyexchange.R
import java.math.BigDecimal
import java.util.Locale

internal class AccountsAdapter :
    RecyclerView.Adapter<AccountsAdapter.ViewHolder>() {

    private var accounts: Map<String, BigDecimal> = emptyMap()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.item_text)
        val numberView: TextView = itemView.findViewById(R.id.item_number)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = accounts.entries.toList()[position]
        holder.textView.text = item.key
        holder.numberView.text = String.format(
            Locale.getDefault(),
            "%.2f",
            item.value
        )
    }

    internal fun setDataSource(accounts: Map<String, BigDecimal>) {
        this.accounts = accounts
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = accounts.size
}