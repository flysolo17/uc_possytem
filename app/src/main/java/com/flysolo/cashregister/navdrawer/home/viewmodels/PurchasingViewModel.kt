package com.flysolo.cashregister.navdrawer.home.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.purchases.ItemPurchased

class PurchasingViewModel : ViewModel() {
    private val selected: MutableLiveData<ItemPurchased> = MutableLiveData<ItemPurchased>()
    private val purchases: MutableLiveData<List<ItemPurchased>> by lazy {
        MutableLiveData<List<ItemPurchased>>()
    }

    fun getPurchases(): LiveData<List<ItemPurchased>> {
        return purchases
    }
    fun setPurchases(purchasedList : List<ItemPurchased>?) {
        purchases.value = purchasedList
    }



    fun setItem(itemPurchased: ItemPurchased) {
        selected.value = itemPurchased
    }

    fun getItemPurchased(): LiveData<ItemPurchased> {
        return selected
    }
}