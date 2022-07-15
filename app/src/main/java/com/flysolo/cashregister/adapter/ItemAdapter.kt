package com.flysolo.cashregister.adapter

import android.content.Context
import android.view.LayoutInflater

import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView
import com.flysolo.cashregister.databinding.RowItemsBinding
import com.flysolo.cashregister.firebase.models.Items
import java.text.DecimalFormat

class ItemAdapter(val context: Context,val itemList : List<Items>, val onItemIsClick: OnItemIsClick)  :
    RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
    interface OnItemIsClick{
        fun itemClick(position: Int)
    }

    inner class ItemViewHolder(private val dataBinding: RowItemsBinding) : RecyclerView.ViewHolder(dataBinding.root) {
        private val decimalFormat = DecimalFormat("#,###.00")
        fun bindProduct(item: Items) {
            dataBinding.items = item
            dataBinding.textItemPrice.text = decimalFormat.format(item.itemPrice)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val dataBinding = RowItemsBinding.inflate(
            layoutInflater,
            parent,
            false
        )
        return ItemViewHolder(dataBinding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = itemList[position]
        holder.bindProduct(item)
        holder.itemView.setOnClickListener{
            onItemIsClick.itemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

}