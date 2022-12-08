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

import android.app.Dialog
import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import ink.kscope.parallelspace.R
import ink.kscope.parallelspace.SystemInterfaces
import java.lang.RuntimeException

/**
 * Show the dialog of space settings.
 */

class SpaceSettingsDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val userId = arguments!!.getInt(ARG_TARGET, -1)
        if (userId < 0)
            throw RuntimeException("Bad userId $userId")

        val view = layoutInflater.inflate(R.layout.fragment_space_settings_dialog, null)
        return AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.space_settings))
            .setView(view)
            .setNegativeButton(R.string.close, null)
            .create()
    }

    companion object {
        private const val ARG_TARGET = "target"

        @JvmStatic
        fun newInstance(userId: Int) =
            SpaceSettingsDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TARGET, userId)
                }
            }
    }
}
