package com.flysolo.cashregister.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment

import com.flysolo.cashregister.databinding.FragmentAddInventoryBinding
import com.flysolo.cashregister.dialog.ProgressDialog
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.login.LoginActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import java.io.IOException
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanOptions

class AddInventory : DialogFragment() {
    private lateinit var binding : FragmentAddInventoryBinding
    private var imageURI: Uri? = null
    private var storage: StorageReference? = null
    private var mUploadTask: StorageTask<*>? = null
    private var firebaseQueries: FirebaseQueries? = null
    private val firestore = FirebaseFirestore.getInstance()
    private var galleryLauncher: ActivityResultLauncher<Intent>? = null
    private lateinit var progressDialog : ProgressDialog
    private lateinit var categoryAdapter : ArrayAdapter<*>
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
        binding = FragmentAddInventoryBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressDialog = ProgressDialog(requireActivity())
        firebaseQueries = FirebaseQueries(requireContext(), firestore)
        storage = FirebaseStorage.getInstance().getReference("items")

        categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            getAllCategories()
        )
        binding.inputCategory.apply {
            threshold = 1
            setAdapter(categoryAdapter)
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
        //TODO: launch barcode scanner and get the result
        val barcodeLauncher =
            registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
                if (result.contents == null) {
                    val originalIntent = result.originalIntent
                    if (originalIntent == null) {
                        Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_LONG).show()
                    } else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                        Toast.makeText(
                            requireContext(),
                            "Cancelled due to missing camera permission",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    binding.inputBarcode.setText(result.contents)
                }
            }
        binding.buttonScanBarcode.setOnClickListener {
            barcodeLauncher.launch(ScanOptions())
        }

        binding.buttonGallery.setOnClickListener {
            launchGallery()
        }

        binding.buttonAddInventory.setOnClickListener {
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
        val itemBarcode: String = binding.inputBarcode.text.toString()
        val itemCategory: String = binding.inputCategory.text.toString()
        val itemCost: String = binding.inputCost.text.toString()
        val itemPrice: String = binding.inputPrice.text.toString()
        val itemQuantity: String = binding.inputQuantity.text.toString()
        when {
            itemName.isEmpty() -> {
                binding.inputItemName.error = "Enter item name"

            }
            itemBarcode.isEmpty() -> {
                binding.inputBarcode.error = "Enter item barcode"

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
                    progressDialog.loading()
                    val fileReference = storage!!.child(System.currentTimeMillis().toString() + "." + getFileExtension(imageURI!!))
                    mUploadTask = fileReference.putFile(imageURI!!)
                        .addOnSuccessListener {
                            fileReference.downloadUrl.addOnSuccessListener { uri: Uri ->
                                val items = Items(
                                    itemBarcode,
                                    uri.toString(),
                                    itemName,
                                    itemCategory,
                                    Integer.parseInt(itemQuantity),
                                    Integer.parseInt(itemCost),
                                    Integer.parseInt(itemPrice),
                                    System.currentTimeMillis())
                                firebaseQueries?.addItem(items)
                                progressDialog.stopLoading()
                                dismiss()
                            }
                        }
                } else {
                    Toast.makeText(requireContext(),"Failed: you need to put an image to the item",Toast.LENGTH_SHORT).show()
                }
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
}