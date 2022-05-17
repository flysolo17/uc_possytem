package com.flysolo.cashregister.activities


import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.flysolo.cashregister.R
import com.flysolo.cashregister.databinding.ActivityCashDrawerBinding
import com.flysolo.cashregister.firebase.QueryDates
import com.flysolo.cashregister.firebase.models.CashDrawer
import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.login.LoginActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*
class CashDrawerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCashDrawerBinding
    private lateinit var dialog: BottomSheetDialog
    private lateinit var firestore: FirebaseFirestore
    private var cashAddedList: MutableList<Int> = mutableListOf()
    private var queryDates = QueryDates()
    private var cashDrawer : CashDrawer? = null
    private lateinit var transactionList: MutableList<Transaction>
    private var today: Long = 0
    private var startingCash = 0
    private var cashAdded = 0
    private var  sales = 0
    private fun init() {
        firestore = FirebaseFirestore.getInstance()
        dialog = BottomSheetDialog(this)
        transactionList = mutableListOf()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.clear()
        today = MaterialDatePicker.todayInUtcMilliseconds()
        getAllTransactionsToday(today)
        getCashDrawerToday(today)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCashDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        binding.buttonSetCashDrawer.setOnClickListener {
            showDialog()
        }
        binding.buttonBack.setOnClickListener {
            finish()
        }
        val datePicker = MaterialDatePicker.Builder.datePicker().build()
        binding.buttonPickDate.setOnClickListener { v: View? ->
            datePicker.show(supportFragmentManager, "DATE_PICKER")
            binding.buttonPickDate.isEnabled = false
        }

        datePicker.addOnPositiveButtonClickListener { selection: Long? ->
            if (selection != null) {
                binding.buttonPickDate.text = datePicker.headerText
                binding.buttonPickDate.isEnabled = true
                getAllTransactionsToday(selection)
                getCashDrawerToday(selection)
            }
        }
        datePicker.addOnDismissListener {
            binding.buttonPickDate.isEnabled = true
        }
        datePicker.addOnCancelListener {
            binding.buttonPickDate.isEnabled = true
        }
    }

    private fun getCashDrawerToday(today: Long) {
        startingCash = 0
        cashAdded = 0
        cashAddedList.clear()

        firestore.collection(User.TABLE_NAME)
            .document(LoginActivity.uid)
            .collection(CashDrawer.TABLE_NAME)
            .document(setCalendarFormat(today)!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val cashDrawer = document.toObject(CashDrawer::class.java)
                    if (cashDrawer != null) {
                        this.cashDrawer = cashDrawer
                        cashAddedList.addAll(cashDrawer.cashAdded)
                        startingCash = cashDrawer.startingCash!!
                        cashAdded = computeCashAdded(cashAddedList)
                        bindViews(startingCash, cashAdded,transactionList)
                    }
                }
                else {
                    bindViews(startingCash,cashAdded,transactionList)
                }
            }
        }

    private fun getAllTransactionsToday(date : Long) {
        transactionList.clear()
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
        query.get()

          .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (task.result != null) {
                    for (document in task.result) {
                        val transactions = document.toObject(Transaction::class.java)
                        transactionList.add(transactions)

                        sales = if (transactionList.size != 0) {
                            computeTotalSales(transactionList)
                        } else {
                            0
                        }
                        binding.textCashSales.text = sales.toString()
                    }
                }
            }
        }
    }

    private fun bindViews(startingCash : Int ,cashAdded : Int,transactions: List<Transaction>) {
        binding.textStartingCash.text = startingCash.toString()
        binding.textCashAdded.text = cashAdded.toString()
        binding.textCashSales.text = computeTotalSales(transactions).toString()
        val total : Int = (startingCash + cashAdded) + computeTotalSales(transactions)
        binding.textTotal.text = total.toString()
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

    private fun showDialog(){
        cashAddedList.clear()
        val view = layoutInflater.inflate(R.layout.dialog_cash_drawer,null,false)
        val buttonSave : Button = view.findViewById(R.id.buttonSaveDrawer)
        val inputStartingCash : EditText = view.findViewById(R.id.inputStartingCash)
        val inputCashAdded : EditText = view.findViewById(R.id.inputCashAdded)
        val textCashAdded : TextView = view.findViewById(R.id.textCashAdded)
        if (cashDrawer != null) {
            cashAddedList.addAll(cashDrawer!!.cashAdded)
            inputStartingCash.setText(cashDrawer!!.startingCash.toString())
            textCashAdded.text = computeCashAdded(cashAddedList).toString()
        }

        buttonSave.setOnClickListener {
            val startingCash = inputStartingCash.text.toString()
            val cashAdded = inputCashAdded.text.toString()
            if (cashAdded.isNotEmpty() && cashAdded != "0") {
                cashAddedList.add(Integer.parseInt(cashAdded))
            }
            if (startingCash.isEmpty() || startingCash == "0"){
                inputStartingCash.error = "Invalid"
            }
            else {
                val cashDrawer = CashDrawer(setCalendarFormat(today)
                    ,Integer.parseInt(startingCash),
                    cashAddedList,
                    System.currentTimeMillis())
                saveStartingCash(cashDrawer)
                cashAddedList.clear()
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
                    getAllTransactionsToday(today)
                    getCashDrawerToday(today)
                }
            }
    }

}