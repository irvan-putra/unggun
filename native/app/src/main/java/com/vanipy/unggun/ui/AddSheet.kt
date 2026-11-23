package com.vanipy.unggun.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.vanipy.unggun.R
import com.vanipy.unggun.data.AppEntry
import com.vanipy.unggun.data.AppStore
import com.vanipy.unggun.data.LogStore

class AddSheet : BottomSheetDialogFragment() {

    companion object {
        private const val SELF_REPO = "irvan-putra/unggun"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.sheet_add, container, false)
        val input = v.findViewById<TextInputEditText>(R.id.input_urls)
        v.findViewById<Button>(R.id.btn_add).setOnClickListener {
            val lines = input.text?.toString()?.lines() ?: emptyList()
            var added = 0
            var skipped = 0
            for (line in lines) {
                if (line.isBlank()) continue
                val id = AppEntry.parseRepoInput(line)
                if (id == null) {
                    skipped++
                    continue
                }
                val entry = AppEntry(id = id, displayName = id.substringAfter('/'), includePrerelease = true)
                // Self-tracking: seed our own package id + version so the first check
                // does not flag the already-installed version as an update.
                if (id.equals(SELF_REPO, ignoreCase = true)) {
                    entry.installedPackageId = requireContext().packageName
                    try {
                        val pi = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
                        entry.installedVersion = pi.versionName
                    } catch (_: Exception) {
                    }
                }
                if (AppStore.add(entry)) added++ else skipped++
            }
            LogStore.log("ui", "added $added app(s), skipped $skipped")
            Toast.makeText(requireContext(), "added $added, skipped $skipped", Toast.LENGTH_SHORT).show()
            if (added > 0) dismiss()
        }
        return v
    }
}
