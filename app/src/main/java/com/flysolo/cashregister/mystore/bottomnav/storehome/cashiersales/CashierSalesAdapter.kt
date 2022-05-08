package com.flysolo.cashregister.mystore.bottomnav.storehome.cashiersales

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.flysolo.cashregister.R
import com.flysolo.cashregister.databinding.FragmentStoreDashboardBinding

import com.flysolo.cashregister.firebase.models.Cashier
import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.mystore.bottomnav.storehome.viewmodels.TransactionViewModel
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class CashierSalesAdapter (val context: Context, options: FirestoreRecyclerOptions<Cashier?>,
                           private val transactionViewModel: TransactionViewModel)
    : FirestoreRecyclerAdapter<Cashier, CashierSalesAdapter.CashierSalesViewHolder>(options) {
    private lateinit var list: MutableList<Transaction>
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CashierSalesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_cashier_sales_today,parent,false)
        return CashierSalesViewHolder(view)
    }

    override fun onBindViewHolder(holder: CashierSalesViewHolder, position: Int, model: Cashier) {
        Picasso.get().load(model.cashierProfile).into(holder.shapeableImageView)
        holder.textCashierName.text = model.cashierName
        val lifecycleOwner = context as LifecycleOwner
        list = mutableListOf()
        transactionViewModel.getTransactionList().observe(lifecycleOwner) { transactionList ->
            for (transactions in transactionList){
                if (transactions.transactionCashier.equals(model.cashierName)){
                    list.add(transactions)
                }
            }
        }
        holder.textCashierTotalSales.text = computeTotalSales(list).toString()
        holder.textCashierItemSold.text = getTotalItemSold(list).toString()
        holder.textCashierTotalTransactions.text = list.size.toString()
    }
    class CashierSalesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
         var shapeableImageView : ShapeableImageView
         var textCashierName : TextView
         var textCashierTotalSales : TextView
        var textCashierTotalTransactions : TextView
        var textCashierItemSold : TextView
        init {
            shapeableImageView = itemView.findViewById(R.id.cashierProfile)
            textCashierName = itemView.findViewById(R.id.textCashierName)
            textCashierTotalSales = itemView.findViewById(R.id.textCashierTotalSales)
            textCashierTotalTransactions= itemView.findViewById(R.id.textCashierTotalTransactions)
            textCashierItemSold= itemView.findViewById(R.id.textCashierItemSold)
        }
    }
    private fun getTotalItemSold(transactions : List<Transaction>) : Int {
        var itemCount = 0
        for (items in transactions) {
            itemCount += items.transactionItems?.size!!
        }
        return itemCount
    }

    private fun computeTotalSales(transactions : List<Transaction>): Int {
        var total = 0
        for (items in transactions) {
            for (totalSales in items.transactionItems!!)
                if (totalSales.itemPurchasedIsRefunded != true){
                    total += totalSales.itemPurchasedPrice!!
                }
        }
        return total
    }

}