package com.flysolo.cashregister.navdrawer.home.tablayout

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.databinding.FragmentItemListBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.navdrawer.home.adapter.ItemListAdapter

import com.flysolo.cashregister.purchases.ItemPurchased
import com.flysolo.cashregister.navdrawer.home.viewmodels.PurchasingViewModel
import com.google.firebase.firestore.FirebaseFirestore


class ItemListFragment : Fragment(),ItemListAdapter.OnItemIsClick {

    private lateinit var binding : FragmentItemListBinding

    private var firebaseQueries : FirebaseQueries? = null
    private lateinit var itemPurchasedList: MutableList<ItemPurchased>
    private lateinit var purchasingViewModel: PurchasingViewModel
    private var itemQuantity = 1
    private lateinit var adapter : ItemListAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentItemListBinding.inflate(layoutInflater,container,false)

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        purchasingViewModel = ViewModelProvider(requireActivity()).get(PurchasingViewModel::class.java)
        firebaseQueries = FirebaseQueries(requireContext(), FirebaseFirestore.getInstance())
        itemPurchasedList = mutableListOf()
        adapter = ItemListAdapter(requireContext(), firebaseQueries!!.getAllItems(CashierLoginActivity.userID!!),this)
        binding.recyclerviewItems.adapter = adapter
        //button Actions
        binding.imageButtonIncrement.setOnClickListener {
            binding.textQuantity.text = incrementQuantity().toString()
        }
        binding.imageButtonDecrement.setOnClickListener {
            binding.textQuantity.text = decrementQuantity().toString()
        }
    }

    // TODO: Increment Quantity
    private fun incrementQuantity(): Int {
        itemQuantity += 1
        return itemQuantity
    }

    // TODO: Decrement Quantity
    private fun decrementQuantity(): Int {
        if (itemQuantity > 1) {
            itemQuantity -= 1
            return itemQuantity
        } else Toast.makeText(context, "minimum item is 1", Toast.LENGTH_SHORT).show()
        return itemQuantity
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun itemClick(position: Int) {
        if (itemQuantity <= adapter.getItem(position).itemQuantity!!){
            val item = adapter.getItem(position)
            purchasingViewModel.setItem(ItemPurchased(item.itemBarcode,
                item.itemName, itemQuantity, item.itemCost,itemQuantity * item.itemPrice!!,false))
            itemQuantity = 1
            binding.textQuantity.text = itemQuantity.toString()
        } else {
            Toast.makeText(requireContext(),"Not enough stocks for this item",Toast.LENGTH_SHORT).show()
        }
    }
}

