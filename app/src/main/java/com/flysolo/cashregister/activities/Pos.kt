package com.flysolo.cashregister.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flysolo.cashregister.MainActivity
import com.flysolo.cashregister.R
import com.flysolo.cashregister.databinding.ActivityPosBinding
import com.flysolo.cashregister.login.LoginActivity
import com.flysolo.cashregister.adapter.ItemAdapter
import com.flysolo.cashregister.adapter.ItemPurchasedAdapter
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.models.*
import com.flysolo.cashregister.firebase.models.Transaction
import com.flysolo.cashregister.fragments.ReceiptFragment
import com.flysolo.cashregister.viewmodels.ReceiptViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import org.w3c.dom.Text
import java.lang.NumberFormatException

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
    private fun _init(uid: String) {
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
            val transaction = Transaction(firebaseQueries.generateID(Transaction.TABLE_NAME),
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