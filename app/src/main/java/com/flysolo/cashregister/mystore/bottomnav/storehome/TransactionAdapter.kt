package com.flysolo.cashregister.mystore.bottomnav.storehome
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.flysolo.cashregister.databinding.RowTransactionsBinding
import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.purchases.ItemPurchased
import java.text.SimpleDateFormat


class TransactionAdapter(val context: Context, options: FirestoreRecyclerOptions<Transaction?>,
                         private val onTransactionClick: OnTransactionClick) :
    FirestoreRecyclerAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(options) {
    interface OnTransactionClick{
        fun onTransactionClick(position: Int)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val dataBinding = RowTransactionsBinding.inflate(
            layoutInflater,
            parent,
            false
        )
        return TransactionViewHolder(dataBinding)
    }


    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int, model: Transaction) {
        val transaction = getItem(position)
        holder.bindTransaction(transaction)
        holder.itemView.setOnClickListener {
            onTransactionClick.onTransactionClick(position)
        }
    }

    inner class TransactionViewHolder(private val dataBinding: RowTransactionsBinding) :
        RecyclerView.ViewHolder(dataBinding.root) {
        fun bindTransaction(transaction: Transaction) {
            dataBinding.transaction = transaction
            dataBinding.textTimestamp.text = dateFormat(transaction.transactionTimestamp!!)
            dataBinding.transactionTotal.text = computeTotalSales(transaction.transactionItems!!).toString()
        }
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
