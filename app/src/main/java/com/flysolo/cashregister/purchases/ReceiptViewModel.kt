package com.flysolo.cashregister.purchases

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.flysolo.cashregister.firebase.models.Transaction

class ReceiptViewModel : ViewModel() {
    private val selected: MutableLiveData<Transaction> = MutableLiveData<Transaction>()

    fun setReceipt(transaction: Transaction) {
        selected.value= transaction
    }

    fun getReceipt(): LiveData<Transaction> {
        return selected
    }
}