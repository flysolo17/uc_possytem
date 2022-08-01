package com.flysolo.cashregister.admin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.flysolo.cashregister.R
import com.flysolo.cashregister.adapter.CashDrawerAdapter
import com.flysolo.cashregister.databinding.ActivityAdminCashDrawerBinding
import com.flysolo.cashregister.databinding.ActivityCashDrawerBinding
import com.flysolo.cashregister.firebase.QueryDates
import com.flysolo.cashregister.firebase.models.CashDrawer
import com.flysolo.cashregister.firebase.models.Cashier
import com.flysolo.cashregister.firebase.models.User
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.squareup.picasso.Picasso
import java.text.Format
import java.text.SimpleDateFormat
import java.util.*

class AdminCashDrawer : AppCompatActivity() {
    private lateinit var binding : ActivityAdminCashDrawerBinding
    private lateinit var cashDrawerAdapter: CashDrawerAdapter
    private lateinit var cashDrawerList: MutableList<CashDrawer>
    private val firestore = FirebaseFirestore.getInstance()
    private val queryDates = QueryDates()
    private fun init() {
        binding.recyclerviewCashDrawer.layoutManager = LinearLayoutManager(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminCashDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        binding.buttonBack.setOnClickListener {
            finish()
        }
        val  uid = FirebaseAuth.getInstance().currentUser?.uid

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.clear()
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        getCashDrawerByDate(
            uid!!,today
        )
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
            getCashDrawerByDate(uid, date)
        }
        binding.buttonShowCalendar.setOnClickListener {
            datePicker.show(supportFragmentManager,"Date Picker")
        }
    }
    private fun getCashDrawerByDate(uid : String,date : Long) {
        cashDrawerList = mutableListOf()
        firestore.collection(User.TABLE_NAME)
            .document(uid)
            .collection(CashDrawer.TABLE_NAME)
            .whereGreaterThan(CashDrawer.TIMESTAMP,queryDates.startOfDay(date))
            .whereLessThan(CashDrawer.TIMESTAMP,queryDates.endOfDay(date))
            .orderBy(CashDrawer.TIMESTAMP,Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                cashDrawerList.clear()
                if (error != null) {
                    error.printStackTrace()
                } else {
                    value?.map { documents ->
                        val cashDrawer = documents.toObject(CashDrawer::class.java)
                        cashDrawerList.add(cashDrawer)
                    }
                    cashDrawerAdapter  = CashDrawerAdapter(this,cashDrawerList)
                    binding.recyclerviewCashDrawer.adapter = cashDrawerAdapter

                }
            }
    }
    private fun setCalendarFormat(timestamp: Long): String? {
        val date = Date(timestamp)
        val format: Format = SimpleDateFormat("MMM dd, yyyy")
        return format.format(date)
    }

}