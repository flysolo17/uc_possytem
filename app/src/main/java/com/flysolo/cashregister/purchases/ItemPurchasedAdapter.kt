package com.flysolo.cashregister.purchases

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flysolo.cashregister.databinding.RowItemPurchasedBinding

class ItemPurchasedAdapter(val context: Context, private val itemPurchasedList : List<ItemPurchased>) :
    RecyclerView.Adapter<ItemPurchasedAdapter.ItemPurchasedViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemPurchasedViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val dataBinding = RowItemPurchasedBinding.inflate(
            layoutInflater,
            parent,
            false
        )
        return ItemPurchasedViewHolder(dataBinding)
    }
    override fun onBindViewHolder(holder: ItemPurchasedViewHolder, position: Int) {
        val item = itemPurchasedList[position]
        holder.bindItemPurchased(item)
    }

    override fun getItemCount(): Int {
        return itemPurchasedList.size
    }
    inner class ItemPurchasedViewHolder(
        private val dataBinding: RowItemPurchasedBinding
    ) : RecyclerView.ViewHolder(dataBinding.root) {
        fun bindItemPurchased(itemPurchased: ItemPurchased) {
            dataBinding.itemPurchased = itemPurchased
        }
    }
}