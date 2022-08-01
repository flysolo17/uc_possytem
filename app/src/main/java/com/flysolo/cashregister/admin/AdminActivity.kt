package com.flysolo.cashregister.admin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.flysolo.cashregister.databinding.ActivityAdminBinding
import com.flysolo.cashregister.firebase.QueryDates

import com.flysolo.cashregister.firebase.models.User
import com.google.android.material.datepicker.MaterialDatePicker

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.text.DateFormat
import java.text.DecimalFormat

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private var user : User? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var transactionList : MutableList<com.flysolo.cashregister.firebase.models.Transaction>
    private lateinit var queryDates : QueryDates
    private val decimalFormat = DecimalFormat("0.00")
    private fun init(userID: String) {
        firestore = FirebaseFirestore.getInstance()
        getUser(userID)
        queryDates = QueryDates()
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        getAllTransactionsToday(today,userID)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val userID = FirebaseAuth.getInstance().currentUser!!.uid
        init(userID)
        binding.buttonCashier.setOnClickListener {
            startActivity(Intent(this,CashierActivity::class.java))
        }
        binding.buttonInventoty.setOnClickListener {
            startActivity(Intent(this,Inventory::class.java))
        }
        binding.buttonTransactions.setOnClickListener {
            startActivity(Intent(this,Transaction::class.java))
        }
        binding.buttonUpdateAccount.setOnClickListener {
            if (user != null) {
                startActivity(Intent(this,UpdateAccount::class.java).putExtra(User.TABLE_NAME,user))
            }
        }
        binding.buttonCashDrawer.setOnClickListener {
            startActivity(Intent(this,AdminCashDrawer::class.java))
        }
        binding.buttonEmployeAttendance.setOnClickListener {
            startActivity(Intent(this,EmployeeAttendance::class.java))
        }
    }
    private fun getUser(userID : String) {
        firestore.collection(User.TABLE_NAME)
            .document(userID)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    error.printStackTrace()
                } else {
                    if (value != null) {
                        if (value.exists()) {
                            val user = value.toObject(User::class.java)
                            if (user != null) {
                                this.user = user
                                displayUserInfo(user)
                            }
                        }
                    }
                }
            }
    }
    private fun displayUserInfo(user: User) {
        if (user.userProfile!!.isNotEmpty()){
            Picasso.get().load(user.userProfile).into(binding.userProfile)
        }
        binding.textUserFullname.text = "${user.userFirstname} ${user.userLastname}"
        binding.textBusinessName.text = user.userBusinessName
    }
    private fun getAllTransactionsToday(date : Long,userID: String) {
        transactionList = mutableListOf()
        val query = firestore.collection(User.TABLE_NAME)
            .document(userID)
            .collection(com.flysolo.cashregister.firebase.models.Transaction.TABLE_NAME)
            .whereGreaterThan(
                com.flysolo.cashregister.firebase.models.Transaction.TIMESTAMP,
                queryDates.startOfDay(date)
            )
            .whereLessThan(
                com.flysolo.cashregister.firebase.models.Transaction.TIMESTAMP,
                queryDates.endOfDay(date)
            )
        query.addSnapshotListener { value, error ->
            transactionList.clear()
            if (error != null) {
                error.printStackTrace()
            } else {
                value?.map { documents ->
                    val transaction = documents.toObject(com.flysolo.cashregister.firebase.models.Transaction::class.java)
                    transactionList.add(transaction)
                }
                binding.textSalesToday.text = computeTotalSales(transactionList)
            }
        }
    }

    private fun computeTotalSales(transactions: List<com.flysolo.cashregister.firebase.models.Transaction>): String {
        var total = 0.0
        for (items in transactions) {
            for (totalSales in items.transactionItems!!)
                if (totalSales.itemPurchasedIsRefunded != true) {
                    total += totalSales.itemPurchasedPrice!!
                }
        }
        return decimalFormat.format(total)
    }


}