package com.flysolo.cashregister.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.flysolo.cashregister.MainActivity
import com.flysolo.cashregister.R
import com.flysolo.cashregister.databinding.ActivityUpdateAccountBinding
import com.flysolo.cashregister.dialog.ProgressDialog
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.viewmodels.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.squareup.picasso.Picasso
import java.io.IOException

class UpdateAccount : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateAccountBinding
    private lateinit var userViewModel: UserViewModel
    private var user: User? = null
    private var imageURI: Uri? = null
    private var galleryLauncher: ActivityResultLauncher<Intent>? = null
    private lateinit var progressDialog: ProgressDialog

    private var storage: StorageReference? = null
    private var mUploadTask: StorageTask<*>? = null
    private val firebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        MainActivity.setTranslucentStatusBar(this)
        user = intent.getParcelableExtra(User.TABLE_NAME)
        progressDialog = ProgressDialog(this)
        storage = FirebaseStorage.getInstance().getReference("storeimages")

        if (user != null) {
            bindViews(user!!)
            this.user = user
            if (user!!.userProfile!!.isNotEmpty()) {
                imageURI = Uri.parse(user!!.userProfile)
            }
        }

        binding.buttonGallery.setOnClickListener {
            val galleryIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher!!.launch(galleryIntent)
        }
        //Get image in the gallery
        galleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val data = result.data
                try {
                    if (data?.data != null) {
                        imageURI = data.data
                        binding.imageAddStoreImage.setImageURI(imageURI)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.buttonSaveChanges.setOnClickListener {
            val firstname : String = binding.inputFirstname.text.toString()
            val lastname : String = binding.inputLastname.text.toString()
            val businessName : String = binding.inputBusinessName.text.toString()
            val phoneNumber : String = binding.inputPhone.text.toString()
            val storePin : String = binding.inputStorePin.text.toString()
            when {
                firstname.isEmpty() -> {
                    binding.inputFirstname.error = "Input firstname"
                }
                lastname.isEmpty() -> {
                    binding.inputLastname.error = "Input lastname"
                }
                businessName.isEmpty() -> {
                    binding.inputBusinessName.error = "Input business name"
                }
                phoneNumber.isEmpty() -> {
                    binding.inputPhone.error = "Input phone number"
                }
                !phoneNumber.startsWith('9') -> {
                    binding.inputPhone.error = "Invalid phone number"
                }
                storePin.isEmpty() -> {
                    binding.inputStorePin.error = "Input Store Pin"
                }
                storePin.length != 6 -> {
                    binding.inputStorePin.error = "Invalid Store Pin"
                }
                else -> {
                    if (imageURI != null){
                        if (imageURI.toString() == user?.userProfile){
                            progressDialog.loading()
                            val user = User(
                                this.user?.userId,
                                imageURI.toString(),
                                firstname,
                                lastname,
                                businessName,
                                storePin,
                                phoneNumber,
                                this.user?.userEmail)
                            updateProfile(user)
                        } else {
                            uploadProfile(imageURI!!,firstname,lastname,businessName,phoneNumber,storePin)
                        }
                    }
                }
            }
        }
    }
    //TODO: get the file extension of the file
    private fun getFileExtension(uri: Uri): String? {
        val cR = this.contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(cR.getType(uri))
    }
    private fun uploadProfile(profileUri: Uri,firstname : String,lastname: String,businessName : String,phone : String,storePin : String){
        progressDialog.loading()
        val fileReference = storage!!.child(System.currentTimeMillis().toString() + "." + getFileExtension(profileUri))
        mUploadTask = fileReference.putFile(profileUri)
            .addOnSuccessListener {
                fileReference.downloadUrl.addOnSuccessListener { uri: Uri ->
                    this.imageURI = uri
                    val user = User(
                        this.user?.userId,
                        imageURI.toString(),
                        firstname,
                        lastname,
                        businessName,
                        storePin,
                        phone,
                        this.user?.userEmail)
                    updateProfile(user)
                }
            }
    }
    private fun updateProfile(user : User){
        firebaseFirestore.collection(User.TABLE_NAME)
            .document(user.userId!!)
            .set(user)
            .addOnCompleteListener {
                if (it.isSuccessful){
                    Toast.makeText(this,"Success", Toast.LENGTH_SHORT).show()
                    progressDialog.stopLoading()
                    finish()
                } else {
                    Toast.makeText(this,"Failed",Toast.LENGTH_SHORT).show()
                    progressDialog.stopLoading()
                }
            }
    }
    private fun bindViews(user : User) {
        if (user.userProfile!!.isNotEmpty()) {
            Picasso.get().load(user.userProfile).placeholder(R.drawable.store).into(binding.imageAddStoreImage)
        }
        binding.inputFirstname.setText(user.userFirstname)
        binding.inputLastname.setText(user.userLastname)
        binding.inputBusinessName.setText(user.userBusinessName)
        binding.inputEmail.setText(user.userEmail)
        binding.inputPhone.setText(user.userPhoneNumber.toString())
        binding.inputStorePin.setText(user.userStorePin.toString())
    }
}