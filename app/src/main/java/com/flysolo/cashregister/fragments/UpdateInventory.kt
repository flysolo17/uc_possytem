package com.flysolo.cashregister.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri

import android.provider.MediaStore
import android.util.Log

import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.flysolo.cashregister.databinding.FragmentUpdateInventoryBinding
import com.flysolo.cashregister.dialog.ProgressDialog
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.login.LoginActivity
import com.flysolo.cashregister.mystore.bottomnav.inventory.viewmodel.InventoryViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import java.io.IOException
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanOptions
import com.squareup.picasso.Picasso

class UpdateInventory : DialogFragment() {
    private lateinit var binding : FragmentUpdateInventoryBinding
    private var imageURI: Uri? = null
    private var storage: StorageReference? = null
    private var mUploadTask: StorageTask<*>? = null
    private var firebaseQueries: FirebaseQueries? = null
    private val firestore = FirebaseFirestore.getInstance()
    private var galleryLauncher: ActivityResultLauncher<Intent>? = null
    private lateinit var progressDialog : ProgressDialog
    private lateinit var categryAdapter : ArrayAdapter<*>
    private  var items: Items?  = null
    private lateinit var invetoryViewModel: InventoryViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
    }
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
        progressDialog = ProgressDialog(requireActivity())
        invetoryViewModel = ViewModelProvider(requireActivity())[InventoryViewModel::class.java]
        invetoryViewModel.getItem().observe(viewLifecycleOwner, Observer { items ->
            if (items != null) {
                this.items = items
                bindViews(items)
            }
        })

        firebaseQueries = FirebaseQueries(requireContext(), firestore)
        storage = FirebaseStorage.getInstance().getReference("items")

        categryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            getAllCategories()
        )
        binding.inputCategory.apply {
            threshold = 1
            setAdapter(categryAdapter)
        }
        binding.buttonBack.setOnClickListener {
            dismiss()
        }
        //Get image in the gallery
        galleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val data = result.data
                try {
                    if (data?.data != null) {
                        imageURI = data.data
                        binding.itemImage.setImageURI(imageURI)
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

        binding.buttonGallery.setOnClickListener {
            launchGallery()
        }

        binding.buttonUpdateInventory.setOnClickListener {
            uploadInventory()
        }


    }
    //TODO: pick image from the gallery
    private fun launchGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher!!.launch(galleryIntent)
    }

    //TODO: get the file extension of the file
    private fun getFileExtension(uri: Uri): String? {
        val cR = requireContext().contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(cR.getType(uri))
    }

    //TODO: upload file in firestore and storage
    private fun uploadInventory() {
        val itemName: String = binding.inputItemName.text.toString()
        val itemCategory: String = binding.inputCategory.text.toString()
        val itemCost: String = binding.inputCost.text.toString()
        val itemPrice: String = binding.inputPrice.text.toString()
        val itemQuantity: String = binding.inputQuantity.text.toString()
        when {
            itemName.isEmpty() -> {
                binding.inputItemName.error = "Enter item name"

            }
            itemCategory.isEmpty() -> {
                binding.inputCategory.error = "Enter item category"

            }
            itemCost.isEmpty() -> {
                binding.inputCost.error = "Enter item cost"

            }
            itemPrice.isEmpty() -> {
                binding.inputPrice.error = "Enter item price"

            }
            itemQuantity.isEmpty() -> {
                binding.inputQuantity.error = "Enter item quantity"

            }
            else -> {
                if (imageURI != null) {
                    if (imageURI.toString() == items?.itemImageURL) {
                        val items = Items(
                            this.items!!.itemBarcode,
                            imageURI.toString(),
                            itemName,
                            itemCategory,
                            Integer.parseInt(itemQuantity),
                            Integer.parseInt(itemCost),
                            Integer.parseInt(itemPrice),
                            System.currentTimeMillis())
                        updateItem(items)
                    } else {
                        uploadItemImage(
                            imageURI!!,
                            itemName,
                            itemCategory,
                            Integer.parseInt(itemQuantity),
                            Integer.parseInt(itemCost),
                            Integer.parseInt(itemPrice))
                    }
                    dismiss()
                }
            }
        }
    }
    private fun updateItem(items: Items){
        progressDialog.loading()
        firestore.collection(User.TABLE_NAME)
            .document(LoginActivity.uid)
            .collection(Items.TABLE_NAME)
            .document(items.itemBarcode!!)
            .set(items)
            .addOnCompleteListener {
                if (it.isSuccessful){
                    progressDialog.stopLoading()
                    Toast.makeText(binding.root.context,"Successfully Updated",Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    progressDialog.stopLoading()
                    Toast.makeText(binding.root.context,"Failed to update item",Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun getAllCategories() : MutableList<String>{
        val list : MutableList<String> = mutableListOf()
        firestore.collection(User.TABLE_NAME)
            .document(LoginActivity.uid)
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
    private fun bindViews(items : Items){
        if (items.itemImageURL!!.isNotEmpty()){
            Picasso.get().load(items.itemImageURL).into(binding.itemImage)
            imageURI = Uri.parse(items.itemImageURL)
        }
        if (items.itemName!!.isNotEmpty()){
            binding.inputItemName.setText(items.itemName)
        }
        if (items.itemCategory!!.isNotEmpty()){
            binding.inputCategory.setText(items.itemCategory)
        }
        if (items.itemQuantity != null){
            binding.inputQuantity.setText(items.itemQuantity.toString())
        }
        if (items.itemCost != null){
            binding.inputCost.setText(items.itemCost.toString())
        }
        if (items.itemQuantity != null){
            binding.inputPrice.setText(items.itemPrice.toString())
        }
    }
    private fun uploadItemImage(uri: Uri,
                                itemName : String ,
                                itemCategory: String,
                                itemQuantity: Int,
                                itemCost : Int,
                                itemPrice : Int){
        progressDialog.loading()
        val fileReference = storage!!.child(System.currentTimeMillis().toString() + "." + getFileExtension(uri))
        mUploadTask = fileReference.putFile(uri)
            .addOnSuccessListener {
                fileReference.downloadUrl.addOnSuccessListener { uri: Uri ->
                    this.imageURI = uri
                    val items = Items(
                        this.items?.itemBarcode,
                        imageURI.toString(),
                        itemName,
                        itemCategory,
                        itemQuantity,
                        itemCost,
                        itemPrice,
                        System.currentTimeMillis())
                    progressDialog.stopLoading()
                    updateItem(items)
                }
            }.addOnFailureListener {
                progressDialog.stopLoading()
            }
    }

}