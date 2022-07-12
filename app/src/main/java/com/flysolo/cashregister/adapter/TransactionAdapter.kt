package com.flysolo.cashregister.adapter
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.flysolo.cashregister.R

import com.flysolo.cashregister.firebase.models.ItemPurchased
import com.flysolo.cashregister.firebase.models.Transaction
import java.text.SimpleDateFormat


class TransactionAdapter(val context: Context, options: FirestoreRecyclerOptions<Transaction?>,
                         private val onTransactionClick: OnTransactionClick) :
    FirestoreRecyclerAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(options) {
    interface OnTransactionClick{
        fun onTransactionClick(position: Int)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val layoutInflater = LayoutInflater.from(context).inflate(R.layout.row_transactions,parent,false)

        return TransactionViewHolder(layoutInflater)
    }


    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int, model: Transaction) {
        val transaction = getItem(position)
        holder.cashier.text = transaction.transactionCashier
        holder.date.text = dateFormat(transaction.transactionTimestamp!!)
        holder.total.text = computeTotalSales(transaction.transactionItems!!).toString()
        holder.itemView.setOnClickListener {
            onTransactionClick.onTransactionClick(position)
        }
    }

    inner class TransactionViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val cashier: TextView = itemView.findViewById<TextView>(R.id.cashierName)
        val date: TextView = itemView.findViewById<TextView>(R.id.date)
        val total: TextView = itemView.findViewById<TextView>(R.id.transactionTotal)
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
