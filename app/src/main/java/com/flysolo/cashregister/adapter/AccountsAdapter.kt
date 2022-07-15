package com.flysolo.cashregister.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.flysolo.cashregister.databinding.RowAccountsBinding
import com.flysolo.cashregister.databinding.RowCashierBinding
import com.flysolo.cashregister.firebase.models.Cashier
import com.squareup.picasso.Picasso

class AccountsAdapter(
    val context: Context,
    options: FirestoreRecyclerOptions<Cashier?>,
    private val cashierClickListener: CashierClickListener
) :
    FirestoreRecyclerAdapter<Cashier, AccountsAdapter.AccountsViewHolder>(options) {
    interface CashierClickListener {
        fun onCashierClick(pos: Int)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val dataBinding = RowAccountsBinding.inflate(
            layoutInflater,
            parent,
            false
        )
        return AccountsViewHolder(dataBinding)
    }

    override fun onBindViewHolder(holder: AccountsViewHolder, position: Int, model: Cashier) {
        val item = getItem(position)
        holder.bindProduct(item)
    }

    inner class AccountsViewHolder(
        private val rowAccountsBinding: RowAccountsBinding
    ) : RecyclerView.ViewHolder(rowAccountsBinding.root) {
        fun bindProduct(cashier: Cashier) {
            rowAccountsBinding.cashier = cashier
            if (cashier.cashierProfile!!.isNotEmpty()) {
                Picasso.get().load(cashier.cashierProfile).into(rowAccountsBinding.cashierProfile)
            }
            rowAccountsBinding.root.setOnClickListener {
                cashierClickListener.onCashierClick(bindingAdapterPosition)
            }
        }

    }
}
