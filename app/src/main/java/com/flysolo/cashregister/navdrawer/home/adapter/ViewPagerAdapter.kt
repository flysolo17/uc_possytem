package com.flysolo.cashregister.navdrawer.home.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.flysolo.cashregister.navdrawer.home.tablayout.BarcodeModeFragment
import com.flysolo.cashregister.navdrawer.home.tablayout.ItemListFragment
class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return ItemListFragment()
            1 -> return BarcodeModeFragment()
        }
        return ItemListFragment()
    }

    override fun getItemCount(): Int {
        return 2
    }
}