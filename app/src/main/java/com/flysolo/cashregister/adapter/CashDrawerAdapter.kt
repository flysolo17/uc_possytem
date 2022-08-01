package com.flysolo.cashregister.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.flysolo.cashregister.R
import com.flysolo.cashregister.firebase.QueryDates
import com.flysolo.cashregister.firebase.models.CashDrawer
import com.flysolo.cashregister.firebase.models.Cashier
import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.text.DecimalFormat
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*

class CashDrawerAdapter(val context: Context, private val cashDrawerList : List<CashDrawer>) : RecyclerView.Adapter<CashDrawerAdapter.CashDrawerViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CashDrawerViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_cash_drawer,parent,false)
        return  CashDrawerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CashDrawerViewHolder, position: Int) {
        val cashDrawer = cashDrawerList[position]

        holder.textCashAdded.text = holder.decimalFormat.format(holder.computeCashAdded(cashDrawer.cashAdded))
        holder.textStartingCash.text = holder.decimalFormat.format(cashDrawer.startingCash)
        holder.displayCashierInfo(FirebaseAuth.getInstance().currentUser!!.uid,
            cashDrawer
        )


    }

    override fun getItemCount(): Int {
        return cashDrawerList.size
    }
    class CashDrawerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cashierProfile : ImageView = itemView.findViewById(R.id.cashierProfile)
        val cashierName : TextView = itemView.findViewById(R.id.textCashierName)
        val textStartingCash : TextView = itemView.findViewById(R.id.textStartingCash)
        val textCashAdded : TextView = itemView.findViewById(R.id.textCashAdded)
        val textCashSales : TextView = itemView.findViewById(R.id.textCashSales)
        val textTotal : TextView = itemView.findViewById(R.id.textTotal)
        val firestore = FirebaseFirestore.getInstance()
        val queryDates = QueryDates()
        val decimalFormat = DecimalFormat("0.00")
        lateinit var transactionList : MutableList<Transaction>
        fun displayCashierInfo(uid : String,cashDrawer: CashDrawer) {
            firestore.collection(User.TABLE_NAME)
                .document(uid)
                .collection(Cashier.TABLE_NAME)
                .document(cashDrawer.cashierID!!)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        error.printStackTrace()
                    } else {
                        if (value != null) {
                            if (value.exists()) {
                                val cashier = value.toObject(Cashier::class.java)
                                if (cashier != null) {
                                    if (cashier.cashierProfile!!.isNotEmpty()) {
                                        Picasso.get().load(cashier.cashierProfile).into(cashierProfile)
                                    }
                                    cashierName.text = cashier.cashierName
                                    getAllTransactionsToday(cashDrawer, cashier.cashierName!!)
                                }
                            }
                        }
                    }
                }
        }
        private fun getAllTransactionsToday(cashDrawer: CashDrawer,cashierName : String) {
            transactionList = mutableListOf()
            val query = firestore.collection(User.TABLE_NAME)
                .document(LoginActivity.uid)
                .collection(Transaction.TABLE_NAME)
                .whereGreaterThan(
                    Transaction.TIMESTAMP,
                    queryDates.startOfDay(cashDrawer.timestamp!!)
                )
                .whereLessThan(
                    Transaction.TIMESTAMP,
                    queryDates.endOfDay(cashDrawer.timestamp!!)
                )
            query.addSnapshotListener { value, error ->
                transactionList.clear()
                if (error != null) {
                    error.printStackTrace()
                } else {
                    value?.map { documents ->
                        val transactions = documents.toObject(Transaction::class.java)
                        if (transactions.transactionCashier.equals(cashierName)) {
                            transactionList.add(transactions)
                        }
                    }
                    bindViews(cashDrawer,transactionList)
                }
            }
        }

        private fun bindViews(cashDrawer: CashDrawer,transactions: List<Transaction>) {
            val cashAdded = computeCashAdded(cashDrawer.cashAdded)
            textCashSales.text = decimalFormat.format(computeTotalSales(transactions))
            val total : Double = (cashDrawer.startingCash!! + cashAdded) + computeTotalSales(transactions)
            textTotal.text = decimalFormat.format(total)
        }
        private fun computeTotalSales(transactions: List<Transaction>): Double {
            var total = 0.0
            for (items in transactions) {
                for (totalSales in items.transactionItems!!)
                    if (totalSales.itemPurchasedIsRefunded != true) {
                        total += totalSales.itemPurchasedPrice!!
                    }
            }
            return total
        }
        fun computeCashAdded(list: List<Int>): Int {
            var total = 0
            for (i in list){
                total += i
            }
            return total
        }
    }



}