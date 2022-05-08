package com.flysolo.cashregister.navdrawer.home.viewmodels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.flysolo.cashregister.purchases.ItemPurchased

class ItemViewModel : ViewModel() {
        private val itemPurchasedList: MutableLiveData<List<ItemPurchased>> by lazy {
            MutableLiveData<List<ItemPurchased>>().also {
                getItemPurchasedList()
            }
        }

         fun getItemPurchasedList(): LiveData<List<ItemPurchased>> {
            return itemPurchasedList
        }

         fun addPurchasedList(itemPurchased: List<ItemPurchased>) {
            itemPurchasedList.value = itemPurchased
    }
}