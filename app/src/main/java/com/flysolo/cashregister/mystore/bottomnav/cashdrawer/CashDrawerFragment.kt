package com.flysolo.cashregister.mystore.bottomnav.cashdrawer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.flysolo.cashregister.R
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.databinding.FragmentCashDrawerBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.QueryDates
import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.mystore.bottomnav.storehome.cashiersales.CashierSalesAdapter
import com.flysolo.cashregister.mystore.bottomnav.storehome.viewmodels.TransactionViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*


class CashDrawerFragment : Fragment() {
    private lateinit var binding : FragmentCashDrawerBinding
    private lateinit var firebaseQueries: FirebaseQueries
    private lateinit var cashDrawerAdapter: CashDrawerAdapter
    private val queryDates = QueryDates()
    private val firebaseFirestore = FirebaseFirestore.getInstance()
    private var  transactionList : MutableList<Transaction> = mutableListOf()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        binding = FragmentCashDrawerBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseQueries = FirebaseQueries(requireContext(), firebaseFirestore)
        cashDrawerAdapter = CashDrawerAdapter(requireContext(),firebaseQueries.getCashiers(CashierLoginActivity.userID!!))
        binding.recyclerviewCashDrawer.adapter = cashDrawerAdapter
        binding.dateToday.text = setCalendarFormat(System.currentTimeMillis())
    }

    override fun onStart() {
        super.onStart()
        cashDrawerAdapter.startListening()
    }
    private fun setCalendarFormat(timestamp: Long): String? {
        val date = Date(timestamp)
        val format: Format = SimpleDateFormat("MMM dd, yyyy")
        return format.format(date)
    }

}