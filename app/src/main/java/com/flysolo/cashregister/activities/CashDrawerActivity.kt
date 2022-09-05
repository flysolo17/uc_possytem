package com.flysolo.cashregister.activities


import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.flysolo.cashregister.R
import com.flysolo.cashregister.adapter.CashierTransactions
import com.flysolo.cashregister.adapter.TransactionAdapter
import com.flysolo.cashregister.databinding.ActivityCashDrawerBinding
import com.flysolo.cashregister.dialog.TransactionDialog
import com.flysolo.cashregister.firebase.QueryDates
import com.flysolo.cashregister.firebase.models.CashDrawer
import com.flysolo.cashregister.firebase.models.Cashier
import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.login.LoginActivity
import com.flysolo.cashregister.mystore.bottomnav.storehome.viewmodels.TransactionViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*
class CashDrawerActivity : AppCompatActivity(),CashierTransactions.OnCashierTransactionClick {
    private lateinit var binding: ActivityCashDrawerBinding
    private lateinit var dialog: BottomSheetDialog
    private lateinit var firestore: FirebaseFirestore
    private var queryDates = QueryDates()
    private var cashDrawer : CashDrawer? = null
    private lateinit var transactionList: MutableList<Transaction>
    private var today: Long = 0
    private val decimalFormat = DecimalFormat("0.00")
    private lateinit var cashierTransactions: CashierTransactions
    private lateinit var transactionViewModel: TransactionViewModel
    private fun init(cashierID: String) {
        firestore = FirebaseFirestore.getInstance()
        dialog = BottomSheetDialog(this)
        transactionViewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.clear()
        today = MaterialDatePicker.todayInUtcMilliseconds()
        cashDrawer = CashDrawer(cashierID+setCalendarFormat(today),cashierID)
        binding.recyclerviewTransactions.apply {
            layoutManager = LinearLayoutManager(this@CashDrawerActivity)
            addItemDecoration(
                DividerItemDecoration(this@CashDrawerActivity,
                    DividerItemDecoration.VERTICAL)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCashDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val cashierID = intent.getStringExtra(Cashier.CASHIER_ID)
        val cashierName = intent.getStringExtra(Cashier.CASHIER_NAME)
        init(cashierID!!)
        getAllTransactionsToday(cashDrawer!!,today, cashierName!!)
        getCashDrawerToday(cashierID,today,transactionList)
        binding.buttonSetCashDrawer.setOnClickListener {
            showDialog(cashierID, cashDrawer!!)
        }
        binding.buttonBack.setOnClickListener {
            finish()
        }
    }

    private fun getCashDrawerToday(cashierID: String,today: Long,transactions: List<Transaction>) {
        firestore.collection(User.TABLE_NAME)
            .document(LoginActivity.uid)
            .collection(CashDrawer.TABLE_NAME)
            .document(cashierID+setCalendarFormat(today)!!)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    error.printStackTrace()
                } else {
                    if (value != null) {
                        if (value.exists()) {
                            val cashDrawer = value.toObject(CashDrawer::class.java)
                            this.cashDrawer = cashDrawer
                        }
                        bindViews(cashDrawer!!,transactions)
                    }
                }
            }
        }

    private fun getAllTransactionsToday(cashDrawer: CashDrawer,date : Long,cashierName : String) {
        transactionList = mutableListOf()
        val query = firestore.collection(User.TABLE_NAME)
            .document(LoginActivity.uid)
            .collection(Transaction.TABLE_NAME)
            .whereGreaterThan(
                Transaction.TIMESTAMP,
                queryDates.startOfDay(date)
            )
            .whereLessThan(
                Transaction.TIMESTAMP,
                queryDates.endOfDay(date)
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
                cashierTransactions = CashierTransactions(binding.root.context,transactionList,this)
                binding.recyclerviewTransactions.adapter = cashierTransactions
            }
        }
    }



    private fun bindViews(cashDrawer: CashDrawer,transactions: List<Transaction>) {
        val cashAdded = computeCashAdded(cashDrawer.cashAdded)
        binding.textStartingCash.text = decimalFormat.format(cashDrawer.startingCash)
        binding.textCashAdded.text = decimalFormat.format(cashAdded)
        val total : Double = (cashDrawer.startingCash!! + cashAdded) + computeTotalSales(transactions)
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

    private fun showDialog(cashierID : String,cashDrawer: CashDrawer){
        val view = layoutInflater.inflate(R.layout.dialog_cash_drawer,null,false)
        val buttonSave : Button = view.findViewById(R.id.buttonSaveDrawer)
        val inputStartingCash : EditText = view.findViewById(R.id.inputStartingCash)
        val inputCashAdded : EditText = view.findViewById(R.id.inputCashAdded)
        val textCashAdded : TextView = view.findViewById(R.id.textCashAdded)
        inputStartingCash.setText(cashDrawer.startingCash.toString())
        textCashAdded.text = computeCashAdded(cashDrawer.cashAdded).toString()

        buttonSave.setOnClickListener {
            val startingCash = inputStartingCash.text.toString()
            val cashAdded = inputCashAdded.text.toString()
            if (startingCash.isEmpty() || startingCash == "0"){
                inputStartingCash.error = "Invalid"
            }
            else {
                if (cashAdded.isNotEmpty() && cashAdded != "0") {
                    cashDrawer.cashAdded.add(Integer.parseInt(cashAdded))
                }
                val newCashDrawer = CashDrawer(cashierID+setCalendarFormat(today),
                    cashierID
                    ,Integer.parseInt(startingCash),
                    cashDrawer.cashAdded,
                    System.currentTimeMillis())
                saveStartingCash(newCashDrawer)
            }
        }
        if (!dialog.isShowing) {
            dialog.setContentView(view)
            dialog.show()
        }

    }
    private fun setCalendarFormat(timestamp: Long): String? {
        val date = Date(timestamp)
        val pattern = "MMM-dd-yyyy"
        val format: Format = SimpleDateFormat(pattern)
        return format.format(date)
    }
    private fun computeCashAdded(list: List<Int>): Int {
        var total = 0
        for (i in list){
            total += i
        }
        return total
    }
    private fun saveStartingCash(cashDrawer: CashDrawer) {
        firestore.collection(User.TABLE_NAME).document(LoginActivity.uid)
            .collection(CashDrawer.TABLE_NAME)
            .document(cashDrawer.cashDrawerID!!)
            .set(cashDrawer)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    dialog.dismiss()
                    Toast.makeText(this,"Success",Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onTransactionClick(position: Int) {
        transactionViewModel.setTransaction(transactionList[position])
        val transactionDialog = TransactionDialog()
        if (!transactionDialog.isAdded) {
            transactionDialog.show(supportFragmentManager,"Transaction Dialog")
        }
    }




}