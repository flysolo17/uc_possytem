package com.flysolo.cashregister.navdrawer

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.flysolo.cashregister.R
import com.flysolo.cashregister.databinding.LogOutDialogBinding
import com.flysolo.cashregister.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class LogOutDialog : DialogFragment() {
   private lateinit var binding : LogOutDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = LogOutDialogBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(requireContext(),"Log Out Successful",Toast.LENGTH_SHORT).show()
            startActivity(Intent(activity,LoginActivity::class.java))
        }
    }
}