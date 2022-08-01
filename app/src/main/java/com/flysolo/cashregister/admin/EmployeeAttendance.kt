package com.flysolo.cashregister.admin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.flysolo.cashregister.R
import com.flysolo.cashregister.adapter.AttendanceAdapter
import com.flysolo.cashregister.databinding.ActivityEmployeeAttendanceBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.QueryDates
import com.flysolo.cashregister.firebase.models.User
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*

class EmployeeAttendance : AppCompatActivity() , AttendanceAdapter.SelfieOutIsClick {
    private lateinit var binding : ActivityEmployeeAttendanceBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var attendanceAdapter : AttendanceAdapter
    private lateinit var firebaseQueries: FirebaseQueries
    private val queryDates = QueryDates()
    private fun  init() {
        firestore = FirebaseFirestore.getInstance()
        firebaseQueries = FirebaseQueries(this,firestore)

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeeAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()

        binding.buttonBack.setOnClickListener {
            finish()
        }

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.clear()
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        displayAttendance(today)
        binding.buttonShowCalendar.text = setCalendarFormat(today)
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .build()
        datePicker.addOnCancelListener {
            datePicker.dismiss()
        }
        datePicker.addOnDismissListener {
            datePicker.dismiss()
        }
        datePicker.addOnPositiveButtonClickListener { date ->
            binding.buttonShowCalendar.text = setCalendarFormat(date)
            displayAttendance(date)
        }
        binding.buttonShowCalendar.setOnClickListener {
           datePicker.show(supportFragmentManager,"Date Picker")
        }
    }


    private fun displayAttendance(date : Long) {
        attendanceAdapter = AttendanceAdapter(this, firebaseQueries.getAttendance(
            queryDates.startOfDay(date),
            queryDates.endOfDay(date)), this)
        binding.recyclerviewAttendance.apply {
            layoutManager = LinearLayoutManager(this@EmployeeAttendance)
            adapter = attendanceAdapter
        }
        attendanceAdapter.startListening()
    }
    override fun onStart() {
        super.onStart()
        attendanceAdapter.startListening()
    }
    private fun setCalendarFormat(timestamp: Long): String? {
        val date = Date(timestamp)
        val format: Format = SimpleDateFormat("MMM dd, yyyy")
        return format.format(date)
    }

    override fun selfieOutClick(position: Int) {
        Log.d(TAG,"owner don't have to take attendance")
    }
    companion object {
        const val TAG = ".EmployeeAttendance"
    }
}