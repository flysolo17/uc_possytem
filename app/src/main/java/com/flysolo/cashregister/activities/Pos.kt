package com.flysolo.cashregister.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ThumbnailUtils
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flysolo.cashregister.R
import com.flysolo.cashregister.adapter.ItemAdapter
import com.flysolo.cashregister.adapter.ItemPurchasedAdapter
import com.flysolo.cashregister.databinding.ActivityPosBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.models.*
import com.flysolo.cashregister.fragments.ReceiptFragment
import com.flysolo.cashregister.login.LoginActivity
import com.flysolo.cashregister.ml.ModelUnquant
import com.flysolo.cashregister.viewmodels.ReceiptViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.BeepManager
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.squareup.picasso.Picasso
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


class Pos : AppCompatActivity() , ItemAdapter.OnItemIsClick, View.OnClickListener{
    private lateinit var binding : ActivityPosBinding
    private lateinit var dialog : BottomSheetDialog
    private lateinit var itemList: MutableList<Items>
    private lateinit var itemPurchasedList: MutableList<ItemPurchased>
    private lateinit var firestore: FirebaseFirestore
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var itemPurchasedAdapter: ItemPurchasedAdapter
    private var itemQuantity = 1
    private lateinit var bottom_pay_now : View
    private lateinit var button_20 : MaterialCardView
    private lateinit var button_50 : MaterialCardView
    private lateinit var button_100 : MaterialCardView
    private  lateinit var button_200 : MaterialCardView
    private lateinit var button_500 : MaterialCardView
    private  lateinit var button_1000 : MaterialCardView
    private lateinit var button_clear : MaterialCardView
    private lateinit var input_custom_cash : EditText
    private lateinit var text_cash_recieved : TextView
    private lateinit var text_cash_change : TextView
    private lateinit var button_pay_now : Button
    private lateinit var firebaseQueries : FirebaseQueries

    private var cashierName : String? = null
    private var store : String? = null
    private lateinit var receiptViewModel: ReceiptViewModel

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var list : List<String>
    private var imageSize = 224
    private var itemResultBitmap : Bitmap?  = null



    private fun _init(uid: String) {
        val fileName="labels.txt"
        val inputString= this.assets.open(fileName).bufferedReader().use { it.readText() }
        list=inputString.split("\n")
        firestore = FirebaseFirestore.getInstance()
        dialog = BottomSheetDialog(this)
        itemList = mutableListOf()
        itemPurchasedList = mutableListOf()
        getAllItems(uid)
        itemAdapter = ItemAdapter(this,itemList,this)
        itemPurchasedAdapter = ItemPurchasedAdapter(this,itemPurchasedList)
        firebaseQueries = FirebaseQueries(this,firestore)
        binding.recyclerviewItemPurchased.apply {
            val manager =LinearLayoutManager(this@Pos)
            manager.reverseLayout = true
            manager.stackFromEnd = true
            layoutManager = manager
            adapter = itemPurchasedAdapter
            addItemDecoration(
                DividerItemDecoration(this@Pos,
                    DividerItemDecoration.VERTICAL)
            )
        }
        swipeToDelete(binding.recyclerviewItemPurchased)
        bottom_pay_now = layoutInflater.inflate(R.layout.bottom_sheet_pay,null,false)
        button_20 = bottom_pay_now.findViewById(R.id.card_button_cash_20)
        button_50 = bottom_pay_now.findViewById(R.id.card_button_cash_50)
        button_100 = bottom_pay_now.findViewById(R.id.card_button_cash_100)

        button_200 = bottom_pay_now.findViewById(R.id.card_button_cash_200)
        button_500 = bottom_pay_now.findViewById(R.id.card_button_cash_500)
        button_1000 = bottom_pay_now.findViewById(R.id.card_button_cash_1000)
        button_clear = bottom_pay_now.findViewById(R.id.card_button_clear_cash)
        input_custom_cash = bottom_pay_now.findViewById(R.id.input_custom_cash)
        text_cash_change = bottom_pay_now.findViewById(R.id.text_cash_change)
        text_cash_recieved = bottom_pay_now.findViewById(R.id.text_cash_received)
        button_pay_now = bottom_pay_now.findViewById(R.id.button_pay_now)

        receiptViewModel = ViewModelProvider(this).get(ReceiptViewModel::class.java)



    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cashierName = intent.getStringExtra(Cashier.CASHIER_NAME)
        store = intent.getStringExtra(User.BUSINESS_NAME)
        binding.textCashier.text = cashierName
        val uid = intent.getStringExtra(User.USER_ID)
        _init(uid!!)
        binding.buttonShowItems.setOnClickListener {
            viewItems()
        }
        binding.buttonBack.setOnClickListener {
            finish()
        }
        binding.buttonPayNow.setOnClickListener {
            bottomSheetPayNow()
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.data != null) {
                var bitmap = result.data!!.extras!!.get("data") as Bitmap?
                itemResultBitmap = bitmap
                if (bitmap != null) {
                    val dimension: Int = Math.min(bitmap.getWidth(), bitmap.getHeight())
                    bitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension)
                    bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false)
                    classifyImage(bitmap)
                }
            }
        }

        binding.buttonDetectItem.setOnClickListener {
            launchCamera(cameraLauncher)
        }
    }
    @SuppressLint("QueryPermissionsNeeded")
    private fun launchCamera(launcher: ActivityResultLauncher<Intent>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                launcher.launch(intent)
            }

        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            launcher.launch(intent)
        }
    }
    private fun classifyImage(image: Bitmap){
        try {
            val model = ModelUnquant.newInstance(this)
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
                getScanResult(list[maxPos])

            }else {
                Toast.makeText(this,"Item not available",Toast.LENGTH_SHORT).show()
            }
            model.close()
        } catch (e: IOException) {
            Log.d(".BarcodeModeFragment", e.message.toString())
        }
    }
    private fun getScanResult(result: String) {
       firestore.collection(User.TABLE_NAME)
           .document(LoginActivity.uid)
           .collection(Items.TABLE_NAME)
           .document(result)
           .get()
           .addOnSuccessListener { document ->
               if (document.exists()) {
                   val items = document.toObject(Items::class.java)
                   if (items!= null){
                       showScannedItem(items)
                   }
               } else {
                   Toast.makeText(this,"item not available in the inventory",Toast.LENGTH_SHORT).show()
               }
           }
    }
    private fun showScannedItem(items: Items) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_show_item)
        dialog.setTitle("Scanned Item")
        val itemImage : ImageView = dialog.findViewById(R.id.itemImage)
        val itemName : TextView = dialog.findViewById(R.id.textItemName)
        val itemPrice : TextView = dialog.findViewById(R.id.textItemPrice)
        val itemBarcode : TextView = dialog.findViewById(R.id.textItemBarcode)
        val itemCategory : TextView = dialog.findViewById(R.id.textItemCategory)
        if (items.itemImageURL!!.isNotEmpty()) {
            Picasso.get().load(items.itemImageURL).placeholder(R.drawable.store).into(itemImage)
        }
        itemName.text = items.itemName
        itemPrice.text = items.itemPrice.toString()
        itemBarcode.text = items.itemBarcode
        itemCategory.text = items.itemCategory
        val textQuantity : TextView = dialog.findViewById(R.id.text_quantity)
        val buttonIncrement : ImageView = dialog.findViewById(R.id.image_button_increment)
        val buttonDecrement : ImageView = dialog.findViewById(R.id.image_button_decrement)
        //button Actions
        buttonIncrement.setOnClickListener {
            textQuantity.text = incrementQuantity().toString()
        }
        buttonDecrement.setOnClickListener {
            textQuantity.text = decrementQuantity().toString()
        }
        val buttonCancel : Button = dialog.findViewById(R.id.buttonCancel)
        val buttonPurchase : Button = dialog.findViewById(R.id.buttonPurchase)
        buttonCancel.setOnClickListener{
            dialog.dismiss()
        }
        buttonPurchase.setOnClickListener {
            val total = Integer.parseInt(textQuantity.text.toString()) * items.itemPrice!!
            itemPurchasedList.add(ItemPurchased(items.itemBarcode,
                items.itemName,
                Integer.parseInt(textQuantity.text.toString()),
                items.itemCost,
                total,
                false))
            itemQuantity = 1
            binding.textItemCounter.text = refreshItemCounter().toString()
            binding.textSubTotal.text = refreshSubtotalTotalAmount().toString()
            itemPurchasedAdapter.notifyDataSetChanged()
            dialog.dismiss()
        }
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    private fun bottomSheetPayNow(){
        button_20.setOnClickListener(this)
        button_50.setOnClickListener(this)
        button_100.setOnClickListener(this)
        button_200.setOnClickListener(this)
        button_500.setOnClickListener(this)
        button_1000.setOnClickListener(this)
        button_clear.setOnClickListener(this)
        input_custom_cash.addTextChangedListener(cashReceivedTextWatcher)
        binding.textSubTotal.addTextChangedListener(paymentTextWatcher)
        text_cash_recieved.addTextChangedListener(paymentTextWatcher)
        button_pay_now.setOnClickListener {
            val transaction = Transaction(firebaseQueries.generateID("Transaction"),
                cashierName,
                System.currentTimeMillis(),
                itemPurchasedList)
            for (items in itemPurchasedList){
                firebaseQueries.decreaseQuantity(items.itemPurchasedID!!, items.itemPurchasedQuantity!!.toLong())
            }
            firebaseQueries.createTransaction(transaction)
            receiptViewModel.setReceipt(transaction)
            showReceipt(binding.textSubTotal.text.toString(),
                text_cash_recieved.text.toString(),
            text_cash_change.text.toString(),
                cashierName!!,
                store!!
            )
        }
        if (!dialog.isShowing) {
            dialog.setContentView(bottom_pay_now)
            dialog.show()
        }
    }
    private fun showReceipt(total : String,cash : String,change : String,cashier: String,store : String) {
        val receipt = ReceiptFragment.newInstance(total,cash,change,cashier,store)
        if (!receipt.isAdded) {
            receipt.show(supportFragmentManager,"Receipt")
        }
    }
    //bottomsheet itemlist
    private fun viewItems() {
        val view : View = layoutInflater.inflate(R.layout.bottomsheet_show_items,null,false)
        val recyclerViewItems : RecyclerView = view.findViewById(R.id.recyclerviewItems)
        val buttonIncrement : ImageView = view.findViewById(R.id.image_button_increment)
        val buttonDecrement : ImageView = view.findViewById(R.id.image_button_decrement)
        val textQuantity : TextView = view.findViewById(R.id.text_quantity)

        recyclerViewItems.apply {
            layoutManager  = LinearLayoutManager(this@Pos)
            adapter = itemAdapter
            addItemDecoration(
                DividerItemDecoration(this@Pos,
                    DividerItemDecoration.VERTICAL)
            )
        }
        //button Actions
        buttonIncrement.setOnClickListener {
            textQuantity.text = incrementQuantity().toString()
        }
        buttonDecrement.setOnClickListener {
            textQuantity.text = decrementQuantity().toString()
        }
        if (!dialog.isShowing) {
            dialog.setContentView(view)
            dialog.show()
        }
    }
    private fun getAllItems(uid: String){
        firestore.collection(User.TABLE_NAME)
            .document(uid)
            .collection(Items.TABLE_NAME)
            .get().addOnCompleteListener {
                if (it.isSuccessful){
                    for (document in it.result) {
                        val items = document.toObject(Items::class.java)
                        itemList.add(items)
                    }
                }
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun itemClick(position: Int) {
        val item = itemList[position]
        val quantity  = dialog.findViewById<TextView>(R.id.text_quantity)
        val total = Integer.parseInt(quantity?.text.toString()) * item.itemPrice!!

        itemPurchasedList.add(ItemPurchased(item.itemBarcode,
            item.itemName,
            Integer.parseInt(quantity?.text.toString()),
            item.itemCost,
            total,
            false))
        itemQuantity = 1
        quantity?.text  = itemQuantity.toString()
        binding.textItemCounter.text = refreshItemCounter().toString()
        binding.textSubTotal.text = refreshSubtotalTotalAmount().toString()
        itemPurchasedAdapter.notifyDataSetChanged()
    }
    // TODO: Increment Quantity
    private fun incrementQuantity(): Int {
        itemQuantity += 1
        return itemQuantity
    }

    // TODO: Decrement Quantity
    private fun decrementQuantity(): Int {
        if (itemQuantity > 1) {
            itemQuantity -= 1
            return itemQuantity
        } else Toast.makeText(this, "minimum item is 1", Toast.LENGTH_SHORT).show()
        return itemQuantity
    }
    private fun swipeToDelete(recyclerView: RecyclerView?) {
        val callback =
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val pos = viewHolder.bindingAdapterPosition
                    itemPurchasedList.removeAt(pos)
                    itemPurchasedAdapter.notifyItemRemoved(pos)
                    binding.textItemCounter.text = refreshItemCounter().toString()
                    binding.textSubTotal.text = refreshSubtotalTotalAmount().toString()

                }
            })
        callback.attachToRecyclerView(recyclerView)
    }

    //TODO: get total transaction cost
    private fun transactionCost(list : List<ItemPurchased>): Int {
        var purchaseCost = 0
        for (cost in list) {
            purchaseCost += cost.itemPurchasedCost!!
        }
        return purchaseCost
    }
    private fun underDevelopmentDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.development_dialog)
        dialog.setTitle("Under Development")
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    //TODO: count the items
    private fun refreshItemCounter(): Int {
        var counter = 0
        if (itemPurchasedList.isNotEmpty()) {
            for (i in itemPurchasedList.indices) {
                counter += itemPurchasedList[i].itemPurchasedQuantity!!
            }
        }
        return counter
    }
    //TODO: refresh the total amount of the order
    private fun refreshSubtotalTotalAmount(): Int {
        var purchasesSubtotal = 0
        if (itemPurchasedList.isNotEmpty()) {
            for (i in itemPurchasedList.indices) {
                purchasesSubtotal += itemPurchasedList[i].itemPurchasedPrice!!
            }
        }
        return purchasesSubtotal
    }
    @SuppressLint("SetTextI18n")
    override fun onClick(v: View?) {

        when (v!!.id) {
            R.id.card_button_cash_20 -> {
                input_custom_cash.setText("")
                text_cash_recieved.text = "20"
            }
            R.id.card_button_cash_50 -> {
                input_custom_cash.setText("")
                text_cash_recieved.text = "50"
            }
            R.id.card_button_cash_100 -> {
                input_custom_cash.setText("")
                text_cash_recieved.text = "100"
            }
            R.id.card_button_cash_200 -> {
                input_custom_cash.setText("")
                text_cash_recieved.text = "200"
            }
            R.id.card_button_cash_500 -> {
                input_custom_cash.setText("")
               text_cash_recieved.text = "500"
            }
            R.id.card_button_cash_1000 -> {
                input_custom_cash.setText("")
                text_cash_recieved.text = "1000"
            }
            R.id.card_button_clear_cash -> {
                input_custom_cash.setText("")
                text_cash_recieved.text = "0"
            }
            else -> {
                input_custom_cash.setText("")
                text_cash_recieved.text = "0"
            }
        }
    }
    //TextWatchers
    private val paymentTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            val subtotal: Int = binding.textSubTotal.text.toString().toInt()
            val cashReceived: Int = text_cash_recieved.text.toString().toInt()
            if (cashReceived > subtotal) {
                val total = cashReceived - subtotal
                text_cash_change.text = total.toString()
            } else text_cash_change.text = 0.toString()
        }

        override fun afterTextChanged(s: Editable) {}
    }

    private val cashReceivedTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            try {
                if (s.toString().isNotEmpty()) {
                    text_cash_recieved.text = s.toString()
                    val cash = s.toString().toInt()
                    if (cash > 100000) {
                        s.replace(0, s.length, "0")
                    }
                } else {
                    text_cash_recieved.text = 0.toString()
                }
            } catch (ignored: NumberFormatException) {
            }
        }
    }
}