package com.vanipy.unggun.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.vanipy.unggun.R
import com.vanipy.unggun.data.LogStore
import com.vanipy.unggun.data.Prefs
import com.vanipy.unggun.data.TokenStore

class SettingsSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.sheet_settings, container, false)
        val ctx = requireContext()
        val token = v.findViewById<TextInputEditText>(R.id.input_token)
        val nu = v.findViewById<MaterialSwitch>(R.id.switch_notify_updates)
        val ne = v.findViewById<MaterialSwitch>(R.id.switch_notify_errors)
        val da = v.findViewById<MaterialSwitch>(R.id.switch_delete_after_install)

        token.setText(TokenStore.read(ctx))
        nu.isChecked = Prefs.notifyUpdates
        ne.isChecked = Prefs.notifyErrors
        da.isChecked = Prefs.deleteAfterInstall

        v.findViewById<Button>(R.id.btn_save).setOnClickListener {
            TokenStore.store(ctx, token.text?.toString()?.trim() ?: "")
            Prefs.notifyUpdates = nu.isChecked
            Prefs.notifyErrors = ne.isChecked
            Prefs.deleteAfterInstall = da.isChecked
            LogStore.log("ui", "settings saved (token ${if (TokenStore.hasToken(ctx)) "set" else "unset"})")
            Toast.makeText(ctx, "saved", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        return v
    }
}
