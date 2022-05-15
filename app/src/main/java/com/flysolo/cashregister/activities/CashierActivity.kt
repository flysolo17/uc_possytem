package com.flysolo.cashregister.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.flysolo.cashregister.R
import com.flysolo.cashregister.adapter.CashierAdapter
import com.flysolo.cashregister.databinding.ActivityCashierBinding
import com.flysolo.cashregister.dialog.ProgressDialog
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.models.Cashier
import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.login.LoginActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import java.io.IOException


class CashierActivity : AppCompatActivity(),CashierAdapter.CashierClickListener {
    private var uid : String? =null
    private lateinit var binding : ActivityCashierBinding
    private lateinit var dialog : BottomSheetDialog
    private lateinit var firestore : FirebaseFirestore
    private var galleryLauncher: ActivityResultLauncher<Intent>? = null
    private var cashierURI : Uri? = null
    private lateinit var cashierImage : ImageView
    private lateinit var inputCashierName : EditText
    private lateinit var inputCashierPin : EditText
    private lateinit var buttonGotoGallery : ImageButton
    private lateinit var buttonSaveEmployee : Button
    private lateinit var firebaseQueries: FirebaseQueries

    private var storage: StorageReference? = null
    private var mUploadTask: StorageTask<*>? = null
    private lateinit var progressDialog : ProgressDialog
    private lateinit var cashierAdapter: CashierAdapter
    private fun init(uid : String) {
        dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.bottom_sheet_add_employee)
        cashierImage = dialog.findViewById(R.id.cashierImage)!!
        inputCashierName = dialog.findViewById(R.id.inputEmployeeName)!!
        inputCashierPin = dialog.findViewById(R.id.inputEmployeePin)!!
        buttonGotoGallery = dialog.findViewById(R.id.buttonGallery)!!
        buttonSaveEmployee = dialog.findViewById(R.id.buttonSaveEmployee)!!
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance().getReference("cashier")
        firebaseQueries = FirebaseQueries(this,firestore)
        progressDialog = ProgressDialog(this)
        cashierAdapter = CashierAdapter(this,firebaseQueries.getCashiers(uid),this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCashierBinding.inflate(layoutInflater)
        setContentView(binding.root)
        uid = intent.getStringExtra(User.USER_ID)
        init(uid!!)
        //Get image in the gallery
        galleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val data = result.data
                try {
                    if (data?.data != null) {
                        cashierURI = data.data
                        cashierImage.setImageURI(cashierURI)
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        binding.buttonBack.setOnClickListener {
            finish()
        }
        binding.recyclerviewCashier.apply {
            layoutManager = LinearLayoutManager(this@CashierActivity)
            adapter = cashierAdapter
            addItemDecoration(
                DividerItemDecoration(this@CashierActivity,
                    DividerItemDecoration.VERTICAL)
            )
        }
        binding.buttonAddEmployee.setOnClickListener {
            buttonGotoGallery.setOnClickListener {
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galleryLauncher!!.launch(galleryIntent)
            }
            buttonSaveEmployee.setOnClickListener {
                validateCashier()
            }

            if (!dialog.isShowing) {
                dialog.show()
            }
        }
    }
    private fun validateCashier() {
        val cashierName = inputCashierName.text.toString()
        val cashierPin = inputCashierPin.text.toString()
        when {
            cashierURI == null -> {
                Toast.makeText(this,"Please add employee image",Toast.LENGTH_SHORT).show()
            }
            cashierName.isEmpty() -> {
                inputCashierName.error = " Enter employee name"
            }
            cashierPin.isEmpty() -> {
                inputCashierPin.error = "Enter employee pin"
            }
            else -> {
                progressDialog.loading()
                val fileReference = storage!!.child(System.currentTimeMillis().toString() + "." + getFileExtension(cashierURI!!))
                mUploadTask = fileReference.putFile(cashierURI!!)
                    .addOnSuccessListener {
                        fileReference.downloadUrl.addOnSuccessListener { uri: Uri ->
                            cashierURI = uri
                            val cashierID = firebaseQueries.generateID(Cashier.TABLE_NAME)
                            val cashier = Cashier(cashierID,cashierURI.toString(),cashierName,cashierPin)
                            createCashier(uid!!,cashier)
                            progressDialog.stopLoading()
                            dialog.dismiss()
                        }
                    }
            }
        }
    }
    //Cashier
    private fun createCashier(uid: String,cashier: Cashier) {
        firestore.collection(User.TABLE_NAME).document(uid)
            .collection(Cashier.TABLE_NAME)
            .document(cashier.cashierID!!)
            .set(cashier)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Cashier account created successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show()
                }
            }
    }
    //TODO: get the file extension of the file
    private fun getFileExtension(uri: Uri): String? {
        val cR = contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(cR.getType(uri))
    }

    override fun onStart() {
        super.onStart()
        cashierAdapter.startListening()
    }
    override fun onCashierClick(pos: Int) {
        Toast.makeText(this,cashierAdapter.getItem(pos).cashierName,Toast.LENGTH_SHORT).show()
    }
}