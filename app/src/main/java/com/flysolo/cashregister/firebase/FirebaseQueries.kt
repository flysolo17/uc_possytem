package com.flysolo.cashregister.firebase
import android.content.Context
import android.widget.Toast
import com.firebase.ui.firestore.FirestoreRecyclerOptions

import com.flysolo.cashregister.firebase.models.*
import com.flysolo.cashregister.login.LoginActivity
import com.flysolo.cashregister.firebase.models.ItemPurchased
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FirebaseQueries(val context: Context,val firebaseFirestore: FirebaseFirestore) {

    fun generateID(TABLE_NAME: String?): String {
        return firebaseFirestore
            .collection(User.TABLE_NAME)
            .document(LoginActivity.uid)
            .collection(TABLE_NAME!!).document().id
    }
    fun createAccount(user: User) {
        firebaseFirestore.collection(User.TABLE_NAME)
            .document(user.userId!!).set(user)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(context, "Account created successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Account created Failed!", Toast.LENGTH_SHORT).show()
                }
            }
    }

        fun addItem(item: Items) {
            firebaseFirestore.collection(User.TABLE_NAME)
                .document(LoginActivity.uid)
                .collection(Items.TABLE_NAME)
                .document(item.itemBarcode!!).set(item).addOnCompleteListener { task: Task<Void?> ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Item Added Successfully..", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(context, "Failed adding item", Toast.LENGTH_SHORT).show()
                    }
                }
        }







        fun addItemPurchased(itemPurchase: ItemPurchased) {
            firebaseFirestore.collection(User.TABLE_NAME)
                .document(LoginActivity.uid)
                .collection(ItemPurchased.TABLE_NAME)
                .document(itemPurchase.itemPurchasedID!!).set(itemPurchase)
                .addOnCompleteListener { task: Task<Void?> ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            context,
                            "You Purchased: ${itemPurchase.itemPurchasedName}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {

                        Toast.makeText(context, "Failed..", Toast.LENGTH_SHORT).show()
                    }
                }
        }


        fun getItemPurchased(itemPurchase: ItemPurchased) {
            firebaseFirestore.collection(User.TABLE_NAME)
                .document(LoginActivity.uid)
                .collection(ItemPurchased.TABLE_NAME)
                .document(itemPurchase.itemPurchasedID!!).set(itemPurchase)
                .addOnCompleteListener { task: Task<Void?> ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            context,
                            "You Purchased: ${itemPurchase.itemPurchasedName}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {

                        Toast.makeText(context, "Failed..", Toast.LENGTH_SHORT).show()
                    }
                }
        }

     fun getCashiers(userID: String): FirestoreRecyclerOptions<Cashier?> {
        val query: Query = firebaseFirestore
            .collection(User.TABLE_NAME)
            .document(userID)
            .collection(Cashier.TABLE_NAME)

        return FirestoreRecyclerOptions.Builder<Cashier>()
            .setQuery(query, Cashier::class.java)
            .build()
    }
    //Cashier
    fun createTransaction(transaction: Transaction) {
        firebaseFirestore.collection(User.TABLE_NAME).document(LoginActivity.uid)
            .collection(Transaction.TABLE_NAME)
            .document(transaction.transactionID!!)
            .set(transaction)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(context, "New Transaction Success", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed!", Toast.LENGTH_SHORT).show()
                }
            }
    }
    fun decreaseQuantity(itemID : String , itemQuantity : Long){
        firebaseFirestore.collection(User.TABLE_NAME)
            .document(LoginActivity.uid)
            .collection(Items.TABLE_NAME)
            .document(itemID).update(Items.ITEM_QUANTITY,FieldValue.increment(-itemQuantity))
    }
    fun getAllTransactions(startDay : Long,endDay : Long): FirestoreRecyclerOptions<Transaction?> {
        val query: Query = firebaseFirestore
            .collection(User.TABLE_NAME)
            .document(LoginActivity.uid)
            .collection(Transaction.TABLE_NAME)
            .whereGreaterThan(Transaction.TIMESTAMP,startDay)
            .whereLessThan(Transaction.TIMESTAMP,endDay)
            .orderBy(Transaction.TIMESTAMP,Query.Direction.DESCENDING)
        return FirestoreRecyclerOptions.Builder<Transaction>()
            .setQuery(query, Transaction::class.java)
            .build()
    }


    fun updateTransaction(transactionID : String,itemPurchasedList : List<ItemPurchased>) {
        firebaseFirestore.collection(User.TABLE_NAME).document(LoginActivity.uid)
            .collection(Transaction.TABLE_NAME)
            .document(transactionID)
            .update(Transaction.TRANSACTION_ITEMS,itemPurchasedList)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(context, "Transaction updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed!", Toast.LENGTH_SHORT).show()
                }
            }
    }
    //Cashier
    fun signInAttendance(attendance: Attendance) {
        firebaseFirestore.collection(User.TABLE_NAME)
            .document(LoginActivity.uid)
            .collection(Attendance.TABLE_NAME)
            .document(attendance.attendanceID!!)
            .set(attendance)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed!", Toast.LENGTH_SHORT).show()
                }
            }
    }
    //Cashier
    fun signOutAttendance(attendanceID : String,image : String,timestamp: Long) {
        firebaseFirestore.collection(User.TABLE_NAME)
            .document(LoginActivity.uid)
            .collection(Attendance.TABLE_NAME)
            .document(attendanceID).update(Attendance.TIMEOUT_IMAGE,image)
        firebaseFirestore.collection(User.TABLE_NAME)
            .document(LoginActivity.uid)
            .collection(Attendance.TABLE_NAME)
            .document(attendanceID).update(Attendance.TIMEOUT_TIMESTAMP,timestamp)

    }

    fun getAttendance(startDay: Long,endDay: Long): FirestoreRecyclerOptions<Attendance?> {
        val query: Query = firebaseFirestore
            .collection(User.TABLE_NAME)
            .document(LoginActivity.uid)
            .collection(Attendance.TABLE_NAME)
            .whereGreaterThan(Attendance.TIMEIN_TIMESTAMP,startDay)
            .whereLessThan(Attendance.TIMEIN_TIMESTAMP,endDay)
            .orderBy(Attendance.TIMEIN_TIMESTAMP,Query.Direction.ASCENDING)
        return FirestoreRecyclerOptions.Builder<Attendance>()
            .setQuery(query, Attendance::class.java)
            .build()
    }
    fun queryResult(itemName: String) : ItemPurchased {
        var itemPurchased : ItemPurchased? = null
        val query = firebaseFirestore
            .collection(User.TABLE_NAME)
            .document(LoginActivity.uid)
            .collection(Items.TABLE_NAME)
            .whereEqualTo(Items.ITEM_NAME,itemName)
        query.get().addOnCompleteListener {
            if (it.isSuccessful && it.result != null){
                for (items in it.result){
                    val item = items.toObject(Items::class.java)
                    itemPurchased = ItemPurchased(item.itemBarcode,
                    item.itemName,
                    1,
                    item.itemCost,
                        1 * item.itemCost!!,
                    false)
                }


            } else {
                Toast.makeText(context,"unidentified item",Toast.LENGTH_SHORT).show()
            }
        }
        return itemPurchased!!
    }
    fun updateProfile(uri: String){
        firebaseFirestore.collection(User.TABLE_NAME)
            .document(LoginActivity.uid)
            .update(User.STORE_IMAGE,uri)
            .addOnCompleteListener {
                if (it.isSuccessful){
                    Toast.makeText(context,"New Profile Updated",Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context,"Failed to update profile",Toast.LENGTH_SHORT).show()
                }
            }
    }
}