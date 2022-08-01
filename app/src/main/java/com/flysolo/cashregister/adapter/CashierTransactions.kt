package com.flysolo.cashregister.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.flysolo.cashregister.R
import com.flysolo.cashregister.firebase.models.ItemPurchased
import com.flysolo.cashregister.firebase.models.Transaction
import java.text.DecimalFormat
import java.text.SimpleDateFormat

class CashierTransactions(val context: Context, private val transactionList:List<Transaction>,private val onCashierTransactionClick: OnCashierTransactionClick) : RecyclerView.Adapter<CashierTransactions.CashierTransactionViewHolder>() {
    interface OnCashierTransactionClick {
        fun onTransactionClick(position: Int)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CashierTransactionViewHolder {
        val layoutInflater = LayoutInflater.from(context).inflate(R.layout.row_transactions,parent,false)

        return CashierTransactionViewHolder(layoutInflater)
    }

    override fun onBindViewHolder(holder: CashierTransactionViewHolder, position: Int) {
        val transaction = transactionList[position]
        val decimalFormat= DecimalFormat("0.00")
        holder.cashier.text = transaction.transactionCashier
        holder.date.text = dateFormat(transaction.transactionTimestamp!!)
        holder.total.text = decimalFormat.format(computeTotalSales(transaction.transactionItems!!))
        holder.itemView.setOnClickListener {
            onCashierTransactionClick.onTransactionClick(position)
        }
    }

    override fun getItemCount(): Int {
        return transactionList.size
    }
    class CashierTransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cashier: TextView = itemView.findViewById(R.id.cashierName)
        val date: TextView = itemView.findViewById(R.id.date)
        val total: TextView = itemView.findViewById(R.id.transactionTotal)
    }
    private fun dateFormat(timestamp: Long): String {
        val simpleDateFormat = SimpleDateFormat("dd-MMM hh:mm a")
        return simpleDateFormat.format(timestamp)
    }
    private fun computeTotalSales(items : List<ItemPurchased>): Double {
        var total = 0.0
        for (price in items){
            if (price.itemPurchasedIsRefunded != true){
                total += price.itemPurchasedPrice!!
            }
        }
        return total
    }
}