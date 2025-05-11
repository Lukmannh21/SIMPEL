package com.mbkm.telgo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore

class UserManagementActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var btnBack: MaterialButton
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)

        // Apply enter animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        btnBack = findViewById(R.id.btnBack)

        // Set up back button
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Set up the view pager with fragments
        setupViewPager()
    }

    private fun setupViewPager() {
        val adapter = UserPagerAdapter(this)
        viewPager.adapter = adapter

        // Connect the tab layout with the view pager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Unverified Users"
                1 -> tab.text = "Verified Users"
            }
        }.attach()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    // Inner class for the ViewPager adapter
    class UserPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> UserListFragment.newInstance("unverified")
                1 -> UserListFragment.newInstance("verified")
                else -> UserListFragment.newInstance("unverified")
            }
        }
    }
}