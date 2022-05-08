package com.flysolo.cashregister.mystore.bottomnav.storehome

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.util.Pair
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.flysolo.cashregister.R
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.databinding.FragmentStoreDashboardBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.QueryDates
import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.mystore.bottomnav.storehome.refund.RefundFragment
import com.flysolo.cashregister.mystore.bottomnav.storehome.viewmodels.TransactionViewModel
import com.flysolo.cashregister.purchases.ReceiptViewModel

import com.google.android.material.datepicker.MaterialDatePicker

import com.google.firebase.firestore.FirebaseFirestore
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*


class StoreDashBoardFragment : Fragment(),TransactionAdapter.OnTransactionClick {
    private lateinit var binding : FragmentStoreDashboardBinding
    private lateinit var firebaseFirestore : FirebaseFirestore
    private val queryDates = QueryDates()
    private lateinit var transactionList : MutableList<Transaction>
    private lateinit var itemList : MutableList<Items>
    private var today : Long = 0
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var firebaseQueries: FirebaseQueries
    private lateinit var transactionViewModel: TransactionViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentStoreDashboardBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionViewModel = ViewModelProvider(requireActivity()).get(TransactionViewModel::class.java)
        transactionList = mutableListOf()
        itemList = mutableListOf()
        firebaseFirestore = FirebaseFirestore.getInstance()
        firebaseQueries = FirebaseQueries(requireContext(),firebaseFirestore)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.clear()
        today = MaterialDatePicker.todayInUtcMilliseconds()
        binding.buttonShowCalendar.text = setCalendarFormat(today)
        getTotalSalesSince(queryDates.startOfDay(today), queryDates.endOfDay(today))
        binding.buttonShowCalendar.setOnClickListener {
           showCalendar()
        }
        binding.recyclerviewTransactions.layoutManager = LinearLayoutManager(requireContext())
        transactionAdapter = TransactionAdapter(requireContext(),firebaseQueries.getAllTransactions(
           queryDates.startOfDay(today),queryDates.endOfDay(today)),this)
        binding.recyclerviewTransactions.adapter = transactionAdapter
        binding.recyclerviewTransactions.addItemDecoration(
            DividerItemDecoration(
                binding.recyclerviewTransactions.context,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.buttonCashierSales.setOnClickListener {
            transactionViewModel.setTransactionList(transactionList)
            Navigation.findNavController(requireView()).navigate(R.id.action_nav_store_dashboard_to_cashierSalesFragment)
        }
    }
    private fun getTotalSalesSince(startTime : Long, endTime : Long) {
        transactionList.clear()
        val query  = firebaseFirestore.collection(User.TABLE_NAME).document(CashierLoginActivity.userID!!)
            .collection(Transaction.TABLE_NAME)
            .whereGreaterThan(Transaction.TIMESTAMP,startTime)
            .whereLessThan(Transaction.TIMESTAMP,endTime)
        query.get().addOnCompleteListener{ task ->
            if (task.isComplete) {
                if (task.result != null) {
                    if (task.result.isEmpty) {
                        binding.textTotalSales.text =0.toString()
                        binding.textTotalTransaction.text = 0.toString()
                        binding.textItemSold.text = 0.toString()
                        binding.textCostOfGoods.text = 0.toString()
                        binding.textProfit.text = 0.toString()
                        binding.textTotalRefunds.text = 0.toString()
                    }
                }
            }
            if (task.isSuccessful) {
                for (document in task.result) {
                    val transactions = document.toObject(Transaction::class.java)
                    transactionList.add(transactions)
                    binding.textTotalSales.text = computeTotalSales(transactionList).toString()
                    binding.textTotalTransaction.text = transactionList.size.toString()
                    binding.textItemSold.text = getTotalItemSold(transactionList).toString()
                    binding.textCostOfGoods.text = computeCostOfGoods(transactionList).toString()
                    binding.textProfit.text = (computeTotalSales(transactionList) - computeCostOfGoods(transactionList)).toString()
                    binding.textTotalRefunds.text = computeTotalRefunds(transactionList).toString()
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
    private fun computeCostOfGoods(transactions : List<Transaction>): Int {
        var cost = 0
        for (purchaseCost in transactions) {
           for (totalCost in purchaseCost.transactionItems!!){
               cost += totalCost.itemPurchasedCost!!
           }
        }
        return cost
    }
    //TODO: get total transaction refunds
    private fun computeTotalRefunds(list : List<Transaction>): Int {
        var totalRefund = 0
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
            parentFragmentManager,
            "date_range_picker"
        )
        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            if (selection.first != null && selection.second != null) {
                binding.buttonShowCalendar.isEnabled = true
                binding.buttonShowCalendar.text = dateRangePicker.headerText
                getTotalSalesSince(queryDates.startOfDay(selection.first),queryDates.endOfDay(selection.second))
                transactionAdapter = TransactionAdapter(requireContext(),firebaseQueries.getAllTransactions(
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
        Toast.makeText(requireContext(),transactionAdapter.getItem(position).transactionCashier,Toast.LENGTH_SHORT).show()
        transactionViewModel.setTransaction(transactionAdapter.snapshots[position])
        val refundFragment = RefundFragment()
        if (!refundFragment.isAdded){
            refundFragment.show(parentFragmentManager,"Refund")
        }

    }
}