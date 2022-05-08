package com.flysolo.cashregister

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.databinding.ActivityMainBinding
import com.google.firebase.firestore.FirebaseFirestore



class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mAppBarConfiguration : AppBarConfiguration
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var  textOwnerName : TextView
    private lateinit var textBusinessName : TextView
    private lateinit var headerView: View
    private lateinit var firestore: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        setSupportActionBar(toolbar)
        firestore = FirebaseFirestore.getInstance()
        setUpHeaderViews()
        cashierName = intent.getStringExtra("param2")!!
        cashierID = intent.getStringExtra("param0")!!
        mAppBarConfiguration = AppBarConfiguration.Builder(R.id.nav_home, R.id.nav_cashier_cash_drawer,R.id.nav_inventory,R.id.nav_attendance, R.id.nav_logout)
            .setOpenableLayout(binding.drawerLayout).build()
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration)
        NavigationUI.setupWithNavController(binding.navView, navController)

    }
    override fun onSupportNavigateUp(): Boolean {
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        return (NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp())
    }


    private fun initView() {
        //App bar main layout
        toolbar = findViewById(R.id.toolbar)
        headerView = binding.navView.getHeaderView(0)
        textOwnerName = headerView.findViewById(R.id.textOwnerName)
        textBusinessName = headerView.findViewById(R.id.textBusinessName)

    }


    private fun setUpHeaderViews() {

        textOwnerName.text = CashierLoginActivity.storeOwnerName
        textBusinessName.text = CashierLoginActivity.storeName
    }

    companion object {
         var cashierName = ""
        var cashierID = ""
    }
}