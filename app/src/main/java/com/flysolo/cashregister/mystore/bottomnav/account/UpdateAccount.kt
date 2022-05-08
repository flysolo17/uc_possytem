package com.flysolo.cashregister.mystore.bottomnav.account

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.flysolo.cashregister.R
import com.flysolo.cashregister.databinding.FragmentUpdateAccountBinding
import com.flysolo.cashregister.dialogs.ProgressDialog
import com.flysolo.cashregister.firebase.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.squareup.picasso.Picasso
import java.io.IOException


class UpdateAccount : Fragment() {

    private lateinit var binding : FragmentUpdateAccountBinding
    private lateinit var userViewModel: UserViewModel
    private var user : User? = null
    private var imageURI : Uri? = null
    private var galleryLauncher: ActivityResultLauncher<Intent>? = null
    private lateinit var progressDialog: ProgressDialog

    private var storage: StorageReference? = null
    private var mUploadTask: StorageTask<*>? = null
    private val firebaseFirestore = FirebaseFirestore.getInstance()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUpdateAccountBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressDialog = ProgressDialog(requireActivity())
        storage = FirebaseStorage.getInstance().getReference("storeimages")
        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
        userViewModel.getUser().observe(viewLifecycleOwner, { user ->
            this.user = user
            bindViews(user)
            if (user.userProfile.isNotEmpty()){
                imageURI = Uri.parse(user.userProfile)
            }
        })
        binding.buttonClose.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_updateAccount_to_nav_account)
        }
        binding.buttonSave.setOnClickListener {
            val firstname : String = binding.inputName.editText?.text.toString()
            val lastname : String = binding.inputLastName.editText?.text.toString()
            val businessName : String = binding.inputBusinessName.editText?.text.toString()
            val phoneNumber : String = binding.inputPhone.editText?.text.toString()
            val storePin : String = binding.inputUpdateStorePin.editText?.text.toString()
            when {
                firstname.isEmpty() -> {
                    binding.inputName.error = "Input firstname"
                }
                lastname.isEmpty() -> {
                    binding.inputLastName.error = "Input lastname"
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
                    binding.inputUpdateStorePin.error = "Input Store Pin"
                }
                storePin.length != 6 -> {
                    binding.inputUpdateStorePin.error = "Invalid Store Pin"
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
                        Navigation.findNavController(requireView()).navigate(R.id.action_updateAccount_to_nav_account)
                    }
                }
            }
        }
        binding.textEditProfile.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher!!.launch(galleryIntent)
        }
        //Get image in the gallery
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val data = result.data
            try {
                if (data?.data != null) {
                    imageURI = data.data
                    binding.image.setImageURI(imageURI)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }
    //TODO: get the file extension of the file
    private fun getFileExtension(uri: Uri): String? {
        val cR = requireContext().contentResolver
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
                    Toast.makeText(requireContext(),"Success",Toast.LENGTH_SHORT).show()
                    progressDialog.stopLoading()
                } else {
                    Toast.makeText(requireContext(),"Failed",Toast.LENGTH_SHORT).show()
                    progressDialog.stopLoading()
                }
            }
    }
    private fun bindViews(user : User){
        if (user.userProfile.isNotEmpty()){
            Picasso.get().load(user.userProfile).into(binding.image)
        }
        if (user.userBusinessName!!.isNotEmpty()){
            binding.inputBusinessName.editText!!.setText(user.userBusinessName)
        }
        if (user.userFirstname!!.isNotEmpty()){
            binding.inputName.editText!!.setText(user.userFirstname)
        }
        if (user.userLastname!!.isNotEmpty()){
            binding.inputLastName.editText!!.setText(user.userLastname)
        }
        if (user.userPhoneNumber!!.isNotEmpty()){
            binding.inputPhone.editText!!.setText(user.userPhoneNumber)
        }
        if (user.userStorePin!!.isNotEmpty()){
            binding.inputUpdateStorePin.editText!!.setText(user.userStorePin)
        }
    }
}