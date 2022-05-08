package com.flysolo.cashregister.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.flysolo.cashregister.dialogs.ProgressDialog
import com.flysolo.cashregister.databinding.ActivityCreateAccountBinding
import com.flysolo.cashregister.firebase.FirebaseQueries
import com.flysolo.cashregister.firebase.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseUser




class CreateAccountActivity : AppCompatActivity() {
    private lateinit var binding : ActivityCreateAccountBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var firebaseQueries : FirebaseQueries? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        progressDialog =  ProgressDialog(this)
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseQueries = FirebaseQueries(this, FirebaseFirestore.getInstance())
        binding.buttonCreateAccount.setOnClickListener {
            validateAccountInfo()
        }
        binding.buttonHaveAccount.setOnClickListener{
            startActivity(Intent(this,LoginActivity::class.java))
        }
    }

    private fun validateAccountInfo(){
        val firstname : String = binding.inputName.editText?.text.toString()
        val lastname : String = binding.inputLastName.editText?.text.toString()
        val businessName : String = binding.inputBusinessName.editText?.text.toString()
        val phoneNumber : String = binding.inputPhone.editText?.text.toString()
        val email : String = binding.inputEmail.editText?.text.toString()
        val password : String = binding.inputPassword.editText?.text.toString()
        val confirmPassword : String = binding.inputConfirmPassword.editText?.text.toString()
        when {
            firstname.isEmpty() -> {
                binding.inputName.error = "Input firstname"
            }
            lastname.isEmpty() -> {
                binding.inputLastName.error = "Input lastname"
            }
            businessName.isEmpty() -> {
                binding.inputBusinessName.error = "Input business name"
            }
            phoneNumber.isEmpty() -> {
                binding.inputPhone.error = "Input phone number"
            }
            !phoneNumber.startsWith('9') -> {
                binding.inputPhone.error = "Invalid phone number"
            }
            email.isEmpty() -> {
                binding.inputEmail.error = "Input email"
            }
            password.isEmpty() -> {
                binding.inputPassword.error = "Input password"
            }
            password.length < 6 -> {
                binding.inputPassword.error = "password must contain 7 characters"
            }
            confirmPassword.isEmpty() -> {
                binding.inputConfirmPassword.error = "Input password"
            }
            confirmPassword != password -> {
                binding.inputConfirmPassword.error = "password not equal"
            }
            else -> {
                Toast.makeText(this,"Success",Toast.LENGTH_SHORT).show()

                createAccount(firstname,lastname,businessName,phoneNumber,email,password)
            }
        }
    }

    private fun createAccount(firstname : String,lastname: String,businessName : String,phone : String,email : String,password : String) {
        progressDialog.loading()
        firebaseAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener{
            if (it.isSuccessful) {
                val userID: FirebaseUser = firebaseAuth.currentUser!!
                val user = User(userID.uid,
                    "",
                    firstname,
                    lastname,
                    businessName,
                    "000000",
                    phone,
                    email)

                firebaseQueries!!.createAccount(user)
                progressDialog.stopLoading()
                startActivity(Intent(this,LoginActivity::class.java))
            } else {
                Toast.makeText(this,"Failed!",Toast.LENGTH_SHORT).show()
                progressDialog.stopLoading()
            }
        }.addOnFailureListener{
            progressDialog.stopLoading()
        }
    }
}