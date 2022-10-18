package com.flysolo.cashregister.activities

import android.os.Bundle
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.flysolo.cashregister.adapter.InventoryAdapter

import com.flysolo.cashregister.databinding.ActivityCashierInventoryBinding
import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.firebase.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CashierInventory : AppCompatActivity(),InventoryAdapter.ItemClick {

    private lateinit var binding: ActivityCashierInventoryBinding
    private lateinit var inventoryAdapter: InventoryAdapter
    private lateinit var firestore : FirebaseFirestore
    private fun init(uid : String) {
        firestore = FirebaseFirestore.getInstance()
        inventoryAdapter = InventoryAdapter(this,getAllItems(uid),1,this)
        binding.recyclerviewItems.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL,false)
            adapter = inventoryAdapter
            addItemDecoration(
                DividerItemDecoration(this@CashierInventory,
                    DividerItemDecoration.VERTICAL)
            )
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCashierInventoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init(FirebaseAuth.getInstance().currentUser!!.uid)
        binding.buttonBack.setOnClickListener {
            finish()
        }

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

    override fun onItemClick(position: Int) {
        Toast.makeText(binding.root.context,"Unable to edit inventory",Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        inventoryAdapter.startListening()
    }

}