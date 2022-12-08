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

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ink.kscope.parallelspace.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backend of AppListFragment.
 */

class AppListViewModel(private val context: Application) : AndroidViewModel(context) {

    val appList = MutableLiveData<List<ItemData>>()
    var userId: Int = -1

    fun initAppListIfNeeded(target: Int, force: Boolean = false) {
        // Already initialized
        if (userId > 0 && !force)
            return
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val allApps = SystemInterfaces.getUserApplications(
                    context.packageManager,
                    SystemInterfaces.getCurrentUserId(context)
                )
                val dupedApps = SystemInterfaces.getUserApplications(
                    context.packageManager,
                    target
                )
                val result = arrayListOf<ItemData>()
                for (app in allApps) {
                    var enabled = false
                    for (dupedApp in dupedApps) {
                        if (dupedApp.packageName == app.packageName)
                            enabled = true
                    }
                    result.add(
                        ItemData(
                            app.loadLabel(context.packageManager).toString(),
                            app.packageName,
                            app.loadIcon(context.packageManager),
                            enabled
                        )
                    )
                }
                result
            }
            userId = target
            appList.value = result.sortedWith(compareBy({ it.label.lowercase() }))
        }
    }

    fun dupPackage(itemData: ItemData) {
        SystemInterfaces.duplicatePackage(itemData.packageName, userId)
        itemData.enabled = true
    }

    fun rmPackage(itemData: ItemData) {
        SystemInterfaces.removePackage(itemData.packageName, userId)
        itemData.enabled = false
    }

    data class ItemData(
        val label: String,
        val packageName: String,
        val icon: Drawable,
        var enabled: Boolean
    )
}
