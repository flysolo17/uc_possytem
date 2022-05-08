package com.flysolo.cashregister.purchases

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.ViewModelProvider

import com.flysolo.cashregister.cashierlogin.CashierLoginActivity

import android.widget.TextView
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build


import android.util.Log

import com.google.android.material.snackbar.Snackbar


import android.net.Uri
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import com.flysolo.cashregister.databinding.FragmentReceiptBinding
import java.io.*
import java.text.SimpleDateFormat

class ReceiptFragment : Fragment() {

    private lateinit var binding: FragmentReceiptBinding
    private lateinit var receiptViewModel: ReceiptViewModel
    private lateinit var itemPurchasedList : MutableList<ItemPurchased>
    private val shareImage = ShareImage()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentReceiptBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemPurchasedList = mutableListOf()
        binding.textStoreName.text = CashierLoginActivity.storeName
        receiptViewModel = ViewModelProvider(requireActivity()).get(ReceiptViewModel::class.java)
        receiptViewModel.getReceipt().observe(viewLifecycleOwner){ transaction ->
            for (itemPurchased in transaction.transactionItems!!){
                addView(itemPurchased)
            }
            binding.textCashierName.text = transaction.transactionCashier
            itemPurchasedList.addAll(transaction.transactionItems)
            binding.transactionTimestamp.text = dateFormat(transaction.transactionTimestamp!!)
            binding.textTotal.text  = computeTotalSales(transaction.transactionItems).toString()
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
        val receiptView: View = layoutInflater.inflate(com.flysolo.cashregister.R.layout.row_receipt, null, false)
        val textName = receiptView.findViewById<TextView>(com.flysolo.cashregister.R.id.textItemPurchasedName)
        val textQuantity = receiptView.findViewById<TextView>(com.flysolo.cashregister.R.id.itemPurcasedQuantity)
        val textPrice = receiptView.findViewById<TextView>(com.flysolo.cashregister.R.id.itemPurchasedPrice)
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
            Snackbar.make(requireView(),"Failed",Snackbar.LENGTH_SHORT).show()
            Log.e("GFG", "Failed to capture screenshot because:" + e.message)
        }
        // return the bitmap
        return screenshot
    }
   private fun sdkCheck() : Boolean{
       if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.Q){
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
        val simpleDateFormat = SimpleDateFormat("EEEE, dd-MMM-yyyy hh-mm-ss a")
        return simpleDateFormat.format(timestamp)
    }
    private fun computeTotalSales(items : List<ItemPurchased>): Int {
        var total = 0
        for (price in items){
            total += price.itemPurchasedPrice!!
        }
        return total
    }
    fun shareImageNow(){
        val intent = Intent(Intent.ACTION_SEND)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra(Intent.EXTRA_STREAM,shareImage.getImageUri())
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        intent.type = "image/jpg"
        startActivity(Intent.createChooser(intent,"Share image via"))
    }
}
class ShareImage {
    var uri :Uri? = null
    fun setImageUri(uri: Uri){
        this.uri = uri
    }
    fun getImageUri(): Uri? {
        return uri
    }
}