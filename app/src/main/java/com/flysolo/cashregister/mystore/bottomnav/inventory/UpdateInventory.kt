package com.flysolo.cashregister.mystore.bottomnav.inventory

import android.content.Intent
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.flysolo.cashregister.R

import com.flysolo.cashregister.databinding.FragmentUpdateInventoryBinding
import com.flysolo.cashregister.dialogs.ProgressDialog
import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.mystore.bottomnav.inventory.viewmodel.InventoryViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.squareup.picasso.Picasso
import java.io.IOException


class UpdateInventory : Fragment() {
    private lateinit var binding : FragmentUpdateInventoryBinding
    private lateinit var inventoryViewModel: InventoryViewModel
    private val firestore = FirebaseFirestore.getInstance()
    private var galleryLauncher: ActivityResultLauncher<Intent>? = null
    private var imageURI: Uri? = null
    private lateinit var categryAdapter : ArrayAdapter<*>
    private  var itemID : String? = null
    private var storage: StorageReference? = null
    private var mUploadTask: StorageTask<*>? = null
    private lateinit var progressDialog : ProgressDialog
    private var newURI : String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUpdateInventoryBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        storage = FirebaseStorage.getInstance().getReference("items")
        progressDialog = ProgressDialog(requireActivity())
        inventoryViewModel = ViewModelProvider(requireActivity())[InventoryViewModel::class.java]
        categryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,getAllCategories())
        binding.updateCategory.apply {
            threshold = 1
            setAdapter(categryAdapter)
        }

        inventoryViewModel.getItem().observe(viewLifecycleOwner, { item ->
            imageURI = Uri.parse(item.itemImageURL.toString())
            itemID = item.itemBarcode
            bindViews(item)
        })
        //Get image in the gallery
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val data = result.data
            try {
                if (data?.data != null) {
                    binding.buttonAddImage.setImageURI(data.data)
                    imageURI = data.data
                    uploadItemImage(imageURI!!)
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        binding.buttonAddImage.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher!!.launch(galleryIntent)
        }
        binding.buttonClose.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_updateInventory_to_nav_inventory)
        }
        binding.buttonUpdate.setOnClickListener {
            val itemName: String = binding.inputUpdateName.editText?.text.toString()
            val itemCategory: String = binding.updateCategory.text.toString()
            val itemCost: String = binding.inputUpdateCost.editText?.text.toString()
            val itemPrice: String = binding.inputUpdatePrice.editText?.text.toString()
            val itemQuantity: String = binding.inputUpdateQuantity.editText?.text.toString()
            when {
                itemName.isEmpty() -> {
                    binding.inputUpdateName.error = "Enter item name"
                }
                itemCategory.isEmpty() -> {
                    binding.inputUpdateCategory.error = "Enter item category"
                }
                itemCost.isEmpty() -> {
                    binding.inputUpdateCost.error = "Enter item cost"
                }
                itemPrice.isEmpty() -> {
                    binding.inputUpdatePrice.error = "Enter item price"

                }
                itemQuantity.isEmpty() -> {
                    binding.inputUpdateQuantity.error = "Enter item quantity"

                }
                else -> {
                    val items = Items(
                        itemID,
                        imageURI.toString(),
                        itemName,
                        itemCategory,
                        Integer.parseInt(itemQuantity),
                        Integer.parseInt(itemCost),
                        Integer.parseInt(itemPrice),
                        System.currentTimeMillis())
                    updateItem(items)
                }
            }
        }
    }

    private fun uploadItemImage(uri: Uri){
        progressDialog.loading()
        if (imageURI != null) {
            val fileReference = storage!!.child(System.currentTimeMillis().toString() + "." + getFileExtension(uri))
            mUploadTask = fileReference.putFile(uri)
                .addOnSuccessListener {
                    fileReference.downloadUrl.addOnSuccessListener { uri: Uri ->
                        this.imageURI = uri
                        updateImage(imageURI.toString())
                    }
                }.addOnFailureListener {
                    progressDialog.stopLoading()
                }
        }
    }
    private fun updateImage(newImage : String){
        firestore.collection(User.TABLE_NAME)
            .document(FirebaseAuth.getInstance().currentUser!!.uid)
            .collection(Items.TABLE_NAME)
            .document(itemID!!)
            .update(Items.IMAGE,newImage)
            .addOnCompleteListener {
                if (it.isSuccessful){
                    progressDialog.stopLoading()
                    Toast.makeText(requireContext(),"Image Successfully Updated",Toast.LENGTH_SHORT).show()
                    Picasso.get().load(newImage).into(binding.buttonAddImage)
                } else {
                    progressDialog.stopLoading()
                    Toast.makeText(requireContext(),"Failed to update image",Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun updateItem(items: Items){
        progressDialog.loading()
        firestore.collection(User.TABLE_NAME)
            .document(FirebaseAuth.getInstance().currentUser!!.uid)
            .collection(Items.TABLE_NAME)
            .document(itemID!!)
            .set(items)
            .addOnCompleteListener {
                if (it.isSuccessful){
                    progressDialog.stopLoading()
                    Toast.makeText(requireContext(),"Successfully Updated",Toast.LENGTH_SHORT).show()
                    Navigation.findNavController(requireView()).navigate(R.id.action_updateInventory_to_nav_inventory)
                } else {
                    progressDialog.stopLoading()
                    Toast.makeText(requireContext(),"Failed to update item",Toast.LENGTH_SHORT).show()
                }
            }
    }
    //TODO: get the file extension of the file
    private fun getFileExtension(uri: Uri): String? {
        val cR = requireContext().contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(cR.getType(uri))
    }
    private fun bindViews(items : Items){
        if (items.itemImageURL!!.isNotEmpty()){
            Picasso.get().load(items.itemImageURL).into(binding.buttonAddImage)
        }
        if (items.itemName!!.isNotEmpty()){
            binding.inputUpdateName.editText?.setText(items.itemName)
        }
        if (items.itemCategory!!.isNotEmpty()){
            binding.updateCategory.setText(items.itemCategory)
        }
        if (items.itemQuantity != null){
            binding.inputUpdateQuantity.editText?.setText(items.itemQuantity.toString())
        }
        if (items.itemCost != null){
            binding.inputUpdateCost.editText?.setText(items.itemCost.toString())
        }
        if (items.itemQuantity != null){
            binding.inputUpdatePrice.editText?.setText(items.itemPrice.toString())
        }
    }

    private fun getAllCategories() : MutableList<String>{
        val list : MutableList<String> = mutableListOf()
        firestore.collection(User.TABLE_NAME)
            .document(FirebaseAuth.getInstance().currentUser!!.uid)
            .collection(Items.TABLE_NAME)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val item = document.toObject(Items::class.java)
                    if (!list.contains(item.itemCategory)){
                        list.add(item.itemCategory!!)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("AddRecipe", "Error getting documents: ", exception)
            }
        return  list
    }

}