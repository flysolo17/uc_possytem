package com.flysolo.cashregister.purchases

import android.annotation.SuppressLint

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation

import androidx.recyclerview.widget.LinearLayoutManager
import com.flysolo.cashregister.databinding.FragmentPaymentBinding

import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.flysolo.cashregister.MainActivity
import com.flysolo.cashregister.R
import com.flysolo.cashregister.firebase.FirebaseQueries

import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.navdrawer.home.viewmodels.PurchasingViewModel

import java.lang.NumberFormatException


class PaymentFragment : Fragment() , View.OnClickListener{
    private lateinit var binding : FragmentPaymentBinding
    private lateinit var itemPurchasedList : MutableList<ItemPurchased>
    private lateinit var itemPurchasedAdapter: ItemPurchasedAdapter
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var purchasingViewModel: PurchasingViewModel
    private lateinit var receiptViewModel: ReceiptViewModel
    private lateinit var firebaseQueries: FirebaseQueries
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPaymentBinding.inflate(layoutInflater,container,false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        purchasingViewModel = ViewModelProvider(requireActivity()).get(PurchasingViewModel::class.java)
        receiptViewModel = ViewModelProvider(requireActivity()).get(ReceiptViewModel::class.java)
        itemPurchasedList = mutableListOf()
        firebaseFirestore = FirebaseFirestore.getInstance()
        val layoutManager = LinearLayoutManager(requireContext())
        firebaseQueries = FirebaseQueries(requireContext(),firebaseFirestore)
        binding.recyclerviewOrders.layoutManager = layoutManager
        binding.recyclerviewOrders.addItemDecoration(
            DividerItemDecoration(
                binding.recyclerviewOrders.context,
                DividerItemDecoration.VERTICAL
            )
        )
        //set text watchers
        binding.textOrderSubtotal.addTextChangedListener(paymentTextWatcher)
        binding.textCashReceived.addTextChangedListener(paymentTextWatcher)
        binding.inputCustomCash.addTextChangedListener(cashReceivedTextWatcher)
        setOnClickToTheButtons()
        swipeToDelete(binding.recyclerviewOrders)
        //TODO: observe if new order is coming
        purchasingViewModel.getPurchases().observe(viewLifecycleOwner) { purchases ->
            itemPurchasedList.addAll(purchases)
            itemPurchasedAdapter = ItemPurchasedAdapter(requireContext(), itemPurchasedList)
            binding.recyclerviewOrders.adapter = itemPurchasedAdapter
            binding.textItemCounter.text = refreshItemCounter().toString()
            binding.textOrderSubtotal.text = refreshSubtotalTotalAmount().toString()
        }
        binding.buttonPayNow.setOnClickListener{
            val transaction = Transaction(firebaseQueries.generateID(Transaction.TABLE_NAME),
            MainActivity.cashierName,
            System.currentTimeMillis(),
            itemPurchasedList)
            for (items in itemPurchasedList){
                firebaseQueries.decreaseQuantity(items.itemPurchasedID!!, items.itemPurchasedQuantity!!.toLong())
            }
            firebaseQueries.createTransaction(transaction)
            receiptViewModel.setReceipt(transaction)
            Navigation.findNavController(requireView()).navigate(R.id.action_paymentFragment_to_recieptFragment)
        }
    }
    private fun setOnClickToTheButtons(){
        binding.cardButtonCash20.setOnClickListener(this)
        binding.cardButtonCash50.setOnClickListener(this)
        binding.cardButtonCash100.setOnClickListener(this)
        binding.cardButtonCash200.setOnClickListener(this)
        binding.cardButtonCash500.setOnClickListener(this)
        binding.cardButtonCash1000.setOnClickListener(this)
        binding.cardButtonClearCash.setOnClickListener(this)
    }

    //TODO: refresh the total amount of the order
    private fun refreshSubtotalTotalAmount(): Int {
        var purchasesSubtotal = 0
        if (itemPurchasedList.isNotEmpty()) {
            for (i in itemPurchasedList.indices) {
                purchasesSubtotal += itemPurchasedList[i].itemPurchasedPrice!!
            }
        }
        return purchasesSubtotal
    }
    //TODO: get total transaction cost
    private fun transactionCost(list : List<ItemPurchased>): Int {
        var purchaseCost = 0
        for (cost in list) {
            purchaseCost += cost.itemPurchasedCost!!
        }
        return purchaseCost
    }


    //TODO: count the items
    private fun refreshItemCounter(): Int {
        var counter = 0
        if (itemPurchasedList.isNotEmpty()) {
            for (i in itemPurchasedList.indices) {
                counter += itemPurchasedList[i].itemPurchasedQuantity!!
            }
        }
        return counter
    }

    private fun swipeToDelete(recyclerView: RecyclerView?) {
        val callback =
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val pos = viewHolder.bindingAdapterPosition
                    itemPurchasedList.removeAt(pos)
                    itemPurchasedAdapter.notifyItemRemoved(pos)
                    binding.textItemCounter.text = refreshItemCounter().toString()
                    binding.textOrderSubtotal.text = refreshSubtotalTotalAmount().toString()

                }
            })
        callback.attachToRecyclerView(recyclerView)
    }


    @SuppressLint("SetTextI18n")
    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.card_button_cash_20 -> {
                binding.inputCustomCash.setText("")
                binding.textCashReceived.text = "20"
            }
            R.id.card_button_cash_50 -> {
                binding.inputCustomCash.setText("")
                binding.textCashReceived.text = "50"
            }
            R.id.card_button_cash_100 -> {
                binding.inputCustomCash.setText("")
                binding.textCashReceived.text = "100"
            }
            R.id.card_button_cash_200 -> {
                binding.inputCustomCash.setText("")
                binding.textCashReceived.text = "200"
            }
            R.id.card_button_cash_500 -> {
                binding.inputCustomCash.setText("")
                binding.textCashReceived.text = "500"
            }
            R.id.card_button_cash_1000 -> {
                binding.inputCustomCash.setText("")
                binding.textCashReceived.text = "1000"
            }
            R.id.card_button_clear_cash -> {
                binding.inputCustomCash.setText("")
                binding.textCashReceived.text = "0"
            }
            else -> {
                binding.inputCustomCash.setText("")
                binding.textCashReceived.text = "0"
            }
        }
    }

    //TextWatchers
    private val paymentTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            val subtotal: Int = binding.textOrderSubtotal.text.toString().toInt()
            val cashReceived: Int = binding.textCashReceived.text.toString().toInt()
            if (cashReceived > subtotal) {
                val total = cashReceived - subtotal
                binding.textCashChange.text = total.toString()
            } else binding.textCashChange.text = 0.toString()
        }

        override fun afterTextChanged(s: Editable) {}
    }

    private val cashReceivedTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            try {
                if (s.toString().isNotEmpty()) {
                    binding.textCashReceived.text = s.toString()
                    val cash = s.toString().toInt()
                    if (cash > 100000) {
                        s.replace(0, s.length, "0")
                    }
                } else {
                    binding.textCashReceived.text = 0.toString()
                }
            } catch (ignored: NumberFormatException) {
            }
        }
    }


}