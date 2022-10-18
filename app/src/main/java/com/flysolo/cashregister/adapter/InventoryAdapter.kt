package com.flysolo.cashregister.adapter

import android.content.Context
import android.graphics.Color

import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.flysolo.cashregister.R



import com.flysolo.cashregister.firebase.models.Items

import com.squareup.picasso.Picasso
import java.text.DecimalFormat

class InventoryAdapter(val context: Context, options: FirestoreRecyclerOptions<Items?>,val userType : Int, private val itemClick: ItemClick) :
    FirestoreRecyclerAdapter<Items, InventoryAdapter.InventoryViewModel>(options) {

    interface ItemClick {
        fun onItemClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewModel {

        val view = LayoutInflater.from(context).inflate(R.layout.row_inventory,parent,false);
        return InventoryViewModel(view)
    }

    override fun onBindViewHolder(holder: InventoryViewModel, position: Int, model: Items) {
        val item = getItem(position)
        val decimalFormat = DecimalFormat("#,###.00")
        holder.itemName.text = item.itemName
        holder.itemCategory.text = item.itemCategory
        holder.itemCost.text = decimalFormat.format(item.itemCost)
        holder.itemPrice.text = decimalFormat.format(item.itemPrice)
        holder.itemQuantity.text = item.itemQuantity.toString()
        if (item.itemImageURL!!.isNotEmpty()){
            Picasso.get().load(item.itemImageURL).placeholder(R.drawable.store).into(holder.itemImage)
        }
        holder.itemView.setOnClickListener {
            itemClick.onItemClick(position)
        }
        if (userType == 0) {
            holder.layoutWholesale.visibility = View.VISIBLE
        } else {
            holder.layoutWholesale.visibility = View.INVISIBLE
        }
    }

    inner class InventoryViewModel(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemCategory: TextView = itemView.findViewById(R.id.itemCategory)
        val itemCost: TextView = itemView.findViewById(R.id.itemCost)
        val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
        val itemQuantity: TextView = itemView.findViewById(R.id.itemQuantity)
        val itemImage : ImageView = itemView.findViewById(R.id.itemImage)
        val layoutWholesale : LinearLayout = itemView.findViewById(R.id.layoutWholesale)
    }
}