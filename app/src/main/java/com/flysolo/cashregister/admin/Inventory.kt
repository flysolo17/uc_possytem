package com.flysolo.cashregister.admin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.flysolo.cashregister.adapter.InventoryAdapter
import com.flysolo.cashregister.databinding.ActivityInventoryBinding
import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.fragments.AddInventory
import com.flysolo.cashregister.fragments.UpdateInventory
import com.flysolo.cashregister.login.LoginActivity.Companion.uid
import com.flysolo.cashregister.mystore.bottomnav.inventory.viewmodel.InventoryViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Inventory : AppCompatActivity(),InventoryAdapter.ItemClick {
    private lateinit var binding : ActivityInventoryBinding
    private lateinit var inventoryAdapter: InventoryAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var inventoryViewModel: InventoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firestore = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        inventoryViewModel = ViewModelProvider(this)[InventoryViewModel::class.java]
        inventoryAdapter = InventoryAdapter(this,getAllItems(uid!!),0,this)
        binding.recyclerviewItems.apply {
            layoutManager = LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false)
            adapter = inventoryAdapter
            addItemDecoration(
                DividerItemDecoration(this@Inventory,
                DividerItemDecoration.VERTICAL)
            )
        }
        swipeToDelete(binding.recyclerviewItems)

        binding.buttonAddInventory.setOnClickListener {
            val addInventory = AddInventory()
            if (!addInventory.isAdded){
                addInventory.show(supportFragmentManager,"add inventory")
            }
        }
        binding.buttonBack.setOnClickListener {
            finish()
        }

    }

    override fun onResume() {
        super.onResume()
        inventoryAdapter.startListening()
    }
    private fun getAllItems(userID: String): FirestoreRecyclerOptions<Items?> {
        val query: Query = firestore
            .collection(User.TABLE_NAME)
            .document(userID)
            .collection(Items.TABLE_NAME)
        return FirestoreRecyclerOptions.Builder<Items>()
            .setQuery(query, Items::class.java)
            .build()
    }

    private fun swipeToDelete(recyclerView: RecyclerView?) {
        val callback = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback( 0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                deleteItem(inventoryAdapter.snapshots[position].itemBarcode!!)
            }
        })
        callback.attachToRecyclerView(recyclerView)
    }

    private fun deleteItem(id: String) {
        firestore.collection(User.TABLE_NAME)
            .document(uid)
            .collection(Items.TABLE_NAME)
            .document(id).delete().addOnCompleteListener { task: Task<Void?> ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Item deleted successfully..", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(this, "Failed to delete item", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onItemClick(position: Int) {
        showUpdateInventory(position)
    }
    private fun showUpdateInventory(position : Int) {
        inventoryViewModel.setItem(inventoryAdapter.snapshots[position])
        val updateInventory = UpdateInventory()
        if (!updateInventory.isAdded) {
            updateInventory.show(supportFragmentManager,"update item")
        }
    }
}