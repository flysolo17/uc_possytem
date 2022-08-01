package com.flysolo.cashregister.dialog

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.flysolo.cashregister.R
import com.flysolo.cashregister.databinding.DialogTransactionBinding
import com.flysolo.cashregister.firebase.models.ItemPurchased
import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.mystore.bottomnav.storehome.viewmodels.TransactionViewModel
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*


class TransactionDialog : DialogFragment() {
    private lateinit var binding : DialogTransactionBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DialogTransactionBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val transactionViewModel = ViewModelProvider(requireActivity())[TransactionViewModel::class.java]
        transactionViewModel.getTransaction().observe(viewLifecycleOwner) {transaction ->
            if (transaction != null) {
                displayTransaction(transaction)
            }
        }
        binding.buttonOK.setOnClickListener {
            dismiss()
        }
    }

    private fun displayTransaction(transaction: Transaction) {
        binding.textCashierName.text = "Transaction by : ${transaction.transactionCashier}"
        binding.textDate.text = "Date & Time: ${setCalendarFormat(transaction.transactionTimestamp!!)}"
        binding.textItemPurchased.text = "Item Purchased: ${transaction.transactionItems?.size}"
        binding.textItemPurchasedTotal.text = "Total amount: ${computeTotalSales(transaction.transactionItems!!)}"
    }
    private fun setCalendarFormat(timestamp: Long): String? {
        val date = Date(timestamp)
        val format: Format = SimpleDateFormat("yyyy-mm-dd hh:mm aa")
        return format.format(date)
    }
    private fun computeTotalSales(items : List<ItemPurchased>): Double {
        var total = 0.0
        for (price in items){
            if (price.itemPurchasedIsRefunded != true){
                total += price.itemPurchasedPrice!!
            }
        }
        return total
    }

}