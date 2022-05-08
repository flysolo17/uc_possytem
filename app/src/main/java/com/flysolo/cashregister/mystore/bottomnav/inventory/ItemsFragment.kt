package com.flysolo.cashregister.mystore.bottomnav.inventory

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flysolo.cashregister.R
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.databinding.FragmentItemsBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.mystore.bottomnav.inventory.viewmodel.InventoryViewModel
import com.flysolo.cashregister.navdrawer.home.adapter.ItemListAdapter
import com.flysolo.cashregister.navdrawer.home.viewmodels.ItemViewModel
import com.flysolo.cashregister.navdrawer.inventory.adapter.InventoryAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class ItemsFragment : Fragment(),ItemListAdapter.OnItemIsClick{
    private lateinit var binding : FragmentItemsBinding
    private lateinit var firebaseQueries: FirebaseQueries
    private lateinit var itemAdapter: ItemListAdapter
    private lateinit var inventoryViewModel : InventoryViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentItemsBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inventoryViewModel = ViewModelProvider(requireActivity())[InventoryViewModel::class.java]
        firebaseQueries = FirebaseQueries(requireContext(), FirebaseFirestore.getInstance())
        itemAdapter = ItemListAdapter(requireContext(),firebaseQueries.getAllItems(FirebaseAuth.getInstance().currentUser!!.uid),this)
        binding.recyclerviewInventory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = itemAdapter
        }
        swipeToDelete(binding.recyclerviewInventory)

        binding.imageButtonAddItem.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_nav_inventory_to_addInventory)
        }
    }

    override fun onStart() {
        super.onStart()
        itemAdapter.startListening()
    }
    private fun swipeToDelete(recyclerView: RecyclerView?) {
        val callback = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback( 0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                firebaseQueries.deleteItem(itemAdapter.snapshots[position].itemBarcode!!)

            }
        })
        callback.attachToRecyclerView(recyclerView)
    }

    override fun itemClick(position: Int) {
        inventoryViewModel.setItem(itemAdapter.getItem(position))
        Navigation.findNavController(requireView()).navigate(R.id.action_nav_inventory_to_updateInventory2)
    }


}