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

package ink.kscope.parallelspace.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ink.kscope.parallelspace.R

/**
 * Show a list of apps can be installed in a parallel space.
 */

class AppListFragment : Fragment() {
    private val viewModel by viewModels<AppListViewModel>()
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshData()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_app_list, container, false)
    }

    override fun onResume() {
        super.onResume()
        /**
         * BUG: Switch lost animation after switching pages when
         * using with recyclerview + viewpager.
         * Force a rebind to fix this.
         */
        recyclerView.adapter?.notifyDataSetChanged()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.app_list_recyclerview)
        swipeRefreshLayout = view.findViewById(R.id.app_list_refresh)

        swipeRefreshLayout.setColorSchemeResources(android.R.color.system_accent1_600)
        swipeRefreshLayout.isRefreshing = true
        recyclerView.layoutManager = LinearLayoutManager(context)

        viewModel.appList.observe(viewLifecycleOwner) {
            // Called when data is fully prepared
            recyclerView.adapter = AppListAdapter(it)
            swipeRefreshLayout.isRefreshing = false
        }

        swipeRefreshLayout.setOnRefreshListener {
            refreshData(true)
        }
    }

    private fun refreshData(force: Boolean = false) {
        arguments?.apply {
            viewModel.initAppListIfNeeded(getInt(ARG_TARGET), force)
        }
    }

    private inner class AppListAdapter(private val dataList: List<AppListViewModel.ItemData>) :
        RecyclerView.Adapter<AppListAdapter.AppListViewHolder>() {
        private inner class AppListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val iconView: ImageView
            val labelView: TextView
            val packageNameView: TextView
            val switch: Switch

            init {
                iconView = itemView.findViewById(R.id.app_list_item_icon)
                labelView = itemView.findViewById(R.id.app_list_item_label)
                packageNameView = itemView.findViewById(R.id.app_list_item_package_name)
                switch = itemView.findViewById(R.id.app_list_item_switch)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppListViewHolder {
            return AppListViewHolder(
                layoutInflater.inflate(R.layout.app_list_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
            holder.apply {
                dataList[position].let {
                    iconView.setImageDrawable(it.icon)
                    labelView.text = it.label
                    packageNameView.text = it.packageName
                    switch.isChecked = it.enabled
                    switch.setOnClickListener { view ->
                        if ((view as Switch).isChecked)
                            viewModel.dupPackage(it)
                        else
                            viewModel.rmPackage(it)
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return dataList.size
        }
    }

    companion object {
        private const val ARG_TARGET = "target"

        @JvmStatic
        fun newInstance(target: Int) =
            AppListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TARGET, target)
                }
            }
    }
}
