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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.UserInfo
import android.provider.Settings
import ink.kaleidoscope.ParallelSpaceManager
import java.lang.RuntimeException

/**
 * Wrapper for hidden system interfaces.
 */

class SystemInterfaces {
    companion object {
        fun getUserApplications(
            packageManager: PackageManager,
            userId: Int
        ): List<ApplicationInfo> {
            val all = ArrayList(packageManager.getInstalledApplicationsAsUser(0, userId))
            val system = arrayListOf<ApplicationInfo>()
            for (app in all) {
                if ((app.isSystemApp()))
                    system.add(app)
            }
            return all - system
        }

        fun getCurrentUserId(context: Context): Int {
            return context.getUserId()
        }

        fun getParallelUsers(): List<UserInfo> {
            return ParallelSpaceManager.getInstance().getParallelUsers()
        }

        fun duplicatePackage(packageName: String, userId: Int) {
            val ret = ParallelSpaceManager.getInstance().duplicatePackage(packageName, userId)
            if (ret < 0)
                throw RuntimeException("Failed when duplicatePackage() ret=$ret")
        }

        fun removePackage(packageName: String, userId: Int) {
            val ret = ParallelSpaceManager.getInstance().removePackage(packageName, userId)
            if (ret < 0)
                throw RuntimeException("Failed when removePackage() ret=$ret")
        }

        fun createSpace(name: String) {
            val ret = ParallelSpaceManager.getInstance().create(name)
            if (ret < 0)
                throw RuntimeException("Failed when createSpace() ret=$ret")
        }

        fun removeSpace(userId: Int) {
            val ret = ParallelSpaceManager.getInstance().remove(userId)
            if (ret < 0)
                throw RuntimeException("Failed when removeSpace() ret=$ret")
        }

        fun getSpaceCountLimit(context: Context): Int {
            return context.resources.getInteger(
                com.android.internal.R.integer.config_parallelSpaceMaxCount
            )
        }
    }
}
