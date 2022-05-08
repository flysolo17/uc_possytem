package com.flysolo.cashregister.mystore

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.flysolo.cashregister.R
import com.flysolo.cashregister.databinding.ActivityMyStoreBinding

class MyStoreActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMyStoreBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navController = findNavController(R.id.fragmentContainerView)
        binding.bottomNavigationView.setupWithNavController(navController)
    }


}