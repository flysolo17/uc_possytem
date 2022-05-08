package com.flysolo.cashregister.navdrawer.cashdrawer

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.flysolo.cashregister.DataBinderMapperImpl
import com.flysolo.cashregister.MainActivity
import com.flysolo.cashregister.R
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.databinding.FragmentCashDrawerBinding
import com.flysolo.cashregister.databinding.FragmentCashierCashDrawerBinding
import com.flysolo.cashregister.firebase.QueryDates
import com.flysolo.cashregister.firebase.models.CashDrawer
import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.navdrawer.cashdrawer.cashadded.CashAddedFragment
import com.flysolo.cashregister.navdrawer.cashdrawer.viewmodel.CashDrawerViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*


class CashierCashDrawerFragment : Fragment() {
    private lateinit var binding : FragmentCashierCashDrawerBinding
    private val firebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var transactionList : MutableList<Transaction>
    private lateinit var adapter: CashierTransactionAdapter
    private val queryDates = QueryDates()
    private lateinit var cashDrawerViewModel : CashDrawerViewModel
    private var cashDrawer : CashDrawer? = null
    private var startingCash  = 0
    private var cashAdded  = 0
    private var cashSalesToday  = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =  FragmentCashierCashDrawerBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cashDrawerViewModel = ViewModelProvider(requireActivity()).get(CashDrawerViewModel::class.java)
        transactionList = mutableListOf()
        getCashDrawerToday(MainActivity.cashierID)
        getAllTransactionsToday(MainActivity.cashierName)
        binding.recyclerviewTransactions.layoutManager = LinearLayoutManager(requireContext())
        adapter = CashierTransactionAdapter(requireContext(),transactionList)
        binding.recyclerviewTransactions.adapter = adapter
        binding.buttonAddCash.setOnClickListener {
            if (cashDrawer == null){
                Toast.makeText(requireContext(),"Unable to put cash!",Toast.LENGTH_SHORT).show()
            } else {
                val cashAddedFragment = CashAddedFragment()
                cashAddedFragment.show(parentFragmentManager,"Cash Added")
                cashDrawerViewModel.setCashDrawer(cashDrawer!!)
            }
        }
    }
    private fun getCashDrawerToday(cashierID: String){
        val query = firebaseFirestore.collection(User.TABLE_NAME)
            .document(CashierLoginActivity.userID!!)
            .collection(CashDrawer.TABLE_NAME)
            .document(setCalendarFormat(System.currentTimeMillis()).toString() + "-" + cashierID)
        query.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val cashDrawer = document.toObject(CashDrawer::class.java)
                if (cashDrawer!!.cashierID.equals(cashierID)) {
                    this.cashDrawer = cashDrawer
                    startingCash = cashDrawer.startingCash!!
                    cashAdded = computeCashAdded(cashDrawer.cashAdded)
                    binding.textStartingCash.text = startingCash.toString()
                    binding.textCashAdded.text = cashAdded.toString()
                    binding.textTotal.text = computeTotalInDrawer(startingCash,cashAdded,cashSalesToday).toString()
                }
            }
        }
    }

    private fun setCalendarFormat(timestamp: Long): String? {
        val date = Date(timestamp)
        val format: Format = SimpleDateFormat("MM-dd-yyyy")
        return format.format(date)
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun getAllTransactionsToday(cashierName: String) {
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
            ).orderBy(Transaction.TIMESTAMP,Query.Direction.DESCENDING)
        query.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                for (document in task.result) {
                    val transactions = document.toObject(Transaction::class.java)
                    if (transactions.transactionCashier.equals(cashierName)) {
                        transactionList.add(transactions)
                        adapter.notifyDataSetChanged()
                        cashSalesToday = computeTotalSales(transactionList)
                        binding.textCashSalesToday.text = cashSalesToday.toString()
                        binding.textTotal.text = computeTotalInDrawer(startingCash,cashAdded,cashSalesToday).toString()
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

    private fun computeCashAdded(list : List<Int>): Int {
        var total = 0
        for (cash in list) {
          total += cash
        }
        return total
    }

    private fun computeTotalInDrawer(startingCash : Int,cashAdded : Int, cashSalesToday: Int): Int {
        return startingCash + cashAdded + cashSalesToday
    }
}