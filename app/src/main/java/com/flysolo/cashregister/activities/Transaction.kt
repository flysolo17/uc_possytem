package com.flysolo.cashregister.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.util.Pair
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.flysolo.cashregister.R
import com.flysolo.cashregister.adapter.TransactionAdapter
import com.flysolo.cashregister.databinding.ActivityTransactionBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.QueryDates
import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.login.LoginActivity
import com.flysolo.cashregister.mystore.bottomnav.storehome.viewmodels.TransactionViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*

class Transaction : AppCompatActivity() ,TransactionAdapter.OnTransactionClick {
    private lateinit var binding : ActivityTransactionBinding
    private lateinit var firebaseFirestore : FirebaseFirestore
    private val queryDates = QueryDates()
    private lateinit var transactionList : MutableList<Transaction>
    private lateinit var itemList : MutableList<Items>
    private var today : Long = 0
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var firebaseQueries: FirebaseQueries
    private lateinit var transactionViewModel: TransactionViewModel
    val decimalFormat = DecimalFormat("#.##")
    private fun init() {
        transactionViewModel = ViewModelProvider(this).get(TransactionViewModel::class.java)
        transactionList = mutableListOf()
        itemList = mutableListOf()
        firebaseFirestore = FirebaseFirestore.getInstance()
        firebaseQueries = FirebaseQueries(this,firebaseFirestore)


    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.clear()
        today = MaterialDatePicker.todayInUtcMilliseconds()
        binding.buttonShowCalendar.text = setCalendarFormat(today)
        getTotalSalesSince(queryDates.startOfDay(today), queryDates.endOfDay(today))
        transactionAdapter = TransactionAdapter(this,firebaseQueries.getAllTransactions(
            queryDates.startOfDay(today),queryDates.endOfDay(today)),this)
        binding.recyclerviewTransactions.apply {
            layoutManager = LinearLayoutManager(this@Transaction)
            adapter = transactionAdapter
            addItemDecoration(
                DividerItemDecoration(this@Transaction,
                    DividerItemDecoration.VERTICAL)
            )
        }

        binding.buttonShowCalendar.setOnClickListener {
            showCalendar()
        }
        binding.buttonBack.setOnClickListener {
            finish()
        }
    }
    private fun getTotalSalesSince(startTime : Long, endTime : Long) {
        transactionList.clear()
        val query  = firebaseFirestore.collection(User.TABLE_NAME).document(LoginActivity.uid)
            .collection(Transaction.TABLE_NAME)
            .whereGreaterThan(Transaction.TIMESTAMP,startTime)
            .whereLessThan(Transaction.TIMESTAMP,endTime)
        query.get().addOnCompleteListener{ task ->
            if (task.isComplete) {
                if (task.result != null) {
                    if (task.result.isEmpty) {
                        binding.textTotalSales.text = decimalFormat.format(0)
                        binding.textTotalTransaction.text = 0.toString()
                        binding.textItemSold.text = 0.toString()
                        binding.textCostOfGoods.text =decimalFormat.format(0)
                        binding.textProfit.text =decimalFormat.format(0)
                        binding.textTotalRefunds.text = decimalFormat.format(0)
                    }
                }
            }
            if (task.isSuccessful) {
                for (document in task.result) {
                    val transactions = document.toObject(Transaction::class.java)
                    transactionList.add(transactions)
                    binding.textTotalSales.text = decimalFormat.format(computeTotalSales(transactionList))
                    binding.textTotalTransaction.text = transactionList.size.toString()
                    binding.textItemSold.text = getTotalItemSold(transactionList).toString()
                    binding.textCostOfGoods.text = decimalFormat.format(computeCostOfGoods(transactionList))
                    binding.textProfit.text = decimalFormat.format(computeTotalSales(transactionList) - computeCostOfGoods(transactionList))
                    binding.textTotalRefunds.text = decimalFormat.format(computeTotalRefunds(transactionList))
                }
            }
        }
    }
    private fun getTotalItemSold(transactions : List<Transaction>) : Int {
        var itemCount = 0
        for (items in transactions) {
            itemCount += items.transactionItems?.size!!
        }
        return itemCount
    }

    private fun computeTotalSales(transactions : List<Transaction>): Double {
        var total = 0.0
        for (items in transactions) {
            for (totalSales in items.transactionItems!!)
                if (totalSales.itemPurchasedIsRefunded != true){
                    total += totalSales.itemPurchasedPrice!!
                }
        }
        return total
    }
    private fun computeCostOfGoods(transactions : List<Transaction>): Double {
        var cost = 0.0
        for (purchaseCost in transactions) {
            for (totalCost in purchaseCost.transactionItems!!){
                cost += totalCost.itemPurchasedCost!!
            }
        }
        return cost
    }
    //TODO: get total transaction refunds
    private fun computeTotalRefunds(list : List<Transaction>): Double {
        var totalRefund = 0.0
        for (refund in list) {
            for (purchases in refund.transactionItems!!){
                if (purchases.itemPurchasedIsRefunded == true){
                    totalRefund += purchases.itemPurchasedPrice!!
                }
            }

        }
        return totalRefund
    }

    private fun setCalendarFormat(timestamp: Long): String? {
        val date = Date(timestamp)
        val format: Format = SimpleDateFormat("MMM dd, yyyy")
        return format.format(date)
    }
    private fun showCalendar(){
        val dateRangePicker =
            MaterialDatePicker
                .Builder.dateRangePicker().setSelection(Pair.create(today, today))
                .setTitleText("Select Date")
                .build()
        dateRangePicker.show(
            supportFragmentManager,
            "date_range_picker"
        )
        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            if (selection.first != null && selection.second != null) {
                binding.buttonShowCalendar.isEnabled = true
                binding.buttonShowCalendar.text = dateRangePicker.headerText
                getTotalSalesSince(queryDates.startOfDay(selection.first),queryDates.endOfDay(selection.second))
                transactionAdapter = TransactionAdapter(this,firebaseQueries.getAllTransactions(
                    queryDates.startOfDay(selection.first),queryDates.endOfDay(selection.second)),this)
                binding.recyclerviewTransactions.adapter = transactionAdapter
                transactionAdapter.startListening()

            }
        }
    }

    override fun onStart() {
        super.onStart()
        transactionAdapter.startListening()
    }

    override fun onTransactionClick(position: Int) {
        Toast.makeText(this,transactionAdapter.getItem(position).transactionCashier,
            Toast.LENGTH_SHORT).show()
        transactionViewModel.setTransaction(transactionAdapter.snapshots[position])


    }
}