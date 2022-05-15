package com.flysolo.cashregister.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.flysolo.cashregister.databinding.RowCashierBinding
import com.flysolo.cashregister.firebase.models.Cashier
import com.squareup.picasso.Picasso

class CashierAdapter(
    val context: Context,
    options: FirestoreRecyclerOptions<Cashier?>,
    private val cashierClickListener: CashierClickListener
) :
    FirestoreRecyclerAdapter<Cashier, CashierAdapter.CashierViewHolder>(options) {
    interface CashierClickListener {
        fun onCashierClick(pos: Int)
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CashierViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val dataBinding = RowCashierBinding.inflate(
            layoutInflater,
            parent,
            false
        )
        return CashierViewHolder(dataBinding)
    }

    override fun onBindViewHolder(holder: CashierViewHolder, position: Int, model: Cashier) {
        val item = getItem(position)
        holder.bindProduct(item)

    }
    inner class CashierViewHolder(
        private val rowCashierBinding: RowCashierBinding
    ) : RecyclerView.ViewHolder(rowCashierBinding.root) {
        fun bindProduct(cashier: Cashier) {
            rowCashierBinding.cashier = cashier
            Picasso.get().load(cashier.cashierProfile).into(rowCashierBinding.cashierProfile)
            rowCashierBinding.cashierProfile.setOnClickListener {
                cashierClickListener.onCashierClick(bindingAdapterPosition)
            }
        }

    }

}
