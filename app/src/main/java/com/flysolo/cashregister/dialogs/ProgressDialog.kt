package com.flysolo.cashregister.dialogs
import android.app.Activity
import com.flysolo.cashregister.R
import com.flysolo.cashregister.mystore.bottomnav.cashier.CashierFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProgressDialog(private val activity: Activity) {
    private lateinit var alertDialog: androidx.appcompat.app.AlertDialog
     fun loading(){
        val materialAlertDialogBuilder = MaterialAlertDialogBuilder(activity)
        val layoutInflater = activity.layoutInflater
        materialAlertDialogBuilder.setView(layoutInflater.inflate(R.layout.progress_dialog,null))
        materialAlertDialogBuilder.setCancelable(false)
        alertDialog = materialAlertDialogBuilder.create()
        alertDialog.show()
    }
     fun stopLoading(){
        alertDialog.dismiss()
    }
}