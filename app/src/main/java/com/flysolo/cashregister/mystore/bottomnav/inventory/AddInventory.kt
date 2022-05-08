package com.flysolo.cashregister.mystore.bottomnav.inventory




import android.content.Intent
import android.graphics.Bitmap
import android.media.ThumbnailUtils

import android.net.Uri


import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher

import androidx.activity.result.contract.ActivityResultContracts

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation

import com.flysolo.cashregister.R
import com.flysolo.cashregister.databinding.FragmentAddInventoryBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.ml.ModelUnquant
import com.flysolo.cashregister.navdrawer.home.models.Scanning
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.google.zxing.client.android.Intents
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AddInventory : Fragment() {

    private lateinit var binding: FragmentAddInventoryBinding
    private var imageURI: Uri? = null
    private var storage: StorageReference? = null
    private var mUploadTask: StorageTask<*>? = null
    private var firebaseQueries: FirebaseQueries? = null
    private val firestore = FirebaseFirestore.getInstance()
    private var galleryLauncher: ActivityResultLauncher<Intent>? = null

    private lateinit var categryAdapter : ArrayAdapter<*>

    private lateinit var itemList : List<String>
    private var imageSize = 224
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAddInventoryBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fileName="labels.txt"
        val inputString= requireActivity().assets.open(fileName).bufferedReader().use { it.readText() }
        itemList=inputString.split("\n")

        firebaseQueries = FirebaseQueries(requireContext(),firestore)
        storage = FirebaseStorage.getInstance().getReference("items")

        categryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line,getAllCategories())
        binding.textCategories.apply {
            threshold = 1
            setAdapter(categryAdapter)
        }
        binding.buttonClose.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_addInventory_to_nav_inventory)
        }
        //Get image in the gallery
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val data = result.data
            try {
                if (data?.data != null) {
                    imageURI = data.data
                    var bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, data.data)
                    if (bitmap != null) {
                        val dimension: Int = Math.min(bitmap.getWidth(), bitmap.getHeight())
                        bitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension)
                        bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false)
                        classifyImage(bitmap)
                        binding.buttonAddImage.setImageBitmap(bitmap)
                    }
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        //TODO: launch barcode scanner and get the result
        val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
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
                    binding.inputItemId.editText?.setText(result.contents)
                }
            }

        binding.buttonScanBarcode.setOnClickListener {
            barcodeLauncher.launch(ScanOptions())
        }

        binding.buttonAddImage.setOnClickListener {
            launchGallery()
        }
        binding.buttonSaveInventory.setOnClickListener {
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
        binding.progressIndicator.isVisible = true
        val itemName: String = binding.inputItemName.editText?.text.toString()
        val itemBarcode: String = binding.inputItemId.editText?.text.toString()
        val itemCategory: String = binding.textCategories.text.toString()
        val itemCost: String = binding.inputItemCost.editText?.text.toString()
        val itemPrice: String = binding.inputItemPrice.editText?.text.toString()
        val itemQuantity: String = binding.inputItemQuantity.editText?.text.toString()
        when {
            itemName.isEmpty() -> {
                binding.inputItemName.error = "Enter item name"
                binding.progressIndicator.isVisible = false
            }
            itemBarcode.isEmpty() -> {
                binding.inputItemId.error = "Enter item barcode"
                binding.progressIndicator.isVisible = false
            }

            itemCategory.isEmpty() -> {
                binding.inputItemCategory.error = "Enter item category"
                binding.progressIndicator.isVisible = false
            }
            itemCost.isEmpty() -> {
                binding.inputItemCost.error = "Enter item cost"
                binding.progressIndicator.isVisible = false
            }
            itemPrice.isEmpty() -> {
                binding.inputItemPrice.error = "Enter item price"
                binding.progressIndicator.isVisible = false
            }
            itemQuantity.isEmpty() -> {
                binding.inputItemQuantity.error = "Enter item quantity"
                binding.progressIndicator.isVisible = false
            }
            else -> {
                if (imageURI != null) {
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
                                binding.progressIndicator.isVisible = false
                                Navigation.findNavController(requireView()).navigate(R.id.action_addInventory_to_nav_inventory)
                            }
                        }
                } else {
                    binding.progressIndicator.isVisible = false
                    Toast.makeText(requireContext(),"Failed: you need to put an image to the item",Toast.LENGTH_SHORT).show()
                }
            }
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

    private fun classifyImage(image: Bitmap){
        try {
            val model = ModelUnquant.newInstance(requireContext())
            // Creates inputs for reference.
            val inputFeature0 =
                TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
            byteBuffer.order(ByteOrder.nativeOrder())

            // get 1D array of 224 * 224 pixels in image
            val intValues = IntArray(imageSize * imageSize)
            image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)

            // iterate over pixels and extract R, G, and B values. Add to bytebuffer.
            var pixel = 0
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val `val` = intValues[pixel++] // RGB
                    byteBuffer.putFloat((`val` shr 16 and 0xFF) * (1f / 255f))
                    byteBuffer.putFloat((`val` shr 8 and 0xFF) * (1f / 255f))
                    byteBuffer.putFloat((`val` and 0xFF) * (1f / 255f))
                }
            }
            inputFeature0.loadBuffer(byteBuffer)
            // Runs model inference and gets result.
            val outputs= model.process(inputFeature0)
            val outputFeature0: TensorBuffer = outputs.outputFeature0AsTensorBuffer
            val confidences = outputFeature0.floatArray

            var maxPos = 0
            var maxConfidence = 0f
            for (i in confidences.indices) {
                if (confidences[i] > maxConfidence ) {
                    maxConfidence = confidences[i]
                    maxPos = i

                }
            }
            if (maxConfidence * 100 > 60f){
                getScanResult(itemList[maxPos])
            }else {
                Toast.makeText(requireContext(),"I don't recognize the item",Toast.LENGTH_SHORT).show()
            }

            model.close()
        } catch (e: IOException) {
            Log.d(".BarcodeModeFragment", e.message.toString())
        }
    }
    private fun getScanResult(result: String) {
        firestore.collection(Scanning.TABLE_NAME)
            .document(result)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val scan = document.toObject(Scanning::class.java)
                    if (scan!!.name!!.isNotEmpty()){
                        binding.inputItemName.editText?.setText(scan.name)
                    }
                    if (scan.id!!.isNotEmpty()){
                        binding.inputItemId.editText?.setText(scan.id)
                    }
                }
            }
    }
}