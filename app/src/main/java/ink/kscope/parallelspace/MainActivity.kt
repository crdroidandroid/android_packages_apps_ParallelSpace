/*
 * Copyright (C) 2022 Project Kaleidoscope
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ink.kscope.parallelspace

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ink.kscope.parallelspace.fragments.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<MainViewModel>()
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var menuLayout: LinearLayout
    private lateinit var deleteBtn: ImageView
    private lateinit var settingsBtn: ImageView

    // Show only the loading page
    private val loadingAdapter = object : FragmentStateAdapter(this) {
        override fun getItemCount(): Int {
            return 1
        }

        override fun createFragment(position: Int): Fragment {
            return LoadingFragment.newInstance()
        }
    }

    // Called when data loading is done
    private val loadingDoneListener = Observer<List<MainViewModel.MinimalUserInfo>> {
        // Block ui update if needed.
        if (viewModel.isRefreshing)
            return@Observer
        val adapter = ViewPagerAdapter(this, it)
        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val users = adapter.users
            if (position < users.size)
                tab.text = users[position].userName
            else
                tab.text = getString(R.string.new_space_tab)
        }.attach()
        if (it.isNotEmpty())
            tabLayout.visibility = View.VISIBLE
        viewPager.currentItem = viewModel.lastPageToSwitch
    }

    // Called when page scrolls
    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            val adapter = viewPager.adapter
            // Make sure loading is done
            if (adapter is ViewPagerAdapter) {
                // Animate menu when switching between space page and non-space page
                if (!adapter.isDataPage(position)) {
                    menuLayout.scaleX = 0f
                    menuLayout.alpha = 0f
                } else if (position < adapter.getLastDataPage()) {
                    menuLayout.scaleX = 1f
                    menuLayout.alpha = 1f
                } else if (position == adapter.getLastDataPage()) {
                    // The real animation is here
                    menuLayout.scaleX = clamp(1 - 2 * positionOffset, 0f, 1f)
                    menuLayout.alpha = clamp(1 - 4 * positionOffset, 0f, 1f)
                }
            } else {
                // Loading is now done, hide it
                menuLayout.scaleX = 0f
                menuLayout.alpha = 0f
            }
        }
    }

    // Called when delete button is clicked
    private val onDeleteListener = View.OnClickListener {
        DeleteSpaceDialog.newInstance().show(supportFragmentManager, null)
    }

    // Called when settings button is clicked
    private val onSettingsListener = View.OnClickListener {
        val adapter = viewPager.adapter
        if (adapter !is ViewPagerAdapter)
            return@OnClickListener
        val curUser = adapter.getUserInfo(viewPager.currentItem) ?: return@OnClickListener
        SpaceSettingsDialogFragment.newInstance(curUser.userId).show(supportFragmentManager, null)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.main_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewPager = findViewById(R.id.main_viewpager)
        tabLayout = findViewById(R.id.main_tab)
        menuLayout = findViewById(R.id.main_menu)
        deleteBtn = findViewById(R.id.main_delete)
        settingsBtn = findViewById(R.id.main_settings)
        viewPager.registerOnPageChangeCallback(onPageChangeCallback)
        viewModel.users.observe(this, loadingDoneListener)
        deleteBtn.setOnClickListener(onDeleteListener)
        settingsBtn.setOnClickListener(onSettingsListener)
        setLoading()
    }

    // Show the loading page and wait for onPageChangeCallback
    private fun setLoading() {
        viewPager.adapter = loadingAdapter
        tabLayout.visibility = View.GONE
    }

    // Called from NewSpaceDialog to create a new space
    fun createNewSpace(name: String) {
        // BUG: Do not use lifecycleScope here because a bug on the fwb side is causing the
        // activity to be recreated, which interrupts the process.
        // Should be same problem as:
        // https://github.com/Project-Kaleidoscope/android_packages_apps_Settings/commit/335ddbc
        viewModel.viewModelScope.launch {
            viewModel.lastPageToSwitch = viewPager.currentItem
            setLoading()
            // Mark that we are getting down to unstable part. We should not respond to any ui
            // update during it. Otherwise everything fucks up.
            viewModel.isRefreshing = true
            withContext(Dispatchers.Default) {
                SystemInterfaces.createSpace(name)
                // System is fucking unstable now. Let it rest for 5 seconds. We are going to
                // suffer from ~5 activity recreating during this period. What only survive is
                // this coroutine and our view model.
                delay(5000)
            }
            viewModel.refreshData()
        }
    }

    // Called from DeleteSpaceDialog
    fun removeSpace() {
        val adapter = viewPager.adapter
        if (adapter !is ViewPagerAdapter)
            return

        val curIndex = viewPager.currentItem
        if (!adapter.isDataPage(curIndex))
            return

        val userInfo = adapter.getUserInfo(curIndex) ?: return

        // Same case. See comments in createNewSpace().
        viewModel.viewModelScope.launch {
            viewModel.lastPageToSwitch = 0
            setLoading()
            viewModel.isRefreshing = true
            withContext(Dispatchers.Default) {
                delay(500)
                SystemInterfaces.removeSpace(userInfo.userId)
                delay(2000)
            }
            viewModel.refreshData()
        }
    }

    fun getNewSpaceName(): String {
        val adapter = viewPager.adapter
        val base = getString(R.string.default_space_name)
        if (adapter !is ViewPagerAdapter)
            return base
        return "$base ${adapter.users.size + 1}"
    }

    private inner class ViewPagerAdapter(
        fragmentActivity: FragmentActivity,
        val users: List<MainViewModel.MinimalUserInfo>
    ) :
        FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int {
            if (users.size < SystemInterfaces.getSpaceCountLimit(this@MainActivity))
                return users.size + 1
            // Limit exceeded
            return users.size
        }

        override fun createFragment(position: Int): Fragment {
            if (position < users.size)
                return AppListFragment.newInstance(users[position].userId)
            return NewSpaceFragment.newInstance()
        }

        // Whether a page shows parallel apps
        // Otherwise it may be the "new" page
        fun isDataPage(index: Int): Boolean {
            return index < users.size
        }

        // Get index of the last data page
        fun getLastDataPage(): Int {
            return users.size - 1
        }

        fun getUserInfo(index: Int): MainViewModel.MinimalUserInfo? {
            if (index >= users.size)
                return null
            return users[index]
        }
    }
}
