package com.flysolo.cashregister.admin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.flysolo.cashregister.R
import com.flysolo.cashregister.databinding.ActivityAdminBinding
import com.flysolo.cashregister.firebase.models.Cashier
import com.flysolo.cashregister.firebase.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private var user : User? = null
    private lateinit var firestore: FirebaseFirestore
    private fun init(userID: String) {
        firestore = FirebaseFirestore.getInstance()
        getUser(userID)
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

}