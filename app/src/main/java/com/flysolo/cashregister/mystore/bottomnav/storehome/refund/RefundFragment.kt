package com.flysolo.cashregister.mystore.bottomnav.storehome.refund

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.flysolo.cashregister.R
import com.flysolo.cashregister.databinding.FragmentRefundBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.mystore.bottomnav.storehome.viewmodels.TransactionViewModel
import com.flysolo.cashregister.purchases.ItemPurchased
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat


class RefundFragment : DialogFragment() {
    private lateinit var binding : FragmentRefundBinding
    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var itemPurchasedList : MutableList<ItemPurchased>
    private lateinit var firebaseQueries: FirebaseQueries
    private var transactionID : String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentRefundBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseQueries = FirebaseQueries(requireContext(), FirebaseFirestore.getInstance())
        itemPurchasedList = mutableListOf()
        transactionViewModel = ViewModelProvider(requireActivity()).get(TransactionViewModel::class.java)
        transactionViewModel.getTransaction().observe(viewLifecycleOwner){ transaction ->
            transactionID = transaction.transactionID
            for (itemPurchased in transaction.transactionItems!!){
                itemPurchasedList.add(itemPurchased)
                addView(itemPurchased)
            }

            binding.textCashierName.text = transaction.transactionCashier
            binding.textTransactionDate.text = dateFormat(transaction.transactionTimestamp!!)
            binding.textTransactionTotal.text = computeTotalSales(transaction.transactionItems).toString()
        }
        binding.fabSaveTransaction.setOnClickListener {
            firebaseQueries.updateTransaction(transactionID!!,itemPurchasedList)
        }
    }
    @SuppressLint("SetTextI18n")
    private fun addView(itemPurchased: ItemPurchased) {
        val itemPurchasedView: View = layoutInflater.inflate(R.layout.row_refund, null, false)
        val textName = itemPurchasedView.findViewById<TextView>(R.id.textItemName)
        val textPrice = itemPurchasedView.findViewById<TextView>(R.id.textItemPurchasedPrice)
        val buttonRefund = itemPurchasedView.findViewById<Button>(R.id.buttonRefund)
        textName.text = itemPurchased.itemPurchasedName + " x" + itemPurchased.itemPurchasedQuantity
        textPrice.text = itemPurchased.itemPurchasedPrice.toString()
        if (itemPurchased.itemPurchasedIsRefunded == true){
            buttonRefund.setBackgroundColor(Color.RED)
            buttonRefund.text = "Refunded"
            buttonRefund.setTextColor(Color.WHITE)
            buttonRefund.isEnabled = false
        }
        buttonRefund.setOnClickListener {
            itemPurchased.itemPurchasedIsRefunded = true
            itemPurchasedList[binding.linearItemPurchased.verticalScrollbarPosition].itemPurchasedIsRefunded = true
            buttonRefund.setBackgroundColor(Color.RED)
            buttonRefund.text = "Refunded"
            buttonRefund.setTextColor(Color.WHITE)
            buttonRefund.isEnabled = false
        }
        binding.linearItemPurchased.addView(itemPurchasedView)
    }
    private fun dateFormat(timestamp: Long): String {
        val simpleDateFormat = SimpleDateFormat("dd-MMM hh:mm a")
        return simpleDateFormat.format(timestamp)
    }
    private fun computeTotalSales(items : List<ItemPurchased>): Int {
        var total = 0
        for (price in items){
            total += price.itemPurchasedPrice!!
        }
        return total
    }


}