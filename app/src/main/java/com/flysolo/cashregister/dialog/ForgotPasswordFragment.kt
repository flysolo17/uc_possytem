package com.flysolo.cashregister.dialog

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.flysolo.cashregister.R
import com.flysolo.cashregister.databinding.FragmentForgotPasswordBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth


class ForgotPasswordFragment : DialogFragment() {
    private var binding : FragmentForgotPasswordBinding? =null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL,
            android.R.style.Theme_Light_NoTitleBar_Fullscreen)

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentForgotPasswordBinding.inflate(inflater,container,false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.buttonResetPassword.setOnClickListener {
            val email = binding!!.inputEmail.editText?.text.toString()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(view.context,"Invalid Email", Toast.LENGTH_SHORT).show()
            }
            forgotPassword(email)
        }
    }
    private fun forgotPassword(email: String) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task: Task<Void?> ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        binding!!.root.context,
                        "Check your email to reset your password",
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                } else {
                    Toast.makeText(
                        binding!!.root.context,
                        "Try again! something wrong happened!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

    }

}