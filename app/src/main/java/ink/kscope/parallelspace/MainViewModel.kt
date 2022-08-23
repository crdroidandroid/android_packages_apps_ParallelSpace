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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backend of MainActivity.
 */

class MainViewModel(private val context: Application) : AndroidViewModel(context) {
    val users: MutableLiveData<List<MinimalUserInfo>> = MutableLiveData()
    var lastPageToSwitch = 0

    // A flag uses to indicate the refreshing is in progress. This is used to block ui update
    // when unwanted activity recreation happens. See details in MainActivity/createNewSpace().
    @Volatile
    var isRefreshing = true

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            users.value = withContext(Dispatchers.Default) {
                val result = arrayListOf<MinimalUserInfo>()
                for (user in SystemInterfaces.getParallelUsers()) {
                    result.add(
                        MinimalUserInfo(
                            user.id,
                            user.name ?: context.getString(R.string.default_space_name)
                        )
                    )
                }
                isRefreshing = false
                result
            }
        }
    }

    data class MinimalUserInfo(val userId: Int, val userName: String)
}
