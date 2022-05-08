package com.flysolo.cashregister.mystore.bottomnav.storehome.cashiersales

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.flysolo.cashregister.R
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.databinding.FragmentCashierSalesBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.mystore.bottomnav.storehome.viewmodels.TransactionViewModel
import com.google.firebase.firestore.FirebaseFirestore


class CashierSalesFragment : Fragment() {
    private lateinit var binding : FragmentCashierSalesBinding
    private lateinit var firebaseQueries: FirebaseQueries
    private lateinit var cashierSalesAdapter: CashierSalesAdapter
    private lateinit var transactionViewModel: TransactionViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCashierSalesBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionViewModel = ViewModelProvider(requireActivity()).get(TransactionViewModel::class.java)
        firebaseQueries = FirebaseQueries(requireContext(), FirebaseFirestore.getInstance())
        cashierSalesAdapter = CashierSalesAdapter(requireContext(),firebaseQueries.getCashiers(
            CashierLoginActivity.userID!!),transactionViewModel)
        binding.recyclerviewCashierSales.adapter = cashierSalesAdapter
    }

    override fun onStart() {
        super.onStart()
        cashierSalesAdapter.startListening()
    }
}