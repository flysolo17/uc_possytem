package com.flysolo.cashregister.login

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.flysolo.cashregister.MainActivity
import com.flysolo.cashregister.dialog.ProgressDialog
import com.flysolo.cashregister.databinding.ActivityLoginBinding
import com.flysolo.cashregister.firebase.models.User
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var validation: Validation

    private var progressDialog: ProgressDialog = ProgressDialog(this)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Camera Permission Denied!", Toast.LENGTH_SHORT).show()
            } else {
                checkPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                   READ_STORAGE_PERMISSION_CODE
                )
            }
        } else if (requestCode == READ_STORAGE_PERMISSION_CODE) {
            if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Read Storage Denied!", Toast.LENGTH_SHORT).show()
            } else {
                checkPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    WRITE_STORAGE_PERMISSION_CODE
                )
            }
        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        validation = Validation()
        firebaseFirestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
        binding.buttonLogin.setOnClickListener{
            if (!validation.validateCard(binding.inputEmail) || !validation.validatePassword(binding.inputPassword)){
                return@setOnClickListener
            } else {
                signInWithEmail(binding.inputEmail.editText?.text.toString(),binding.inputPassword.editText?.text.toString())

            }
        }
        binding.buttonSignUp.setOnClickListener{
            startActivity(Intent(this,CreateAccountActivity::class.java))
        }
    }

    private fun signInWithEmail(email : String, password : String){
        progressDialog.loading()

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val currentUser: FirebaseUser? = firebaseAuth.currentUser
                    progressDialog.stopLoading()
                    updateUI(currentUser)
                } else if (!it.isSuccessful) {
                    try {
                        throw Objects.requireNonNull<Exception>(it.exception)
                    } // if user enters wrong email.
                    catch (invalidEmail: FirebaseAuthInvalidUserException) {
                        progressDialog.stopLoading()
                        Log.d(LOGIN_ACTIVITY, "onComplete: invalid_email")
                        Toast.makeText(applicationContext, "Invalid Email", Toast.LENGTH_SHORT)
                            .show()

                    } // if user enters wrong password.
                    catch (wrongPassword: FirebaseAuthInvalidCredentialsException) {
                        Log.d(
                            LOGIN_ACTIVITY, "onComplete: wrong_password"
                        )
                        progressDialog.stopLoading()
                        Toast.makeText(applicationContext, "Wrong Password", Toast.LENGTH_SHORT)
                            .show()
                    } catch (e: Exception) {
                        progressDialog.stopLoading()
                        Log.d(LOGIN_ACTIVITY, "onComplete: " + e.message)
                    }
                } else {
                    progressDialog.stopLoading()
                    // If sign in fails, display a message to the user.
                    Log.w(LOGIN_ACTIVITY, "signInWithCredential:failure", it.exception)
                    Snackbar.make(binding.root, "Login Failed.", Snackbar.LENGTH_SHORT).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        var currentUser: FirebaseUser? = firebaseAuth.currentUser
        if (currentUser != null){
            uid = currentUser.uid
            progressDialog.loading()
            updateUI(currentUser)
            progressDialog.stopLoading()
        }

    }
    override fun onResume() {
        super.onResume()
        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE)
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null){
            uid = currentUser.uid
            val intent = Intent(this, MainActivity::class.java)
                intent.putExtra(User.USER_ID,currentUser.uid)
            startActivity(intent)
        }
    }
    private fun checkPermission(permission: String, requestCode: Int) {
        //checking if permission granted or not
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                requestCode
            )
        }
    }
    companion object {
        const val CAMERA_PERMISSION_CODE = 223
        const val READ_STORAGE_PERMISSION_CODE = 101
        const val WRITE_STORAGE_PERMISSION_CODE = 102
        const val LOGIN_ACTIVITY : String = ".LoginActivity"
         var uid = ""

    }
}