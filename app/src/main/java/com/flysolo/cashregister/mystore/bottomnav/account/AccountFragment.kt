package com.flysolo.cashregister.mystore.bottomnav.account

import android.annotation.SuppressLint
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
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.databinding.FragmentAccountBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.squareup.picasso.Picasso
import java.io.IOException

class AccountFragment : Fragment() {
    private lateinit var binding : FragmentAccountBinding
    private var galleryLauncher: ActivityResultLauncher<Intent>? = null
    private var storage: StorageReference? = null
    private var mUploadTask: StorageTask<*>? = null
    private var firebaseQueries : FirebaseQueries? = null
    private val firebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var userViewModel: UserViewModel
    private var user : User? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAccountBinding.inflate(inflater,container,false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
        user = CashierLoginActivity.user
        firebaseQueries = FirebaseQueries(requireContext(), FirebaseFirestore.getInstance())
        storage = FirebaseStorage.getInstance().getReference("storeimages")
        //Get image in the gallery
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val data = result.data
            try {
                if (data?.data != null) {
                    uploadProfile(data.data!!)
                    binding.imageAddStoreImage.setImageURI(data.data)
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }


        binding.imageAddStoreImage.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher!!.launch(galleryIntent)
        }
        binding.buttonLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireActivity(),LoginActivity::class.java))
        }
        binding.buttonEditAccount.setOnClickListener {
            userViewModel.setUser(user!!)
            Navigation.findNavController(requireView()).navigate(R.id.action_nav_account_to_updateAccount)
        }
    }
    private fun uploadProfile(profileUri: Uri){
        val fileReference = storage!!.child(System.currentTimeMillis().toString() + "." + getFileExtension(profileUri))
        mUploadTask = fileReference.putFile(profileUri)
            .addOnSuccessListener {
                fileReference.downloadUrl.addOnSuccessListener { uri: Uri ->
                    firebaseQueries?.updateProfile(uri.toString())
                    Picasso.get().load(uri).into(binding.imageAddStoreImage)
                }
            }
    }
    //TODO: get the file extension of the file
    private fun getFileExtension(uri: Uri): String? {
        val cR = requireContext().contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(cR.getType(uri))
    }

    private fun bindViews(user : User){
        if (user.userProfile.isNotEmpty()){
            Picasso.get().load(CashierLoginActivity.user?.userProfile).into(binding.imageAddStoreImage)
        }
        if (user.userBusinessName!!.isNotEmpty()){
            binding.textAccountBusinessName.text = user?.userBusinessName
        }
        if (user.userFirstname!!.isNotEmpty() && user.userLastname!!.isNotEmpty()){
            binding.textAccountOwnerName.text = user.userFirstname + " " + user.userLastname
        }
        if (user.userPhoneNumber!!.isNotEmpty()){
            binding.textAccountPhone.text = user.userPhoneNumber
        }
        if (user.userEmail!!.isNotEmpty()){
            binding.textAccountEmail.text = user.userEmail
        }
        if (user.userStorePin!!.isNotEmpty()){
            binding.textStorePin.text = user.userStorePin
        }
    }

    override fun onResume() {
        super.onResume()
        bindViews(CashierLoginActivity.user!!)
    }
}