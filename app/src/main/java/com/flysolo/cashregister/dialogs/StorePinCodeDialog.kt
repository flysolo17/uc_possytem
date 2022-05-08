package com.flysolo.cashregister.dialogs

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity

import com.flysolo.cashregister.databinding.FragmentStorePinCodeDialogBinding
import com.flysolo.cashregister.mystore.MyStoreActivity
import java.lang.NumberFormatException


class StorePinCodeDialog : DialogFragment() {

    private lateinit var binding : FragmentStorePinCodeDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStorePinCodeDialogBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.storePin.addTextChangedListener(storePinWatcher)

    }
    private val storePinWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            try {
                if (s.toString().length == 6) {
                    if (s.toString() == CashierLoginActivity.storePinCode){
                        startActivity(Intent(activity, MyStoreActivity::class.java))
                        dismiss()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Invalid Pin",
                            Toast.LENGTH_SHORT
                        ).show()
                        s.clear()
                    }

                }
            } catch (ignored: NumberFormatException) {
            }
        }
    }
}