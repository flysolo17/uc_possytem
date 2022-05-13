package com.flysolo.cashregister

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.chaos.view.PinView
import com.flysolo.cashregister.activities.*

import com.flysolo.cashregister.databinding.ActivityMainBinding
import com.flysolo.cashregister.firebase.models.Cashier

import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.login.LoginActivity

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.lang.NumberFormatException

const val defaultCashier = "No cashier"
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firestore: FirebaseFirestore
    private var user : User? = null
    private var cashierList = mutableListOf<Cashier>()
    private var cashierNames = mutableListOf(defaultCashier)
    private var activity : Activity? = null
    private lateinit var adminDialog : Dialog
    private lateinit var pinLayout : View
    private lateinit var storePinView: PinView
    private var store : String? = null
    private fun initViews(uid: String) {
        firestore = FirebaseFirestore.getInstance()
        fetchCurrentUser(uid)
        getALlCashier(uid)
        val cashierAdapter = ArrayAdapter(this,android.R.layout.simple_list_item_1,cashierNames)
        binding.cashierSpinner.apply {
            adapter = cashierAdapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    if (p2 != 0) {
                        showDialog(cashierList[p2-1],p2)
                    }

                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    Toast.makeText(this@MainActivity,"Please Select cashier",Toast.LENGTH_SHORT).show()
                }
            }

        }
        adminDialog = Dialog(this)
        pinLayout= layoutInflater.inflate(R.layout.dialog_admin_pincode,null,false)
        storePinView = pinLayout.findViewById(R.id.storePin)
        storePinView.addTextChangedListener(storePinWatcher)
    }

    private fun showDialog(cashier: Cashier,position : Int) {
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
            binding.cashierSpinner.setSelection(0)
            dialogs.dismiss()

        }
        buttonSave.setOnClickListener {
            if (cashier.cashierPin.equals(inputPinCode.text.toString())) {
                binding.cashierSpinner.setSelection(position)
                dialogs.dismiss()
            } else {
                inputPinCode.error = "Wrong Pin"
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val uid = intent.getStringExtra(User.USER_ID)
        initViews(uid!!)

        binding.buttonInventory.setOnClickListener {
            startActivity(Intent(this,Inventory::class.java).putExtra(User.USER_ID,uid))
        }
        binding.buttonLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this,"Successfully Log out!",Toast.LENGTH_SHORT).show()
            startActivity(Intent(this,LoginActivity::class.java))
        }
        binding.buttonProfile.setOnClickListener {
            if (user != null) {
                startActivity(Intent(this,UpdateAccount::class.java).putExtra(User.TABLE_NAME,user).putExtra(User.USER_ID,uid))
            }
        }
        binding.buttonPOS.setOnClickListener {
            posActivity(binding.cashierSpinner.selectedItem.toString(),uid)
        }

        binding.buttonTransaction.setOnClickListener {
            activity = Transaction()

            if (!adminDialog.isShowing) {
                adminDialog.setContentView(pinLayout)
                adminDialog.show()
            }
        }
        binding.buttonCashDrawer.setOnClickListener {
            activity = CashDrawerActivity()
            if (!adminDialog.isShowing) {
                adminDialog.setContentView(pinLayout)
                adminDialog.show()
            }
        }
    }

    private fun posActivity(cashierName: String,uid: String) {
        if (cashierName.isEmpty() || cashierName == defaultCashier){
            Toast.makeText(this,"No cashier",Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(this, Pos::class.java).putExtra(Cashier.CASHIER_NAME,cashierName).putExtra(User.USER_ID,uid).putExtra(User.BUSINESS_NAME,store))
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
                        this.user = user
                        store = user?.userBusinessName
                        if (user?.userProfile!!.isNotEmpty()){
                            Picasso.get().load(user.userProfile).placeholder(R.drawable.store).into(binding.imageAddStoreImage)
                        }
                        binding.textBusinessName.text = user.userBusinessName
                        binding.textOwnerName.text = user.userFirstname + " " + user.userLastname
                    }
                }
        }
    }

    private fun getALlCashier(uid: String){
        firestore.collection(User.TABLE_NAME)
            .document(uid)
            .collection(Cashier.TABLE_NAME)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val cashier = document.toObject(Cashier::class.java)
                    cashierList.add(cashier)
                    cashierNames.add(cashier.cashierName!!)
                }
            }
            .addOnFailureListener { exception ->
                Log.w("AddRecipe", "Error getting documents: ", exception)
            }
    }

    override fun onResume() {
        super.onResume()
        window?.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    private val storePinWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            try {
                if (s.toString().length == 6) {
                    if (s.toString() == user!!.userStorePin ){
                        if (activity != null) {
                            storePinView.editableText.clear()
                            adminDialog.dismiss()
                            adminActivities(activity!!)
                        }
                    } else {
                        storePinView.editableText.clear()
                        Toast.makeText(
                            this@MainActivity,
                            "Invalid Pin",
                            Toast.LENGTH_SHORT
                        ).show()

                    }

                }
            } catch (ignored: NumberFormatException) {
            }
        }
    }
    private fun adminActivities(activity: Activity) {
        startActivity(Intent(this@MainActivity, activity::class.java))
    }



    companion object {
        fun setTranslucentStatusBar(activity : Activity) {
            activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            activity.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            activity.window?.statusBarColor = Color.TRANSPARENT
        }
    }
}