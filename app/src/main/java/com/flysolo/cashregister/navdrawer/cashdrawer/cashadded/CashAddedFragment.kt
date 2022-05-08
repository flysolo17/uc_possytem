package com.flysolo.cashregister.navdrawer.cashdrawer.cashadded

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.flysolo.cashregister.R
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.databinding.FragmentCashAddedBinding
import com.flysolo.cashregister.firebase.models.CashDrawer
import com.flysolo.cashregister.firebase.models.User
import com.flysolo.cashregister.navdrawer.cashdrawer.viewmodel.CashDrawerViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class CashAddedFragment : DialogFragment() {
    private lateinit var binding : FragmentCashAddedBinding
    private lateinit var cashDrawerViewModel: CashDrawerViewModel
    private var listCashAdded : MutableList<Int>? =null
    private lateinit var arrayAdapter: ArrayAdapter<*>
    private val firestore =  FirebaseFirestore.getInstance()
    private var cashDrawer : CashDrawer? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCashAddedBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listCashAdded = mutableListOf()
        cashDrawerViewModel = ViewModelProvider(requireActivity()).get(CashDrawerViewModel::class.java)
        cashDrawerViewModel.getCashDrawer().observe(viewLifecycleOwner, { cashDrawer ->
            if (cashDrawer != null){
                this.cashDrawer = cashDrawer
                for (cashAdded in cashDrawer.cashAdded){
                    listCashAdded!!.add(cashAdded)
                    arrayAdapter.notifyDataSetChanged()
                }
            }
        })
        arrayAdapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_expandable_list_item_1, listCashAdded!!)
        binding.listCashAdded.adapter = arrayAdapter
        binding.buttonAddCash.setOnClickListener {
            val cash = binding.edtCashAdded.editText?.text.toString()
            if (cash.isEmpty()){
                binding.edtCashAdded.error = "Input cash!"
            } else {
                addCashToTheDrawer(Integer.parseInt(cash))
            }
        }
    }
    private fun addCashToTheDrawer(cash : Int){
        firestore.collection(User.TABLE_NAME).document(CashierLoginActivity.userID!!)
            .collection(CashDrawer.TABLE_NAME)
            .document(cashDrawer?.cashDrawerID!!)
            .update(CashDrawer.CASH_ADDED,FieldValue.arrayUnion(cash))
            .addOnCompleteListener {
                if (it.isSuccessful){
                    Toast.makeText(requireContext(),"Successfully Added!",Toast.LENGTH_SHORT).show()
                    binding.edtCashAdded.editText?.setText("")
                    listCashAdded?.add(cash)
                    arrayAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(requireContext(),"Failed to add cash",Toast.LENGTH_SHORT).show()
                }
            }
    }

}