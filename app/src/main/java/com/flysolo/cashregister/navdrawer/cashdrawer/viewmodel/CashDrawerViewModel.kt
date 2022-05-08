package com.flysolo.cashregister.navdrawer.cashdrawer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.flysolo.cashregister.firebase.models.CashDrawer
import com.flysolo.cashregister.firebase.models.Transaction

class CashDrawerViewModel : ViewModel()  {
    private val selected: MutableLiveData<CashDrawer> = MutableLiveData<CashDrawer>()

    fun setCashDrawer(cashDrawer: CashDrawer) {
        selected.value= cashDrawer
    }

    fun getCashDrawer(): LiveData<CashDrawer> {
        return selected
    }
}