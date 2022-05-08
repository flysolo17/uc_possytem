package com.flysolo.cashregister.dialogs

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.flysolo.cashregister.MainActivity
import com.flysolo.cashregister.R
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.databinding.CashierPinDialogBinding
import com.flysolo.cashregister.mystore.MyStoreActivity
import java.lang.NumberFormatException
private const val ARG_ID = "param0"
private const val ARG_PIN = "param1"
private const val ARG_CASHIER = "param2"

class CashierPinDialog : DialogFragment() {
    // TODO: Rename and change types of parameters
    private var cashierID: String? = null
    private var cashierPin: String? = null
    private var cashierName: String? = null
    private lateinit var binding : CashierPinDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            cashierID = it.getString(ARG_ID)
            cashierPin = it.getString(ARG_PIN)
            cashierName= it.getString(ARG_CASHIER)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = CashierPinDialogBinding.inflate(layoutInflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cashierPin.addTextChangedListener(cashierPinWatcher)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CashierPinDialog.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(cashierID : String,cashierPin: String,cashierName : String) =
            CashierPinDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_ID, cashierID)
                    putString(ARG_PIN, cashierPin)
                    putString(ARG_CASHIER,cashierName)
                }
            }
    }
    private val cashierPinWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            try {
                if (s.toString().length == 4) {
                    if (s.toString() == cashierPin){
                        startActivity(Intent(requireActivity(), MainActivity::class.java).putExtra(ARG_CASHIER,cashierName).putExtra(
                            ARG_ID,cashierID))
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