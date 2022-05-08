package com.flysolo.cashregister.navdrawer.inventory.adapter

import android.content.Context
import android.graphics.Color
import android.opengl.Visibility
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.databinding.RowInventoryItemBinding

import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.firebase.models.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class InventoryAdapter(val context: Context, options: FirestoreRecyclerOptions<Items?>) :
    FirestoreRecyclerAdapter<Items, InventoryAdapter.InventoryViewModel>(options) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewModel {
        val layoutInflater = LayoutInflater.from(parent.context)
        val dataBinding = RowInventoryItemBinding.inflate(
            layoutInflater,
            parent,
            false
        )
        return InventoryViewModel(dataBinding)
    }

    override fun onBindViewHolder(holder: InventoryViewModel, position: Int, model: Items) {
        val item = getItem(position)
        holder.bindProduct(item)


    }
    inner class InventoryViewModel(private val dataBinding: RowInventoryItemBinding) : RecyclerView.ViewHolder(dataBinding.root) {
        fun bindProduct(item: Items) {
            dataBinding.item = item

            if (item.itemImageURL == null) {
                dataBinding.itemPicture.setBackgroundColor(Color.BLACK)
            } else {
                Picasso.get()
                    .load(item.itemImageURL)
                    .into(dataBinding.itemPicture)
            }
            dataBinding.root.setOnClickListener {
                TransitionManager.beginDelayedTransition(dataBinding.layoutAddInventory, AutoTransition())
                dataBinding.layoutAddInventory.visibility =  if (dataBinding.layoutAddInventory.visibility === View.GONE) View.VISIBLE else View.GONE
            }
            dataBinding.buttonAddInventory.setOnClickListener {
                val quantity = dataBinding.inputQuantity.editText?.text.toString()
                if (quantity.isEmpty()){
                    dataBinding.inputQuantity.error = "quantity is missing"
                } else {
                    addQuantity(item.itemBarcode!!,quantity.toLong())
                    dataBinding.inputQuantity.editText?.setText("")
                    dataBinding.layoutAddInventory.visibility = View.GONE
                }
            }
        }
    }
    fun addQuantity(itemID : String , itemQuantity : Long){
        FirebaseFirestore.getInstance().collection(User.TABLE_NAME)
            .document(CashierLoginActivity.userID!!)
            .collection(Items.TABLE_NAME)
            .document(itemID).update(Items.ITEM_QUANTITY, FieldValue.increment(itemQuantity))
            .addOnCompleteListener{
                if (it.isSuccessful){
                    Toast.makeText(context,"Item Quantity Updated!",Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context,"Fail to update quantity!",Toast.LENGTH_SHORT).show()
                }
            }
    }


}