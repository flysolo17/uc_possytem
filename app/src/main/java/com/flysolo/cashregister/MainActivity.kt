package com.flysolo.cashregister

import android.app.Activity

import android.content.Intent
import android.graphics.Color

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.view.WindowManager
import android.widget.Toast

import com.flysolo.cashregister.activities.*
import com.flysolo.cashregister.admin.Inventory

import com.flysolo.cashregister.databinding.ActivityMainBinding
import com.flysolo.cashregister.firebase.QueryDates
import com.flysolo.cashregister.firebase.models.Cashier
import com.flysolo.cashregister.firebase.models.Transaction

import com.flysolo.cashregister.firebase.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.text.DecimalFormat



class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firestore: FirebaseFirestore

    private val decimalFormat = DecimalFormat("0.00")
    private var uid : String ? = null
    private var cashierID : String ? = null
    private var cashierName : String ? = null
    private var store : String ? = null
    private val queryDates = QueryDates()
    private lateinit var transactionList : MutableList<Transaction>
    private fun initViews(uid: String) {
        firestore = FirebaseFirestore.getInstance()
        fetchCurrentUser(uid)

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cashierID = intent.getStringExtra(Cashier.CASHIER_ID)
        uid = intent.getStringExtra(User.USER_ID)
        initViews(uid!!)
        getCashier(uid!!, cashierID!!)

        binding.buttonInventory.setOnClickListener {
            startActivity(Intent(this, CashierInventory::class.java))
        }
        
        binding.buttonPOS.setOnClickListener {
            posActivity(cashierName!!, uid!!)
        }
        binding.buttonCashDrawer.setOnClickListener {
            startActivity(Intent(this,CashDrawerActivity::class.java).putExtra(Cashier.CASHIER_ID,cashierID))
        }

        //attendance
        binding.buttonAttendance.setOnClickListener {
            startActivity(Intent(this,AttendanceActivity::class.java).putExtra(User.USER_ID,uid))
        }


    }
    private fun getCashierTotalSales(transactionList : List<Transaction>,cashierName: String) : String {
        var sales : Double = 0.toDouble()

        for(trans in transactionList) {
            if (trans.transactionCashier.equals(cashierName)){
                for (total in trans.transactionItems!!) {
                    if (total.itemPurchasedIsRefunded == false) {
                        sales += total.itemPurchasedPrice!!
                    }
                }
            }
        }
        return decimalFormat.format(sales)
    }
    private fun getCashierCostOfGoods(transactionList : List<Transaction>,cashierName: String) : String {
        var sales : Double = 0.toDouble()
        for(trans in transactionList) {
            if (trans.transactionCashier.equals(cashierName)){
                trans.transactionItems?.map { transition ->
                    sales += transition.itemPurchasedCost!!
                }
            }
        }
        return decimalFormat.format(sales)
    }
    private fun getTransactionToday(uid: String,cashierName: String){
        transactionList = mutableListOf()
        firestore.collection(User.TABLE_NAME)
            .document(uid)
            .collection(Transaction.TABLE_NAME)
            .whereGreaterThan(Transaction.TIMESTAMP,queryDates.startOfDay(System.currentTimeMillis()))
            .whereLessThan(Transaction.TIMESTAMP,queryDates.endOfDay(System.currentTimeMillis()))
            .addSnapshotListener { value, error ->
                transactionList.clear()
                if (error != null) {
                    error.printStackTrace()
                } else {
                    value?.map { documents ->
                        val transaction = documents.toObject(Transaction::class.java)
                        transactionList.add(transaction)
                    }
                    binding.textCashierSales.text = getCashierTotalSales(transactionList,cashierName)
                    binding.textCostOfGoods.text = getCashierCostOfGoods(transactionList, cashierName)
                }
            }
    }


    private fun fetchCurrentUser(uid : String){
        if (uid.isNotEmpty()) {
            firestore.collection(User.TABLE_NAME)
                .document(uid)
                .get()
                .addOnSuccessListener {
                    if (it.exists()){
                        val user = it.toObject(User::class.java)
                        MainActivity.user = user
                        if (user != null) {
                            this.store = user.userBusinessName
                            binding.textBusinessName.text = user.userBusinessName
                        }
                    }
                }
        }
    }
    private fun getCashier(uid: String,cashierID : String) {
        firestore.collection(User.TABLE_NAME)
            .document(uid)
            .collection(Cashier.TABLE_NAME)
            .document(cashierID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val cashier = document.toObject(Cashier::class.java)
                    if (cashier != null) {
                        this.cashierName = cashier.cashierName
                        displayCashierInfo(cashier)
                        getTransactionToday(uid, cashier.cashierName!!)
                    }
                }
            }
    }

    private fun displayCashierInfo(cashier: Cashier?) {
        if (cashier != null) {
            if (cashier.cashierProfile!!.isNotEmpty()){
                Picasso.get().load(cashier.cashierProfile).into(binding.cashierProfile)
            }
            binding.textCashierName.text = cashier.cashierName
        }
    }


    override fun onResume() {
        super.onResume()
        window?.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

    }
    private fun posActivity(cashierName: String,uid: String) {
        if (cashierName.isEmpty() || cashierName == defaultCashier){
            Toast.makeText(this,"No cashier", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(this, Pos::class.java).putExtra(Cashier.CASHIER_NAME,cashierName).putExtra(User.USER_ID,uid).putExtra(User.BUSINESS_NAME,store))
        }
    }



    companion object {
        var user : User? = null
        fun setTranslucentStatusBar(activity : Activity) {
            activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            activity.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            activity.window?.statusBarColor = Color.TRANSPARENT
        }
    }
}