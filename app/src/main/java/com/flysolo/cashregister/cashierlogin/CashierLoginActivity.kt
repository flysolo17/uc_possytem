package com.flysolo.cashregister.cashierlogin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.flysolo.cashregister.cashierlogin.adapter.CashierAdapter
import com.flysolo.cashregister.databinding.ActivityCashierLoginBinding
import com.flysolo.cashregister.dialogs.CashierPinDialog
import com.flysolo.cashregister.dialogs.StorePinCodeDialog
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.models.User
import com.google.firebase.firestore.FirebaseFirestore

class CashierLoginActivity : AppCompatActivity(), CashierAdapter.CashierClickListener{
    private lateinit var binding : ActivityCashierLoginBinding
    private lateinit var firestore : FirebaseFirestore
    private lateinit var cashierAdapter: CashierAdapter
    private lateinit var firebaseQueries: FirebaseQueries
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCashierLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firestore = FirebaseFirestore.getInstance()
        userID = intent.getStringExtra(User.USER_ID)
        getUserInfo(userID!!)
        firebaseQueries= FirebaseQueries(this, firestore)
        cashierAdapter = CashierAdapter(this,firebaseQueries.getCashiers(userID!!),this)
        binding.buttonGoToStore.setOnClickListener {
            val storePinCodeDialog = StorePinCodeDialog()
            storePinCodeDialog.show(supportFragmentManager,"My Store")

        }
        binding.recyclerviewCashiers.setLayoutManager(
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        binding.recyclerviewCashiers.adapter = cashierAdapter

    }
    companion object {
        var userID : String? = null
        var storeOwnerName : String? = null
        var storeName : String? = null
        var storePinCode : String? = null
        var cashierName : String? = null
        var user : User? = null
    }
    private fun getUserInfo(userID : String){
        firestore.collection(User.TABLE_NAME).document(userID)
            .get().addOnSuccessListener { document ->
                if (document != null){
                    val users = document.toObject(User::class.java)
                    storeOwnerName = users?.userFirstname + " " + users?.userLastname
                    storeName = users?.userBusinessName
                    storePinCode = users?.userStorePin
                    binding.textStoreName.text = storeName
                    user = users
                }
            }
    }

    override fun onCashierClick(pos: Int) {
        val cashierPinDialog = CashierPinDialog.newInstance(cashierAdapter.snapshots[pos].cashierID!!,cashierAdapter.snapshots[pos].cashierPin!!,
            cashierAdapter.snapshots[pos].cashierName!!)
        cashierPinDialog.show(supportFragmentManager,"Pos System")
    }

    override fun onStart() {
        super.onStart()
        cashierAdapter.startListening()
    }

    override fun onResume() {
        super.onResume()
        cashierAdapter.startListening()
    }
}