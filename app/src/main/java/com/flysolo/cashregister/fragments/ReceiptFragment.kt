package com.flysolo.cashregister.fragments

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.flysolo.cashregister.R
import com.flysolo.cashregister.databinding.FragmentRecieptBinding
import com.flysolo.cashregister.firebase.models.ItemPurchased
import com.flysolo.cashregister.viewmodels.ReceiptViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.text.SimpleDateFormat

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_CASH = "cash"
private const val ARG_TOTAL= "total"
private const val ARG_CHANGE= "change"
private const val ARG_CASHIER= "cashier"
private const val ARG_STORE= "store"
/**
 * A simple [Fragment] subclass.
 * Use the [RecieptFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ReceiptFragment : DialogFragment() {
    // TODO: Rename and change types of parameters
    private var cash: String? = null
    private var total: String? = null
    private var change: String? = null
    private var cashier: String? = null
    private var store: String? = null
    private lateinit var binding : FragmentRecieptBinding

    private lateinit var receiptViewModel: ReceiptViewModel
    private lateinit var itemPurchasedList : MutableList<ItemPurchased>
    private val shareImage = ShareImage()
    private fun init() {
        binding.textTotal.text = total
        binding.textCash.text = cash
        binding.textChange.text = change
        binding.cashierName.text = cashier
        binding.textStoreName.text = store
        itemPurchasedList = mutableListOf()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        arguments?.let {
            total = it.getString(ARG_TOTAL)
            cash = it.getString(ARG_CASH)
            change = it.getString(ARG_CHANGE)
            cashier = it.getString(ARG_CASHIER)
            store = it.getString(ARG_STORE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentRecieptBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        receiptViewModel = ViewModelProvider(requireActivity()).get(ReceiptViewModel::class.java)
        receiptViewModel.getReceipt().observe(viewLifecycleOwner){ transaction ->
            for (itemPurchased in transaction.transactionItems!!){
                addView(itemPurchased)
            }
            itemPurchasedList.addAll(transaction.transactionItems)
            binding.textDate.text = dateFormat(transaction.transactionTimestamp!!)
        }
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            if (saveImage(System.currentTimeMillis().toString(),
                    getScreenShotFromView(requireView().rootView)!!
                )){
                val snack = Snackbar.make(requireView(),"A screenshot was taken",Snackbar.LENGTH_LONG)
                snack.setAction("Share") {
                    shareImageNow()
                }
                snack.show()
            }
        }, 1000)
    }
    private fun addView(itemPurchased: ItemPurchased) {
        val receiptView: View = layoutInflater.inflate(R.layout.row_receipt, null, false)
        val textName = receiptView.findViewById<TextView>(R.id.textItemPurchasedName)
        val textQuantity = receiptView.findViewById<TextView>(R.id.itemPurcasedQuantity)
        val textPrice = receiptView.findViewById<TextView>(R.id.itemPurchasedPrice)
        textName.text = itemPurchased.itemPurchasedName
        textQuantity.text = itemPurchased.itemPurchasedQuantity.toString()
        textPrice.text = itemPurchased.itemPurchasedPrice.toString()
        binding.layoutReceipt.addView(receiptView)
    }
    private fun getScreenShotFromView(v: View): Bitmap? {
        // create a bitmap object
        var screenshot: Bitmap? = null
        try {

            screenshot = Bitmap.createBitmap(v.measuredWidth, v.measuredHeight, Bitmap.Config.ARGB_8888)
            // Now draw this bitmap on a canvas
            val canvas = Canvas(screenshot)
            v.draw(canvas)

        } catch (e: Exception) {
            Snackbar.make(requireView(),"Failed", Snackbar.LENGTH_SHORT).show()
            Log.e("GFG", "Failed to capture screenshot because:" + e.message)
        }
        // return the bitmap
        return screenshot
    }
    private fun sdkCheck() : Boolean{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            return true
        }
        return false
    }
    private fun saveImage(name : String,bitmap: Bitmap) : Boolean{
        val imageCollection : Uri = if (sdkCheck()){
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,"$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE,"images/jpeg")
            put(MediaStore.Images.Media.WIDTH,bitmap.width)
            put(MediaStore.Images.Media.HEIGHT,bitmap.height)
        }
        return try {
            requireActivity().contentResolver.insert(imageCollection,contentValues)?.also {
                requireActivity().contentResolver.openOutputStream(it).use { outputStream->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG,95,outputStream)) {
                        throw IOException("Failed to save bitmap")
                    }
                }
                shareImage.setImageUri(it)
            }?: throw IOException("Failed to create Media Entry")
            true
        } catch(e: IOException) {
            e.printStackTrace()
            false
        }
    }
    private fun dateFormat(timestamp: Long): String {
        val simpleDateFormat = SimpleDateFormat("MM/dd/yyyy hh-mm-ss a")
        return simpleDateFormat.format(timestamp)
    }
    fun shareImageNow(){
        val intent = Intent(Intent.ACTION_SEND)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra(Intent.EXTRA_STREAM,shareImage.getImageUri())
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        intent.type = "image/jpg"
        startActivity(Intent.createChooser(intent,"Share image via"))
    }
    companion object {
        @JvmStatic
        fun newInstance(
            param1: String,
            param2: String,
            param3: String,
            param4: String,
            param5: String
        ) =
            ReceiptFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TOTAL, param1)
                    putString(ARG_CASH, param2)
                    putString(ARG_CHANGE, param3)
                    putString(ARG_CASHIER, param4)
                    putString(ARG_STORE, param5)
                }
            }
    }
}
class ShareImage {
    var uri : Uri? = null
    fun setImageUri(uri: Uri){
        this.uri = uri
    }
    fun getImageUri(): Uri? {
        return uri
    }
}