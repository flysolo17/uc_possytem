package com.flysolo.cashregister.login

import com.google.android.material.textfield.TextInputLayout

class Validation {
    private var emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
     fun validateCard(textInputLayout: TextInputLayout) : Boolean {
        val email = textInputLayout.editText?.text.toString()
         return if (email.isEmpty()){
             textInputLayout.error = "Invalid Email"
             false
         } else if (!email.trim { it <= ' ' }.matches(emailPattern.toRegex())){
             textInputLayout.error = "Invalid Email"
             false
         }
         else {
             textInputLayout.error = ""
             true
         }
    }
    fun validatePassword(textInputLayout: TextInputLayout) : Boolean {
        val password = textInputLayout.editText?.text.toString()
        return when {
            password.isEmpty() -> {
                textInputLayout.error = "Invalid Password"
                false
            }
            password.length < 7 -> {
                textInputLayout.error = "Password too short"
                false
            }
            else -> {
                textInputLayout.error = ""
                true
            }
        }
    }
}