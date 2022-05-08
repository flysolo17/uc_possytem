package com.flysolo.cashregister.navdrawer.home.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater

import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.flysolo.cashregister.databinding.RowItemsBinding
import com.flysolo.cashregister.firebase.models.Items
import com.squareup.picasso.Picasso

class ItemListAdapter(val context: Context, options: FirestoreRecyclerOptions<Items?>,val onItemIsClick: OnItemIsClick) :
    FirestoreRecyclerAdapter<Items, ItemListAdapter.ItemListViewModel>(options) {
    interface OnItemIsClick{
        fun itemClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemListViewModel {
        val layoutInflater = LayoutInflater.from(parent.context)
        val dataBinding = RowItemsBinding.inflate(
            layoutInflater,
            parent,
            false
        )
        return ItemListViewModel(dataBinding)
    }

    override fun onBindViewHolder(holder: ItemListViewModel, position: Int, model: Items) {
        val item = getItem(position)
        holder.bindProduct(item)
        holder.itemView.setOnClickListener{
            onItemIsClick.itemClick(position)
        }
    }
    inner class ItemListViewModel(private val dataBinding: RowItemsBinding) : RecyclerView.ViewHolder(dataBinding.root) {
        fun bindProduct(item: Items) {
            dataBinding.item = item
            if (item.itemImageURL == null) {
                dataBinding.itemPicture.setBackgroundColor(Color.BLACK)
            } else {
                Picasso.get()
                    .load(item.itemImageURL)
                    .into(dataBinding.itemPicture)
            }
        }
    }


}