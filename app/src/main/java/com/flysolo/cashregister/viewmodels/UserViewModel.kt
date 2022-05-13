package com.flysolo.cashregister.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.firebase.models.User


class UserViewModel : ViewModel() {
    val selected : MutableLiveData<User> = MutableLiveData<User>()

    fun setUser(user: User) {
        selected.value= user
    }

    fun getUser(): LiveData<User> {
        return selected
    }
}