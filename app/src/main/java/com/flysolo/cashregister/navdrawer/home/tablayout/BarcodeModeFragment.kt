package com.flysolo.cashregister.navdrawer.home.tablayout

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity

import com.flysolo.cashregister.databinding.FragmentBarcodeModeBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.models.Items
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.ml.ModelUnquant
import com.flysolo.cashregister.navdrawer.home.adapter.ItemListAdapter
import com.flysolo.cashregister.navdrawer.home.models.Scanning
import com.flysolo.cashregister.navdrawer.home.viewmodels.PurchasingViewModel
import com.flysolo.cashregister.purchases.ItemPurchased
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.squareup.picasso.Picasso
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BarcodeModeFragment : Fragment(),ItemListAdapter.OnItemIsClick {
    private lateinit var binding : FragmentBarcodeModeBinding
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var itemList : List<String>
    private lateinit var firebaseQueries: FirebaseQueries
    private var imageSize = 224
    private lateinit var purchasingViewModel: PurchasingViewModel
    private lateinit var adapter : ItemListAdapter
    private val firebaseFirestore  = FirebaseFirestore.getInstance()
    private var itemResultBitmap : Bitmap?  = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentBarcodeModeBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        purchasingViewModel = ViewModelProvider(requireActivity()).get(PurchasingViewModel::class.java)
        firebaseQueries = FirebaseQueries(requireContext(), firebaseFirestore)

        val fileName="labels.txt"
        val inputString= requireActivity().assets.open(fileName).bufferedReader().use { it.readText() }
        itemList=inputString.split("\n")



        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            var bitmap = result.data!!.extras!!.get("data") as Bitmap?
            itemResultBitmap = bitmap
            if (bitmap != null) {
                val dimension: Int = Math.min(bitmap.getWidth(), bitmap.getHeight())
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension)
                bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false)
                classifyImage(bitmap)
            }
        }
        binding.imageResult.setOnClickListener {
            launchCamera(cameraLauncher)
        }

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
                binding.imageResult.setImageBitmap(itemResultBitmap)
                binding.textResult.text = "Item didn't recognize"
            }
            model.close()
        } catch (e: IOException) {
            Log.d(".BarcodeModeFragment", e.message.toString())
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun launchCamera(launcher: ActivityResultLauncher<Intent>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                launcher.launch(intent)
            }

        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            launcher.launch(intent)
        }
    }

    override fun itemClick(position: Int) {
        val item = adapter.getItem(position)
        purchasingViewModel.setItem(ItemPurchased(item.itemBarcode, item.itemName, 1, item.itemCost,1 * item.itemPrice!!,false))
    }

    private fun getScanResult(result: String) {
        firebaseFirestore.collection(Scanning.TABLE_NAME)
            .document(result)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val scanning = document.toObject(Scanning::class.java)
                    bindResult(scanning?.id!!)
                } else {
                    Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun bindResult(itemID : String){
        firebaseFirestore.collection(User.TABLE_NAME)
            .document(FirebaseAuth.getInstance().currentUser!!.uid)
            .collection(Items.TABLE_NAME)
            .document(itemID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()){
                    val items = document.toObject(Items::class.java)
                    if (items?.itemImageURL!!.isNotEmpty()){
                        Picasso.get().load(items.itemImageURL).into(binding.imageResult)
                    }
                    binding.textResult.text = items.itemName
                    binding.frame.setBackgroundColor(Color.GREEN)
                    purchasingViewModel.setItem(ItemPurchased(items.itemBarcode,
                        items.itemName, 1, items.itemCost,items.itemPrice,false))
                } else {
                    binding.imageResult.setImageBitmap(itemResultBitmap)
                    binding.textResult.text = "item is not available to the inventory"
                    binding.frame.setBackgroundColor(Color.RED)
                }
            }
    }
}