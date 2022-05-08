package com.flysolo.cashregister.mystore.bottomnav.storehome.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.purchases.ItemPurchased

class TransactionViewModel : ViewModel() {
    private val selected: MutableLiveData<Transaction> = MutableLiveData<Transaction>()

    fun setTransaction(transaction: Transaction) {
        selected.value= transaction
    }

    fun getTransaction(): LiveData<Transaction> {
        return selected
    }



    private val transaction: MutableLiveData<List<Transaction>> by lazy {
        MutableLiveData<List<Transaction>>()
    }

    fun getTransactionList(): LiveData<List<Transaction>> {
        return transaction
    }
    fun setTransactionList(transactionList : List<Transaction>?) {
        transaction.value = transactionList
    }

}