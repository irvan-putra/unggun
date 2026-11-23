package com.vanipy.unggun.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.vanipy.unggun.R
import com.vanipy.unggun.data.AppStore
import com.vanipy.unggun.data.LogStore
import com.vanipy.unggun.data.TokenStore
import com.vanipy.unggun.logic.AssetPicker
import com.vanipy.unggun.net.Downloader
import com.vanipy.unggun.net.GitHubApi
import com.vanipy.unggun.net.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_ID = "app_id"
        fun newInstance(id: String) = DetailSheet().apply {
            arguments = Bundle().apply { putString(ARG_ID, id) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.sheet_detail, container, false)
        val id = requireArguments().getString(ARG_ID)!!
        val app = AppStore.get(id) ?: return v
        val ctx = requireContext()

        v.findViewById<TextView>(R.id.detail_title).text = app.displayName
        v.findViewById<TextView>(R.id.detail_subtitle).text =
            app.id + (app.installedPackageId?.let { "  ($it)" } ?: "")
        v.findViewById<TextView>(R.id.detail_status).text = buildString {
            append("installed: ${app.installedVersion ?: "?"}\n")
            append("latest: ${app.latestVersion ?: "?"}\n")
            app.releaseNotes?.takeIf { it.isNotBlank() }?.let {
                append("\n")
                append(it.take(600))
            }
        }

        val pre = v.findViewById<MaterialSwitch>(R.id.switch_prerelease)
        val track = v.findViewById<MaterialSwitch>(R.id.switch_track_only)
        val filter = v.findViewById<TextInputEditText>(R.id.input_filter)
        pre.isChecked = app.includePrerelease
        track.isChecked = app.trackOnly
        filter.setText(app.nameFilter)

        fun saveFlags() {
            app.includePrerelease = pre.isChecked
            app.trackOnly = track.isChecked
            app.nameFilter = filter.text?.toString() ?: ""
            AppStore.update(app)
        }
        pre.setOnCheckedChangeListener { _, _ -> saveFlags() }
        track.setOnCheckedChangeListener { _, _ -> saveFlags() }

        v.findViewById<Button>(R.id.btn_check).setOnClickListener { btn ->
            saveFlags()
            btn.isEnabled = false
            lifecycleScope.launch {
                val s = UpdateChecker.checkAll(ctx.applicationContext, onlyId = id)
                btn.isEnabled = true
                val fresh = AppStore.get(id)
                Toast.makeText(
                    ctx,
                    fresh?.let { "latest ${it.latestVersion ?: "?"}, installed ${it.installedVersion ?: "?"}" }
                        ?: "check failed",
                    Toast.LENGTH_LONG
                ).show()
                if (s.rateLimitedUntil > System.currentTimeMillis() / 1000) {
                    Toast.makeText(ctx, "github limit hit, retry after ${com.vanipy.unggun.net.RateLimit.formatHhMm(s.rateLimitedUntil)}", Toast.LENGTH_LONG).show()
                }
            }
        }

        v.findViewById<Button>(R.id.btn_install).setOnClickListener { btn ->
            saveFlags()
            if (app.trackOnly) {
                Toast.makeText(ctx, "track only: notify but never install", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btn.isEnabled = false
            lifecycleScope.launch {
                val token = TokenStore.read(ctx)
                try {
                    val rel = withContext(Dispatchers.IO) {
                        GitHubApi.fetchLatestRelease(app.owner, app.repo, app.includePrerelease, token)
                    }
                    val abis = android.os.Build.SUPPORTED_ABIS.toList()
                    val asset = AssetPicker.pick(rel.assets, abis, app.nameFilter)
                    if (asset == null) {
                        LogStore.log("install", "$id: no apk asset matches (filter '${app.nameFilter}')")
                        Toast.makeText(ctx, "no apk asset matches", Toast.LENGTH_LONG).show()
                    } else {
                        LogStore.log("install", "$id: picked ${asset.name}")
                        Downloader.start(ctx.applicationContext, app, asset)
                        dismiss()
                    }
                } catch (e: Exception) {
                    LogStore.log("install", "$id: fetch for install failed: ${e.message}")
                    Toast.makeText(ctx, "failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    btn.isEnabled = true
                }
            }
        }

        v.findViewById<Button>(R.id.btn_remove).setOnClickListener {
            AppStore.remove(id)
            LogStore.log("ui", "removed $id")
            dismiss()
        }

        return v
    }
}
