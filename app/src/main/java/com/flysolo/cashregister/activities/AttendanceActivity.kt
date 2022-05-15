package com.flysolo.cashregister.activities

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.flysolo.cashregister.R
import com.flysolo.cashregister.adapter.AttendanceAdapter
import com.flysolo.cashregister.databinding.ActivityAttendanceBinding
import com.flysolo.cashregister.defaultCashier
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.QueryDates
import com.flysolo.cashregister.firebase.models.Attendance
import com.flysolo.cashregister.firebase.models.Cashier
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.login.LoginActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.squareup.picasso.Picasso
import java.io.IOException

const val defaultCashier = "Select Cashier"
class AttendanceActivity : AppCompatActivity(),AttendanceAdapter.SelfieOutIsClick {
    private lateinit var binding : ActivityAttendanceBinding
    private var cashierList = mutableListOf<Cashier>()
    private var cashierNames = mutableListOf(defaultCashier)
    private lateinit var firestore : FirebaseFirestore
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var attendanceAdapter: AttendanceAdapter
    private lateinit var firebaseQueries: FirebaseQueries
    private var selfieUri: Uri? = null
    private var storage: StorageReference? = null
    private var mUploadTask: StorageTask<*>? = null
    private val queryDates = QueryDates()

    private var cashier : Cashier ? = null
    private fun init(uid: String){
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance().getReference("attendance")
        firebaseQueries = FirebaseQueries(this, firestore)
        getALlCashier(uid)
        attendanceAdapter = AttendanceAdapter(this, firebaseQueries.getAttendance(
            queryDates.startOfDay(System.currentTimeMillis()),
            queryDates.endOfDay(System.currentTimeMillis())), this)
        binding.recyclerviewAttendance.apply {
            layoutManager = LinearLayoutManager(this@AttendanceActivity)
            adapter = attendanceAdapter
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val uid = intent.getStringExtra(User.USER_ID)
        init(uid!!)
        val cashierAdapter = ArrayAdapter(this,android.R.layout.simple_list_item_1,cashierNames)
        binding.cashierSpinner.apply {
            adapter = cashierAdapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    if (p2 != 0) {
                        showDialog(cashierList[p2-1])
                    }
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {
                    Toast.makeText(this@AttendanceActivity,"Please Select cashier", Toast.LENGTH_SHORT).show()
                }
            }

        }
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.data != null) {
                val bitmap = result.data!!.extras!!.get("data") as Bitmap?
                if (bitmap != null) {
                    selfieUri = convertBitmapToUri(System.currentTimeMillis().toString(), bitmap)
                    showAttendanceDialog(selfieUri!!,binding.cashierSpinner.selectedItem.toString())
                }
            }

        }

    }
    private fun launchCamera(launcher: ActivityResultLauncher<Intent>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(this.packageManager) != null) {
                launcher.launch(intent)
            }

        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            launcher.launch(intent)
        }
    }
    private fun convertBitmapToUri(name: String, bitmap: Bitmap): Uri? {
        val imageCollection: Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "images/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }
        return contentResolver.insert(imageCollection, contentValues)?.also {
            contentResolver.openOutputStream(it).use { outputStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                    throw IOException("Failed to save bitmap")
                }
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
        attendanceAdapter.startListening()
    }
    private var attendanceID: String? = null
    override fun selfieOutClick(position: Int) {
        attendanceID = attendanceAdapter.getItem(position).attendanceID
        launchCamera(selfieOutLauncher)
    }

    private val selfieOutLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val bitmap = result.data!!.extras!!.get("data") as Bitmap?
            if (bitmap != null) {
                val timestamp: Long = System.currentTimeMillis()
                val selfieOutUri = convertBitmapToUri(timestamp.toString(), bitmap)
                signOutSelfie(selfieOutUri!!)

            }
        }

    private fun signOutSelfie(uri: Uri) {
        val fileReference = storage!!.child(System.currentTimeMillis().toString() + "." + getFileExtension(uri))
        mUploadTask = fileReference.putFile(uri)
            .addOnSuccessListener {
                fileReference.downloadUrl.addOnSuccessListener { uri: Uri ->
                    firebaseQueries.signOutAttendance(attendanceID!!, uri.toString(),System.currentTimeMillis())
                }
            }
    }
    private fun showAttendanceDialog(resultImage : Uri, cashierName : String) {
        val dialogs = Dialog(this)
        dialogs.setTitle("Cashier Login")
        dialogs.setContentView(R.layout.dialog_attendance)
        val selfieIn : ImageView =  dialogs.findViewById(R.id.cashierSelfieIn)
        selfieIn.setImageURI(resultImage)
        val buttonRetake : Button = dialogs.findViewById(R.id.buttonRetake)
        val buttonSend : Button = dialogs.findViewById(R.id.buttonSend)
        buttonSend.setOnClickListener {
            when {
                cashierName.isEmpty() -> {
                    Toast.makeText(this,"cashier name is empty",Toast.LENGTH_SHORT).show()
                }
                cashierName == defaultCashier -> {
                    Toast.makeText(this,"invalid cashier",Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val fileReference = storage!!.child(
                        System.currentTimeMillis().toString() + "." + getFileExtension(resultImage!!)
                    )
                    mUploadTask = fileReference.putFile(resultImage!!)
                        .addOnSuccessListener {
                            fileReference.downloadUrl.addOnSuccessListener { uri: Uri ->
                                val attendance = Attendance(
                                    firebaseQueries.generateID(Attendance.TABLE_NAME),
                                    cashierName,
                                    uri.toString(),
                                    "",
                                    System.currentTimeMillis(), 0
                                )
                                firebaseQueries.signInAttendance(attendance)
                                dialogs.dismiss()
                                binding.cashierSpinner.setSelection(0)
                            }
                        }
                }
            }
        }
        buttonRetake.setOnClickListener {
            launchCamera(cameraLauncher)
        }
        if (!dialogs.isShowing) {
            dialogs.setTitle("Cashier Login")
            dialogs.show()
        }

    }
    private fun showDialog(cashier: Cashier) {
        val dialogs = Dialog(this)
        dialogs.setTitle("Cashier Login")
        dialogs.setContentView(R.layout.fragment_cashier_pin_dialog)
        this.cashier = cashier
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
                launchCamera(cameraLauncher)
                dialogs.dismiss()
            } else {
                inputPinCode.error = "Wrong Pin"
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

}