package com.flysolo.cashregister.navdrawer.inventory

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

import com.flysolo.cashregister.R
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.databinding.FragmentInventoryBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.navdrawer.home.adapter.ItemListAdapter
import com.flysolo.cashregister.navdrawer.inventory.adapter.InventoryAdapter
import com.google.firebase.firestore.FirebaseFirestore


class InventoryFragment : Fragment(){
    private lateinit var binding : FragmentInventoryBinding
    private lateinit var firebaseQueries: FirebaseQueries
    private lateinit var adapter : InventoryAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentInventoryBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseQueries = FirebaseQueries(requireContext(), FirebaseFirestore.getInstance())
        adapter = InventoryAdapter(requireContext(),firebaseQueries.getAllItems(CashierLoginActivity.userID!!))
        binding.recyclerviewItems.adapter = adapter
        swipeToDelete(binding.recyclerviewItems)
    }

    private fun swipeToDelete(recyclerView: RecyclerView?) {
        val callback = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback( 0,ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                firebaseQueries.deleteItem(adapter.snapshots[position].itemBarcode!!)

            }
        })
        callback.attachToRecyclerView(recyclerView)
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }



}