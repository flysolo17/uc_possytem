package com.flysolo.cashregister.login

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.chaos.view.PinView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.flysolo.cashregister.MainActivity
import com.flysolo.cashregister.R
import com.flysolo.cashregister.adapter.AccountsAdapter
import com.flysolo.cashregister.admin.AdminActivity
import com.flysolo.cashregister.databinding.ActivityAccountsBinding
import com.flysolo.cashregister.firebase.models.Cashier
import com.flysolo.cashregister.firebase.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.squareup.picasso.Picasso


class Accounts : AppCompatActivity(), AccountsAdapter.CashierClickListener {
    private lateinit var binding : ActivityAccountsBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var accountsAdapter: AccountsAdapter
    private var storePin : String? = null
    private var uid : String? = null
    private fun init(userID : String) {
        firestore = FirebaseFirestore.getInstance()
        getUserInfo(userID)
        accountsAdapter = AccountsAdapter(this,getCashiers(userID),this)
        binding.recyclerviewCashier.apply {
            layoutManager = LinearLayoutManager(this@Accounts)
            adapter = accountsAdapter
            addItemDecoration(
                DividerItemDecoration(
                    binding.recyclerviewCashier.context,
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val userID  = intent.getStringExtra(User.USER_ID)
        init(userID!!)
        binding.buttonOwner.setOnClickListener {
            showAdminDialog(storePin!!)
        }
        binding.buttonLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            finish()
        }
    }
    private fun getUserInfo(userID: String) {
        firestore.collection(User.TABLE_NAME)
            .document(userID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    displayUserInfo(user)
                }
            }
    }

    private fun displayUserInfo(user: User?) {
        if (user != null) {
            uid = user.userId
            if (user.userProfile!!.isNotEmpty()) {
                Picasso.get().load(user.userProfile).into(binding.ownerProfile)
            }
            storePin = user.userStorePin
            binding.textStoreName.text = user.userBusinessName
            binding.textUserFullname.text = "${user.userFirstname} ${user.userLastname}"
        }
    }
    private fun getCashiers(userID: String): FirestoreRecyclerOptions<Cashier?> {
        val query: Query = firestore
            .collection(User.TABLE_NAME)
            .document(userID)
            .collection(Cashier.TABLE_NAME)

        return FirestoreRecyclerOptions.Builder<Cashier>()
            .setQuery(query, Cashier::class.java)
            .build()
    }

    override fun onCashierClick(pos: Int) {
        if (uid != null) {
            showDialog(accountsAdapter.snapshots[pos])
        }
    }
    private fun showDialog(cashier: Cashier) {
        val dialogs = Dialog(this)
        dialogs.setTitle("Cashier Login")
        dialogs.setContentView(R.layout.fragment_cashier_pin_dialog)
        val cashierName : TextView = dialogs.findViewById(R.id.textCashierName)
        val cashierProfile : ImageView = dialogs.findViewById(R.id.cashierProfile)
        val inputPinCode : EditText = dialogs.findViewById(R.id.inputPinCode)
        val buttonCancel : Button = dialogs.findViewById(R.id.buttonCancel)
        val buttonSave : Button = dialogs.findViewById(R.id.buttonSave)
        cashierName.text = cashier.cashierName
        if (cashier.cashierProfile!!.isNotEmpty()){
            Picasso.get().load(cashier.cashierProfile).into(cashierProfile)
        }
        dialogs.setTitle("Cashier Login")
        dialogs.setCancelable(false)
        dialogs.show()

        buttonCancel.setOnClickListener {

            dialogs.dismiss()

        }
        buttonSave.setOnClickListener {
            if (cashier.cashierPin.equals(inputPinCode.text.toString())) {
                val intent = Intent(binding.root.context,MainActivity::class.java)
                intent.putExtra(User.USER_ID,uid)
                intent.putExtra(Cashier.CASHIER_ID,cashier.cashierID)
                startActivity(intent)
                dialogs.dismiss()
            } else {
                inputPinCode.error = "Wrong Pin"
            }
        }

        binding.buttonLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            finish()
        }
    }


    override fun onStart() {
        super.onStart()
        accountsAdapter.startListening()
    }

    private fun showAdminDialog(storePin : String) {
        val view: View = layoutInflater.inflate(R.layout.dialog_admin_pincode, binding.root, false)
        val pinView: PinView = view.findViewById(R.id.storePin)
        val dialog = Dialog(this)
        dialog.setContentView(view)
        pinView.addTextChangedListener { text: Editable? ->
            if (text.toString().length == 6) {
                if (text.toString() == storePin) {
                    startActivity(Intent(this,AdminActivity::class.java))
                    dialog.dismiss()
                } else {
                    text?.clear()
                    Toast.makeText(
                        this@Accounts,
                        "Invalid Pin",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }

        dialog.show()

    }
}