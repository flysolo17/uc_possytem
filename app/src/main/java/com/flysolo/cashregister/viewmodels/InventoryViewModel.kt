package com.flysolo.cashregister.mystore.bottomnav.inventory.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.firebase.models.Transaction

class InventoryViewModel : ViewModel() {
    private val selected: MutableLiveData<Items> = MutableLiveData<Items>()

    fun setItem(items: Items) {
        selected.value= items
    }

    fun getItem(): LiveData<Items> {
        return selected
    }
}