package com.flysolo.cashregister.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flysolo.cashregister.databinding.RowItemPurchaseBinding
import com.flysolo.cashregister.firebase.models.ItemPurchased
import java.text.DecimalFormat

class ItemPurchasedAdapter(val context: Context, private val itemPurchasedList: List<ItemPurchased>) :
    RecyclerView.Adapter<ItemPurchasedAdapter.ItemPurchasedViewHolder>() {
    inner class ItemPurchasedViewHolder(private val dataBinding: RowItemPurchaseBinding) : RecyclerView.ViewHolder(dataBinding.root) {
        private val decimalFormat = DecimalFormat("#,###.00")
        fun bindProduct(itemPurchased: ItemPurchased) {
            dataBinding.itemPurchased = itemPurchased
            dataBinding.itemPurchasedTotal.text = decimalFormat.format(itemPurchased.itemPurchasedPrice)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemPurchasedViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val dataBinding = RowItemPurchaseBinding.inflate(
            layoutInflater,
            parent,
            false
        )
        return ItemPurchasedViewHolder(dataBinding)
    }

    override fun onBindViewHolder(holder: ItemPurchasedViewHolder, position: Int) {

        val itemPurchased = itemPurchasedList[position]
        holder.bindProduct(itemPurchased)

    }

    override fun getItemCount(): Int {
        return itemPurchasedList.size
    }

}