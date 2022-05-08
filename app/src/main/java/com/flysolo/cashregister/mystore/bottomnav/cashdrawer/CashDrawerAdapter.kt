package com.flysolo.cashregister.mystore.bottomnav.cashdrawer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.flysolo.cashregister.R
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.QueryDates
import com.flysolo.cashregister.firebase.models.CashDrawer
import com.flysolo.cashregister.firebase.models.Cashier
import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.firebase.models.User
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*

class CashDrawerAdapter(val context: Context, options: FirestoreRecyclerOptions<Cashier?>)
    : FirestoreRecyclerAdapter<Cashier, CashDrawerAdapter.CashDrawerViewHolder>(options){
    private lateinit var firebaseQueries  : FirebaseQueries

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CashDrawerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_cash_drawer,parent,false)
        return CashDrawerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CashDrawerViewHolder, position: Int, model: Cashier) {
        holder.getCashDrawerToday(model.cashierID!!)
        holder.getAllTransactionsToday(model.cashierName!!)
        holder.itemView.setOnClickListener {
            Toast.makeText(context,setCalendarFormat(System.currentTimeMillis()),Toast.LENGTH_SHORT).show()
        }
        firebaseQueries = FirebaseQueries(context,holder.firebaseFirestore)
        Picasso.get().load(model.cashierProfile).into(holder.shapeableImageView)
        holder.textCashierName.text = model.cashierName
        holder.buttonSaveStatingCash.setOnClickListener {
            val textStartingCash : String = holder.edtStartingCash.text.toString()
            if (textStartingCash.isEmpty()){
                holder.edtStartingCash.error = "Starting cash is empty!"
            } else {
                if (holder.edtStartingCash.text.isNotEmpty()){
                    holder.checkCashDrawer(model.cashierID!!,Integer.parseInt(textStartingCash))
                    holder.getCashDrawerToday(model.cashierID!!)
                } else {
                    holder.edtStartingCash.error = "Cash is empty"
                }
            }
        }

    }
    private fun setCalendarFormat(timestamp: Long): String? {
        val date = Date(timestamp)
        val format: Format = SimpleDateFormat("MM-dd-yyyy")
        return format.format(date)
    }
    inner class CashDrawerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var shapeableImageView: ShapeableImageView
        var textCashierName: TextView
        var edtStartingCash: EditText
        var textStartingCash: TextView
        var textCashAdded: TextView
        var textCashSalesToday: TextView
        var textTotal: TextView
        val buttonSaveStatingCash: Button
        var layoutStartingCash: LinearLayout
        val firebaseFirestore = FirebaseFirestore.getInstance()
        var cashDrawerList: MutableList<CashDrawer> = mutableListOf()
        val queryDates = QueryDates()
        var startingCash = 0
        var cashAdded = 0
        var cashSalesSinceToday = 0
        private lateinit var transactionList: MutableList<Transaction>

        init {
            shapeableImageView = itemView.findViewById(R.id.cashierProfile)
            textCashierName = itemView.findViewById(R.id.textCashierName)
            edtStartingCash = itemView.findViewById(R.id.edtStartingCash)
            textStartingCash = itemView.findViewById(R.id.textStartingCash)
            textCashAdded = itemView.findViewById(R.id.textCashAdded)
            textCashSalesToday = itemView.findViewById(R.id.textCashSalesToday)
            textTotal = itemView.findViewById(R.id.textTotal)
            buttonSaveStatingCash = itemView.findViewById(R.id.buttonSave)
            layoutStartingCash = itemView.findViewById(R.id.layoutAddStartingCash)
        }

        fun getAllTransactionsToday(cashierName: String) {
            transactionList = mutableListOf()
            val query = firebaseFirestore.collection(User.TABLE_NAME)
                .document(CashierLoginActivity.userID!!)
                .collection(Transaction.TABLE_NAME)
                .whereGreaterThan(
                    Transaction.TIMESTAMP,
                    queryDates.startOfDay(System.currentTimeMillis())
                )
                .whereLessThan(
                    Transaction.TIMESTAMP,
                    queryDates.endOfDay(System.currentTimeMillis())
                )
            query.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result) {
                        val transactions = document.toObject(Transaction::class.java)
                        if (transactions.transactionCashier.equals(cashierName)) {
                            transactionList.add(transactions)
                            cashSalesSinceToday = computeTotalSales(transactionList)
                            textCashSalesToday.text = cashSalesSinceToday.toString()
                            textTotal.text = computeTotalCashDrawerAmount(
                                startingCash,
                                cashAdded,
                                cashSalesSinceToday
                            ).toString()
                        }

                    }
                }
            }
        }

        private fun computeTotalSales(transactions: List<Transaction>): Int {
            var total = 0
            for (items in transactions) {
                for (totalSales in items.transactionItems!!)
                    if (totalSales.itemPurchasedIsRefunded != true) {
                        total += totalSales.itemPurchasedPrice!!
                    }
            }
            return total
        }



        private fun computeTotalCashDrawerAmount(
            startingCash: Int,
            cashAdded: Int,
            cashSalesSinceToday: Int
        ): Int {
            return startingCash + cashAdded + cashSalesSinceToday
        }

        fun getCashDrawerToday(cashierID: String){
            val query = firebaseFirestore.collection(User.TABLE_NAME)
                .document(CashierLoginActivity.userID!!)
                .collection(CashDrawer.TABLE_NAME)
                .document(setCalendarFormat(System.currentTimeMillis()).toString() + "-" + cashierID)
            query.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val cashDrawer = document.toObject(CashDrawer::class.java)
                    if (cashDrawer!!.cashierID.equals(cashierID)) {
                        startingCash = cashDrawer.startingCash!!
                        layoutStartingCash.visibility = if (startingCash == 0) View.VISIBLE else View.GONE
                        textStartingCash.text = startingCash.toString()
                        textTotal.text = computeTotalCashDrawerAmount(startingCash, cashAdded, cashSalesSinceToday).toString()
                    }
                }
            }
        }

        fun checkCashDrawer(cashierID: String,startingCash: Int) {
            val id : String = setCalendarFormat(System.currentTimeMillis()).toString() + "-" + cashierID
            firebaseFirestore.collection(User.TABLE_NAME)
                .document(CashierLoginActivity.userID!!)
                .collection(CashDrawer.TABLE_NAME)
                .document(id)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
                    } else {
                        val cashDrawer = CashDrawer(id, cashierID, startingCash, mutableListOf(), System.currentTimeMillis())
                        firebaseFirestore.collection(User.TABLE_NAME)
                            .document(CashierLoginActivity.userID!!)
                            .collection(CashDrawer.TABLE_NAME)
                            .document(id)
                            .set(cashDrawer)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "$startingCash is added to the drawer",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    textTotal.text = computeTotalCashDrawerAmount(
                                        startingCash,
                                        cashAdded,
                                        cashSalesSinceToday
                                    ).toString()

                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to add starting cash",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                }
            }
        }
}