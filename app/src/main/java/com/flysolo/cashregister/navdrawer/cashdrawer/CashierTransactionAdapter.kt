package com.flysolo.cashregister.navdrawer.cashdrawer

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flysolo.cashregister.databinding.RowTransactionsBinding
import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.purchases.ItemPurchased
import java.text.SimpleDateFormat

class CashierTransactionAdapter(val context: Context, private val transactionList : MutableList<Transaction>)
    : RecyclerView.Adapter<CashierTransactionAdapter.CashierTransactionViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CashierTransactionViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val dataBinding = RowTransactionsBinding.inflate(
            layoutInflater,
            parent,
            false
        )
        return CashierTransactionViewHolder(dataBinding)
    }

    override fun onBindViewHolder(holder: CashierTransactionViewHolder, position: Int) {
        val transaction = transactionList[position]
        holder.bind(transaction)
    }

    override fun getItemCount(): Int {
        return transactionList.size
    }

    inner class CashierTransactionViewHolder(private val dataBinding: RowTransactionsBinding)
        : RecyclerView.ViewHolder(dataBinding.root) {
        fun bind(transaction: Transaction) {
            dataBinding.transaction = transaction
            dataBinding.textTimestamp.text = dateFormat(transaction.transactionTimestamp!!)
            dataBinding.transactionTotal.text = computeTotalSales(transaction.transactionItems!!).toString()
        }

        private fun dateFormat(timestamp: Long): String {
            val simpleDateFormat = SimpleDateFormat("dd-MMM hh:mm a")
            return simpleDateFormat.format(timestamp)
        }
        private fun computeTotalSales(items : List<ItemPurchased>): Int {
            var total = 0
            for (price in items){
                if (price.itemPurchasedIsRefunded != true){
                    total += price.itemPurchasedPrice!!
                }
            }
            return total
        }

    }

}