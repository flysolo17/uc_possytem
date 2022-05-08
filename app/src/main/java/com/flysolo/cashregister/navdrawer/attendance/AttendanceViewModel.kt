package com.flysolo.cashregister.navdrawer.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.flysolo.cashregister.firebase.models.Attendance
import com.flysolo.cashregister.purchases.ItemPurchased

class AttendanceViewModel : ViewModel() {
    private val selected: MutableLiveData<Attendance> = MutableLiveData<Attendance>()

    fun setAttendance(attendance: Attendance) {
        selected.value = attendance
    }

    fun getItemPurchased(): LiveData<Attendance> {
        return selected
    }
}