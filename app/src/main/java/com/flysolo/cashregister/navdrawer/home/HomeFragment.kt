package com.flysolo.cashregister.navdrawer.home

import android.os.Bundle
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.viewpager2.widget.ViewPager2
import com.flysolo.cashregister.MainActivity
import com.flysolo.cashregister.cashierlogin.CashierLoginActivity
import com.flysolo.cashregister.R

import com.flysolo.cashregister.databinding.FragmentHomeBinding
import com.flysolo.cashregister.dialogs.StorePinCodeDialog
import com.flysolo.cashregister.firebase.models.User

import com.flysolo.cashregister.navdrawer.home.adapter.ViewPagerAdapter
import com.flysolo.cashregister.navdrawer.home.viewmodels.PurchasingViewModel
import com.flysolo.cashregister.purchases.ItemPurchased
import com.flysolo.cashregister.purchases.ItemPurchasedAdapter

import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.*

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var itemPurchasedList: MutableList<ItemPurchased>
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var purchasingViewModel: PurchasingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        purchasingViewModel = ViewModelProvider(requireActivity()).get(PurchasingViewModel::class.java)
        itemPurchasedList = mutableListOf()
        firebaseFirestore = FirebaseFirestore.getInstance()
        binding.orderIsClick.setOnClickListener {
            purchasingViewModel.setPurchases(itemPurchasedList)
            Navigation.findNavController(view).navigate(R.id.action_nav_home_to_paymentFragment)
        }
        binding.buttonResetItemPurchased.setOnClickListener {
            itemPurchasedList.clear()
            binding.itemPurchasedCount.text = refreshItemCounter().toString()
            binding.itemPurchasedSubTotal.text = refreshSubtotalTotalAmount().toString()
        }



        //TODO: observe if new order is coming
        purchasingViewModel.getItemPurchased().observe(viewLifecycleOwner) { item ->
            itemPurchasedList.add(item)
            binding.itemPurchasedCount.text = refreshItemCounter().toString()
            binding.itemPurchasedSubTotal.text = refreshSubtotalTotalAmount().toString()
        }


    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(childFragmentManager, lifecycle)
        binding.viewPager.adapter = adapter
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("List"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Barcode Mode"))
        binding.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.viewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })


        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position))
            }
        })
    }




    //TODO: refresh the total amount of the order
    private fun refreshSubtotalTotalAmount(): Int {
        var purchasesSubtotal = 0
        if (itemPurchasedList.isNotEmpty()) {
            for (i in itemPurchasedList.indices) {
                purchasesSubtotal += itemPurchasedList[i].itemPurchasedPrice!!
            }
        }
        return purchasesSubtotal
    }

    //TODO: count the items
    private fun refreshItemCounter(): Int {
        var counter = 0
        if (itemPurchasedList.isNotEmpty()) {
            for (i in itemPurchasedList.indices) {
                counter += itemPurchasedList[i].itemPurchasedQuantity!!
            }
        }
        return counter
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_store,menu)
    }

    //TODO: get clicks in the toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_store -> {
                val pinCodeDialog = StorePinCodeDialog()
                pinCodeDialog.show(parentFragmentManager, "Store Pin Code")
                false
            }
            else -> {
                false
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        itemPurchasedList.clear()
        binding.itemPurchasedCount.text = refreshItemCounter().toString()
        binding.itemPurchasedSubTotal.text = refreshSubtotalTotalAmount().toString()
    }

    override fun onStart() {
        super.onStart()
        binding.cashierName.text = MainActivity.cashierName
    }
}